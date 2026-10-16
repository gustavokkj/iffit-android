package com.ifpr.androidapptemplate.baseclasses

import java.util.Locale
import kotlin.math.roundToLong

data class Atividade(
    var id: String = "",
    var usuarioId: String = "",
    var modalidade: String = "Corrida",
    var inicio: Long = 0,
    var duracaoSegundos: Long = 0,
    var distanciaMetros: Double = 0.0
) {
    fun podeSalvar() = duracaoSegundos >= 1 && distanciaMetros.isFinite() && distanciaMetros >= 0
}

object Metricas {
    fun tempo(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
    }
    fun distancia(metros: Double) = String.format(Locale.getDefault(), "%.2f km", metros / 1000)
    fun ritmo(segundos: Long, metros: Double): String {
        if (metros < 1 || segundos <= 0 || !metros.isFinite()) return "—"
        val pace = (segundos * 1000.0 / metros).roundToLong()
        return String.format(Locale.getDefault(), "%d:%02d min/km", pace / 60, pace % 60)
    }
    fun velocidade(segundos: Long, metros: Double): String =
        if (segundos <= 0) "—" else String.format(Locale.getDefault(), "%.1f km/h", metros * 3.6 / segundos)
}
