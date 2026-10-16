package com.ifpr.androidapptemplate.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Metricas
import com.ifpr.androidapptemplate.data.AtividadesRepository
import com.ifpr.androidapptemplate.databinding.FragmentPerfilUsuarioBinding
import com.ifpr.androidapptemplate.ui.login.LoginActivity

class PerfilUsuarioFragment : Fragment(R.layout.fragment_perfil_usuario) {
    private var binding: FragmentPerfilUsuarioBinding? = null
    private var cancelStats: (() -> Unit)? = null
    private var profileRef: DatabaseReference? = null
    private var profileListener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentPerfilUsuarioBinding.bind(view)
        binding = b
        val user = FirebaseAuth.getInstance().currentUser ?: return
        b.nome.setText(user.displayName)
        b.email.setText(user.email)
        Glide.with(this).load(user.photoUrl).placeholder(R.drawable.ic_profile_black_24dp).into(b.foto)
        profileRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val current = binding ?: return
                current.cidade.setText(snapshot.child("cidade").getValue(String::class.java).orEmpty())
                current.modalidade.setText(snapshot.child("modalidadeFavorita").getValue(String::class.java).orEmpty())
                current.bio.setText(snapshot.child("bio").getValue(String::class.java).orEmpty())
            }
            override fun onCancelled(error: DatabaseError) {
                if (binding != null) binding?.mensagem?.text = getString(R.string.usuario_nao_foi_possivel_carregar_o_perfil_verifique)
            }
        }
        profileRef!!.addListenerForSingleValueEvent(profileListener!!)
        b.salvar.setOnClickListener {
            val name = b.nome.text.toString().trim()
            if (name.isBlank()) { b.nome.error = getString(R.string.usuario_informe_seu_nome); return@setOnClickListener }
            b.salvar.isEnabled = false
            b.mensagem.text = getString(R.string.common_saving)
            val values = mapOf<String, Any>(
                "key" to user.uid, "nome" to name, "email" to user.email.orEmpty(),
                "cidade" to b.cidade.text.toString().trim(),
                "modalidadeFavorita" to b.modalidade.text.toString().trim(), "bio" to b.bio.text.toString().trim()
            )
            // Update only the new profile fields; preserve legacy data from the academic version.
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: IllegalStateException(getString(R.string.usuario_falha_no_perfil))
                    FirebaseDatabase.getInstance().getReference("users").child(user.uid).updateChildren(values)
                }.addOnCompleteListener { task ->
                    binding?.salvar?.isEnabled = true
                    binding?.mensagem?.text = if (task.isSuccessful) getString(R.string.usuario_perfil_atualizado) else getString(R.string.usuario_nao_foi_possivel_salvar_tente_novamente)
                }
        }
        b.sair.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
    override fun onStart() {
        super.onStart()
        cancelStats = AtividadesRepository(requireContext()).observar { items, error ->
            if (binding == null) return@observar
            binding?.estatisticas?.text = error ?: getString(R.string.profile_stats, items.orEmpty().size,
                Metricas.distancia(items.orEmpty().sumOf { it.distanciaMetros }),
                Metricas.tempo(items.orEmpty().sumOf { it.duracaoSegundos }))
        }
    }
    override fun onStop() { cancelStats?.invoke(); cancelStats = null; super.onStop() }
    override fun onDestroyView() {
        profileListener?.let { profileRef?.removeEventListener(it) }
        binding = null
        super.onDestroyView()
    }
}
