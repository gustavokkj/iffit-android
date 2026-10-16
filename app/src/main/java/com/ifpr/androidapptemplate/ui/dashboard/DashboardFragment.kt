package com.ifpr.androidapptemplate.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Metricas
import com.ifpr.androidapptemplate.data.AtividadesRepository
import com.ifpr.androidapptemplate.databinding.FragmentDashboardBinding
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var binding: FragmentDashboardBinding? = null
    private var cancel: (() -> Unit)? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentDashboardBinding.bind(view)
    }
    override fun onStart() {
        super.onStart()
        cancel = AtividadesRepository(requireContext()).observar { items, error ->
            val b = binding ?: return@observar
            b.lista.removeAllViews()
            b.resumo.text = error ?: if (items.isNullOrEmpty()) getString(R.string.dashboard_nenhuma_atividade_ainda_inicie_uma_gravacao_na) else {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val week = items.filter { it.inicio >= start }
                getString(R.string.history_summary, week.size, Metricas.distancia(week.sumOf { it.distanciaMetros }),
                    items.size, Metricas.distancia(items.sumOf { it.distanciaMetros }))
            }
            items?.forEach { item ->
                val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.inicio))
                val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = getString(R.string.history_entry, item.modalidade, date, Metricas.distancia(item.distanciaMetros), Metricas.tempo(item.duracaoSegundos))
                    isAllCaps = false
                    setOnClickListener {
                        val metric = if (item.modalidade == "Ciclismo") Metricas.velocidade(item.duracaoSegundos, item.distanciaMetros)
                            else Metricas.ritmo(item.duracaoSegundos, item.distanciaMetros)
                        MaterialAlertDialogBuilder(requireContext()).setTitle(item.modalidade)
                            .setMessage(getString(R.string.history_detail, date, Metricas.distancia(item.distanciaMetros), Metricas.tempo(item.duracaoSegundos), metric))
                            .setPositiveButton(getString(R.string.common_close), null).show()
                    }
                }
                b.lista.addView(button)
            }
        }
    }
    override fun onStop() { cancel?.invoke(); cancel = null; super.onStop() }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
