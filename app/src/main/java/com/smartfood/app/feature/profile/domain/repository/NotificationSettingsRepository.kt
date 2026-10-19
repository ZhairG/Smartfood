package com.smartfood.app.feature.profile.domain.repository

import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: NotificationSettingsRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de los ajustes de notificaciones guardados en este teléfono (spec 008, HU-PER-04). No
 * dependen de la cuenta: se conservan al cerrar sesión (CA-PER-04-02).
 */
interface NotificationSettingsRepository {

    /** Emite los ajustes vigentes; la primera vez, todos encendidos (CA-PER-04-01). */
    fun observe(): Flow<NotificationSettings>

    /** Guarda los ajustes en el teléfono (CA-PER-04-02). */
    suspend fun set(settings: NotificationSettings)
}
