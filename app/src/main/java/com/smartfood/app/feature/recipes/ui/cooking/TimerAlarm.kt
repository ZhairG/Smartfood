package com.smartfood.app.feature.recipes.ui.cooking

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.smartfood.app.R

/**
 * Nombre: TimerAlarm.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Aviso del temporizador de la cocina guiada al llegar a 0 (spec 009, CA-RIA-04-04, T020): repite el
 * "bip-bip-bip-bip" de un despertador digital (`res/raw/timer_alarm.ogg`, un ciclo de 1 s de "Alarm clock - 01.ogg" de
 * Wikimedia Commons, CC BY-SA 4.0; ver `docs/licenses/sonidos.md`) con el volumen de alarma y vibra al mismo ritmo
 * hasta que la persona lo detiene ([stop]), como los temporizadores comunes. Si nadie lo atiende, se calla solo a los
 * [MAX_MILLIS].
 *
 * @param context Contexto de la aplicación.
 */
class TimerAlarm(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val timeout = Runnable { stop() }

    /** Empieza a sonar y vibrar; si ya sonaba, vuelve a empezar. */
    fun play() {
        stop()
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.timer_alarm}"))
                isLooping = true
                prepare()
                start()
            }
        }
        vibrate()
        handler.postDelayed(timeout, MAX_MILLIS)
    }

    /** Detiene el sonido y la vibración. */
    fun stop() {
        handler.removeCallbacks(timeout)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun vibrate() {
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, REPEAT_FROM_START))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, REPEAT_FROM_START)
            }
        }
    }

    companion object {
        /** Si nadie detiene la alarma, se calla sola a los 60 s. */
        const val MAX_MILLIS = 60_000L
        private const val REPEAT_FROM_START = 0

        /** Vibra mientras suenan los bips y pausa el resto del segundo, al ritmo del sonido. */
        private val VIBRATION_PATTERN = longArrayOf(0, 550, 450)
    }
}
