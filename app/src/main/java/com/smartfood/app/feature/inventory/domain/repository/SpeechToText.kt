package com.smartfood.app.feature.inventory.domain.repository

import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: SpeechToText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato del reconocimiento de voz en español que usa la búsqueda del inventario (HU-INV-07).
 */
interface SpeechToText {

    /** Indica si el teléfono tiene servicio de reconocimiento de voz (CA-INV-07-08). */
    fun isAvailable(): Boolean

    /**
     * Escucha una vez y emite sus eventos hasta un `Result` o un `Failed`. Dejar de recolectar el flujo cancela la
     * escucha (CA-INV-07-07).
     */
    fun listen(): Flow<SpeechEvent>
}
