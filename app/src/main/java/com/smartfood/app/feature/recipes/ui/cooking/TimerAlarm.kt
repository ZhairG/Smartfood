package com.smartfood.app.feature.recipes.ui.cooking

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Nombre: TimerAlarm.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Aviso del temporizador de la cocina guiada al llegar a 0 (spec 009, CA-RIA-04-04, T020): suena la alarma
 * del teléfono durante [SOUND_MILLIS] y vibra tres veces. Usa el uso de audio de alarma, como un temporizador de cocina:
 * el sonido de notificación de algunos teléfonos dura menos de un segundo y no se oye. Guarda la referencia del
 * [Ringtone] mientras suena, para que no se pierda antes de tiempo, y [stop] lo corta al salir de la pantalla.
 *
 * @param context Contexto de la aplicación.
 */
class TimerAlarm(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private val stopRunnable = Runnable { stopSound() }

    /** Suena y vibra; si ya sonaba, vuelve a empezar. */
    fun play() {
        stopSound()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
            handler.postDelayed(stopRunnable, SOUND_MILLIS)
        }
        vibrate()
    }

    /** Corta el sonido, por ejemplo al salir de la cocina guiada. */
    fun stop() {
        stopSound()
    }

    private fun stopSound() {
        handler.removeCallbacks(stopRunnable)
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, NO_REPEAT))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, NO_REPEAT)
            }
        }
    }

    private companion object {
        const val SOUND_MILLIS = 5_000L
        const val NO_REPEAT = -1

        /** Espera, vibra, pausa…: tres vibraciones de medio segundo. */
        val VIBRATION_PATTERN = longArrayOf(0, 500, 300, 500, 300, 500)
    }
}
