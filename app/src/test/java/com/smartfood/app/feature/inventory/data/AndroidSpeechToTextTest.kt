package com.smartfood.app.feature.inventory.data

import android.speech.SpeechRecognizer
import com.smartfood.app.feature.inventory.data.speech.mapSpeechError
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: AndroidSpeechToTextTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias del mapeo de errores del reconocimiento de voz de Android (spec 006, T019).
 */
class AndroidSpeechToTextTest {

    @Test
    fun `CA-INV-07-05 sin coincidencia o tiempo agotado se mapea a NO_MATCH`() {
        assertEquals(SpeechError.NO_MATCH, mapSpeechError(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(SpeechError.NO_MATCH, mapSpeechError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(SpeechError.NO_MATCH, mapSpeechError(SpeechRecognizer.ERROR_CLIENT))
    }

    @Test
    fun `CA-INV-07-06 permiso insuficiente se mapea a PERMISSION_DENIED`() {
        assertEquals(SpeechError.PERMISSION_DENIED, mapSpeechError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
    }

    @Test
    fun `CA-INV-07-08 servicio o idioma no disponible se mapea a NOT_AVAILABLE`() {
        assertEquals(SpeechError.NOT_AVAILABLE, mapSpeechError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED))
        assertEquals(SpeechError.NOT_AVAILABLE, mapSpeechError(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertEquals(SpeechError.NOT_AVAILABLE, mapSpeechError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
    }

    @Test
    fun `NETWORK de voz se mapea a NETWORK`() {
        assertEquals(SpeechError.NETWORK, mapSpeechError(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(SpeechError.NETWORK, mapSpeechError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
    }
}
