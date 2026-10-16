package com.ifpr.androidapptemplate

import com.ifpr.androidapptemplate.baseclasses.Atividade
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtividadeTest {
    @Test fun twoMinutesWithoutGpsDistanceCanBeSaved() {
        assertTrue(Atividade(duracaoSegundos = 120, distanciaMetros = 0.0).podeSalvar())
    }
    @Test fun emptyActivityCannotBeSaved() {
        assertFalse(Atividade().podeSalvar())
    }
    @Test fun normalActivityCanBeSaved() {
        assertTrue(Atividade(duracaoSegundos = 120, distanciaMetros = 300.0).podeSalvar())
    }
    @Test fun invalidDistanceCannotBeSaved() {
        assertFalse(Atividade(duracaoSegundos = 120, distanciaMetros = Double.NaN).podeSalvar())
        assertFalse(Atividade(duracaoSegundos = 120, distanciaMetros = -1.0).podeSalvar())
    }
}
