package com.smartfood.app.feature.inventory.domain.model

/**
 * Nombre: Speech.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Eventos y errores del reconocimiento de voz que usa la búsqueda del inventario (HU-INV-07).
 */

/** Evento de una escucha de voz, en el orden en que ocurre. */
sealed interface SpeechEvent {
    /** El reconocedor está listo: se muestra "Habla ahora" (CA-INV-07-01). */
    data object Ready : SpeechEvent

    /** El usuario dejó de hablar y se procesa el audio: se muestra "…" (CA-INV-07-02). */
    data object Processing : SpeechEvent

    /** Transcripciones posibles, de la más probable a la menos probable. */
    data class Result(val alternatives: List<String>) : SpeechEvent

    /** La escucha terminó con un error. */
    data class Failed(val error: SpeechError) : SpeechEvent
}

/** Motivo por el que una escucha de voz no dio resultado. */
enum class SpeechError {
    /** No se entendió nada (CA-INV-07-05). */
    NO_MATCH,

    /** El teléfono no tiene servicio de reconocimiento de voz (CA-INV-07-08). */
    NOT_AVAILABLE,

    /** No hay permiso de micrófono (CA-INV-07-06). */
    PERMISSION_DENIED,

    /** El servicio de voz necesita conexión y no la hay. */
    NETWORK,
}
