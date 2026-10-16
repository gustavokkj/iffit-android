package com.ifpr.androidapptemplate.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.data.AtividadesRepository
import com.ifpr.androidapptemplate.baseclasses.Metricas
import com.ifpr.androidapptemplate.databinding.FragmentHomeBinding
import com.ifpr.androidapptemplate.ui.ai.AiLogicActivity

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var binding: FragmentHomeBinding? = null
    private lateinit var model: RegistroViewModel
    private lateinit var locationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var lastLocation: Location? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (preciseLocation()) iniciar() else {
            model.erro = getString(R.string.home_autorize_a_localizacao_precisa_para_registrar_a)
            render()
        }
    }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!model.gravando) return
            result.locations.forEach { point ->
                val age = android.os.SystemClock.elapsedRealtimeNanos() - point.elapsedRealtimeNanos
                if (age < 0 || age > 10_000_000_000L) return@forEach
                if (!point.hasAccuracy() || point.accuracy > 30f) return@forEach
                val previous = lastLocation
                if (previous != null && point.elapsedRealtimeNanos <= previous.elapsedRealtimeNanos) return@forEach
                if (previous != null) {
                    val seconds = (point.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1e9
                    val meters = previous.distanceTo(point)
                    val maxSpeed = if (model.modalidade == "Ciclismo") 35 else 15
                    if (seconds <= 0 || meters / seconds > maxSpeed) return@forEach
                    // Keep the anchor until slow movement accumulates enough distance.
                    if (meters < 3) return@forEach
                    model.metros += meters
                }
                lastLocation = point
            }
            render()
        }
    }
    private val tick = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 1000) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentHomeBinding.bind(view)
        model = ViewModelProvider(requireActivity())[RegistroViewModel::class.java]
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val sports = listOf("Corrida", "Caminhada", "Ciclismo")
        binding!!.modalidade.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sports)
        binding!!.modalidade.setSelection(sports.indexOf(model.modalidade).coerceAtLeast(0))
        binding!!.btnIniciar.setOnClickListener {
            if (model.gravando) pausar() else if (preciseLocation()) iniciar() else
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        binding!!.btnFim.setOnClickListener {
            pausar()
            MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.home_finalizar_atividade))
                .setMessage(if (model.metros < 1)
                    getString(R.string.home_o_gps_nao_registrou_distancia_voce_pode)
                    else getString(R.string.home_salve_a_atividade_para_consultar_seu_desempenho))
                .setPositiveButton(getString(R.string.common_save)) { _, _ -> salvar() }
                .setNegativeButton(getString(R.string.home_continuar_depois), null).show()
        }
        binding!!.descartar.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.home_descartar_atividade))
                .setMessage(getString(R.string.home_os_dados_desta_gravacao_serao_perdidos))
                .setPositiveButton(getString(R.string.common_discard)) { _, _ -> pausar(); model.limpar(); render() }
                .setNegativeButton(getString(R.string.common_cancel), null).show()
        }
        binding!!.fabAi.setOnClickListener { startActivity(Intent(requireContext(), AiLogicActivity::class.java)) }
        render()
    }
    private fun preciseLocation() = ContextCompat.checkSelfPermission(requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun iniciar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (model.salvando || !isResumed) return
        if (model.ativa && model.usuarioId != uid) model.limpar()
        model.modalidade = binding?.modalidade?.selectedItem.toString()
        lastLocation = null
        model.iniciar(uid)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build(),
            callback, Looper.getMainLooper()
        ).addOnFailureListener {
            pausar()
            model.erro = getString(R.string.home_nao_foi_possivel_iniciar_o_gps_confira)
            render()
        }
        render()
    }
    private fun pausar() {
        model.pausar()
        lastLocation = null
        locationClient.removeLocationUpdates(callback)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render()
    }
    private fun salvar() {
        if (model.salvando) return
        if (!model.atividade().podeSalvar()) {
            model.erro = getString(R.string.home_registre_pelo_menos_um_segundo_antes_de)
            render()
            return
        }
        model.salvando = true
        model.erro = null
        render()
        val appContext = requireContext().applicationContext
        AtividadesRepository(appContext).salvar(model.atividade()) { error ->
            model.salvando = false
            if (error == null) {
                model.limpar()
                model.erro = appContext.getString(R.string.home_atividade_salva_consulte_a_aba_atividades)
            } else {
                model.erro = appContext.getString(R.string.home_nao_foi_possivel_salvar_no_firebase_confira)
                context?.takeIf { isResumed }?.let {
                    MaterialAlertDialogBuilder(it).setTitle(getString(R.string.home_atividade_nao_salva))
                        .setMessage(model.erro).setPositiveButton(getString(R.string.common_understood), null).show()
                }
            }
            render()
        }
    }
    private fun render() {
        val b = binding ?: return
        b.txtTempo.text = Metricas.tempo(model.segundos)
        b.txtDistancia.text = Metricas.distancia(model.metros)
        b.txtRitmo.text = if (model.modalidade == "Ciclismo") Metricas.velocidade(model.segundos, model.metros)
            else Metricas.ritmo(model.segundos, model.metros)
        b.txtStatus.text = model.erro ?: when {
            model.salvando -> getString(R.string.home_salvando_aguarde_a_confirmacao_da_conexao)
            model.gravando -> if (lastLocation == null) getString(R.string.home_buscando_sinal_gps) else getString(R.string.home_atividade_em_andamento)
            model.ativa -> getString(R.string.home_atividade_pausada)
            else -> getString(R.string.home_seu_proximo_movimento_comeca_aqui)
        }
        b.btnIniciar.text = if (model.gravando) getString(R.string.common_pause) else if (model.ativa) getString(R.string.common_continue) else getString(R.string.home_iniciar_atividade)
        b.btnIniciar.isEnabled = !model.salvando
        b.btnFim.isEnabled = model.ativa && !model.salvando
        b.descartar.isEnabled = model.ativa && !model.salvando
        b.modalidade.isEnabled = !model.ativa
    }
    override fun onStart() { super.onStart(); handler.post(tick) }
    override fun onStop() {
        pausar()
        handler.removeCallbacks(tick)
        super.onStop()
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
