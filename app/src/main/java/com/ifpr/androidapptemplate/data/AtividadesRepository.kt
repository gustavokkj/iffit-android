package com.ifpr.androidapptemplate.data

import android.content.Context

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ifpr.androidapptemplate.baseclasses.Atividade
import com.ifpr.androidapptemplate.R

class AtividadesRepository(context: Context) {
    private val appContext = context.applicationContext
    private fun reference(): DatabaseReference {
        val uid = requireNotNull(FirebaseAuth.getInstance().currentUser?.uid) { appContext.getString(R.string.database_session_error) }
        return FirebaseDatabase.getInstance().getReference("atividades").child(uid)
    }

    fun salvar(atividade: Atividade, complete: (Exception?) -> Unit) {
        try {
            check(FirebaseAuth.getInstance().currentUser?.uid == atividade.usuarioId) { appContext.getString(R.string.database_session_error) }
            reference().child(atividade.id).setValue(atividade)
                .addOnSuccessListener { complete(null) }
                .addOnFailureListener { complete(it) }
        } catch (e: Exception) { complete(e) }
    }

    fun observar(result: (List<Atividade>?, String?) -> Unit): () -> Unit {
        return try {
            val ref = reference()
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { it.getValue(Atividade::class.java) }
                    result(items.sortedByDescending { it.inicio }, null)
                }
                override fun onCancelled(error: DatabaseError) {
                    result(null, appContext.getString(R.string.database_load_error))
                }
            }
            ref.addValueEventListener(listener)
            val cancel: () -> Unit = { ref.removeEventListener(listener) }
            cancel
        } catch (e: Exception) {
            result(null, appContext.getString(R.string.database_load_error))
            val cancel: () -> Unit = {}
            cancel
        }
    }
}
