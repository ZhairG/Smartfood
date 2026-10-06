package com.smartfood.app.feature.auth.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * <build>
 * <b>Nombre:</b> DataStoreSessionPreferences.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Implementación de [SessionPreferences] con Jetpack DataStore (RF-AUTH-06, ADR-06).<br>
 * </build>
 *
 * @param dataStore Almacén de preferencias `session_prefs` provisto por Hilt.
 */
class DataStoreSessionPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionPreferences {

    override suspend fun isRememberMeEnabled(): Boolean =
        dataStore.data.map { it[REMEMBER_ME] ?: true }.first()

    override suspend fun setRememberMe(enabled: Boolean) {
        dataStore.edit { it[REMEMBER_ME] = enabled }
    }

    private companion object {
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
    }
}
