package com.ifpr.androidapptemplate

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ifpr.androidapptemplate.ui.home.RegistroViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroStateTest {
    @Test fun resumeKeepsIdOwnerDistanceAndStartDate() {
        val model = RegistroViewModel(SavedStateHandle())
        model.iniciar("test-owner")
        val id = model.id
        val start = model.inicio
        model.metros = 250.0
        model.pausar()
        assertFalse(model.gravando)
        model.iniciar("test-owner")
        assertEquals(id, model.id)
        assertEquals(start, model.inicio)
        assertEquals("test-owner", model.usuarioId)
        assertEquals(250.0, model.metros, 0.0)
        model.pausar()
    }

    @Test fun restoredActivityIsPausedAndDiscardResetsIt() {
        val state = SavedStateHandle(mapOf(
            "id" to "saved-session", "uid" to "test-owner", "inicio" to 1700000000000L,
            "modalidade" to "Caminhada", "metros" to 250.0, "tempo" to 120000L
        ))
        val restored = RegistroViewModel(state)
        assertFalse(restored.gravando)
        assertTrue(restored.ativa)
        assertEquals(120L, restored.segundos)
        assertTrue(restored.atividade().podeSalvar())
        restored.limpar()
        assertFalse(restored.ativa)
        assertEquals(0L, restored.segundos)
        assertEquals(0.0, restored.metros, 0.0)
    }
}
