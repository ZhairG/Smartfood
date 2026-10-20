package com.smartfood.app.testutil

import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nombre: NotificationSettingsFake.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fake de [NotificationSettingsRepository] en memoria para las pruebas de los ajustes de notificaciones
 * (spec 008, HU-PER-04). Emite cada cambio y registra lo guardado.
 */
class FakeNotificationSettingsRepository(
    initial: NotificationSettings = NotificationSettings()
) : NotificationSettingsRepository {

    private val state = MutableStateFlow(initial)

    /** Ajustes guardados, en orden. */
    val saved = mutableListOf<NotificationSettings>()

    override fun observe(): Flow<NotificationSettings> = state.asStateFlow()

    override suspend fun set(settings: NotificationSettings) {
        saved += settings
        state.value = settings
    }
}
