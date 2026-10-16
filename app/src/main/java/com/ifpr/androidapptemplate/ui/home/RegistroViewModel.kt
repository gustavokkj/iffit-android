package com.ifpr.androidapptemplate.ui.home

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ifpr.androidapptemplate.baseclasses.Atividade
import java.util.UUID

/** Dados sobrevivem à recriação da tela. A gravação é pausada quando ela deixa de estar visível. */
class RegistroViewModel(private val state: SavedStateHandle) : ViewModel() {
    var id: String
        get() = state["id"] ?: ""
        private set(value) { state["id"] = value }
    var usuarioId: String
        get() = state["uid"] ?: ""
        private set(value) { state["uid"] = value }
    var modalidade: String
        get() = state["modalidade"] ?: "Corrida"
        set(value) { state["modalidade"] = value }
    var inicio: Long
        get() = state["inicio"] ?: 0L
        private set(value) { state["inicio"] = value }
    var metros: Double
        get() = state["metros"] ?: 0.0
        set(value) { state["metros"] = value }
    private var acumulado: Long
        get() = state["tempo"] ?: 0L
        set(value) { state["tempo"] = value }
    private var marca: Long? = null
    var salvando = false
    var erro: String? = null
    val ativa get() = id.isNotEmpty()
    val gravando get() = marca != null
    val segundos get() = (acumulado + (marca?.let { SystemClock.elapsedRealtime() - it } ?: 0L)) / 1000

    fun iniciar(uid: String) {
        if (!ativa) {
            id = UUID.randomUUID().toString()
            usuarioId = uid
            inicio = System.currentTimeMillis()
            metros = 0.0
            acumulado = 0L
        }
        marca = SystemClock.elapsedRealtime()
        erro = null
    }
    fun pausar() {
        marca?.let { acumulado += SystemClock.elapsedRealtime() - it }
        marca = null
    }
    fun atividade() = Atividade(id, usuarioId, modalidade, inicio, segundos, metros)
    fun limpar() {
        marca = null
        id = ""
        usuarioId = ""
        inicio = 0L
        acumulado = 0L
        metros = 0.0
        erro = null
    }
}
