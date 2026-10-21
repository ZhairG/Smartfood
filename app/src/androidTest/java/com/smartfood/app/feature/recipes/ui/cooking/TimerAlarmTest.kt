package com.smartfood.app.feature.recipes.ui.cooking

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: TimerAlarmTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Prueba en el dispositivo del aviso del temporizador (spec 009, CA-RIA-04-04, T020): al llamar a
 * [TimerAlarm.play] el sistema tiene un sonido de alarma en curso, y [TimerAlarm.stop] lo corta. Antes sonaba la
 * notificación, que en algunos teléfonos dura menos de un segundo y no se oía.
 */
class TimerAlarmTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun alarmPlaying(): Boolean =
        audio.activePlaybackConfigurations.any { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM }

    @Test
    fun caRia0404_alLlegarACeroSuenaLaAlarmaYSeCortaAlSalir() {
        val alarm = TimerAlarm(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { alarm.play() }

        val sono = (1..20).any { Thread.sleep(100); alarmPlaying() }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { alarm.stop() }
        Thread.sleep(500)

        assertTrue("No sonó ninguna alarma", sono)
        assertTrue("La alarma siguió sonando después de stop()", !alarmPlaying())
    }
}
