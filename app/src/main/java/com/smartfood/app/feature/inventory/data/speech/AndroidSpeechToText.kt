package com.smartfood.app.feature.inventory.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import com.smartfood.app.feature.inventory.domain.repository.SpeechToText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Nombre: AndroidSpeechToText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Adapta el reconocimiento de voz del sistema Android al contrato [SpeechToText] para la búsqueda de
 * inventario. Cada escucha usa español de Colombia y libera el reconocedor al cancelar o terminar el flujo.
 *
 * @param context Contexto de aplicación para consultar y crear el reconocedor del sistema.
 */
class AndroidSpeechToText @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SpeechToText {

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun listen(): Flow<SpeechEvent> = callbackFlow {
        if (!isAvailable()) {
            trySend(SpeechEvent.Failed(SpeechError.NOT_AVAILABLE))
            close()
            awaitClose { }
        } else {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(SpeechEvent.Ready)
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    trySend(SpeechEvent.Processing)
                }

                override fun onError(error: Int) {
                    trySend(SpeechEvent.Failed(mapSpeechError(error)))
                    close()
                }

                override fun onResults(results: Bundle?) {
                    val alternatives = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    trySend(SpeechEvent.Result(alternatives))
                    close()
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer.startListening(createRecognizerIntent())

            awaitClose {
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }.flowOn(Dispatchers.Main)

    private fun createRecognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
    }

    private companion object {
        const val LANGUAGE = "es-CO"
        const val MAX_RESULTS = 3
    }
}

/** Clasifica los códigos de error del reconocedor en el contrato de dominio de inventario. */
internal fun mapSpeechError(code: Int): SpeechError = when (code) {
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.NOT_AVAILABLE
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechError.NETWORK
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION_DENIED
    else -> SpeechError.NO_MATCH
}
