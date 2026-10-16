package com.smartfood.app.feature.inventory.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.smartfood.app.feature.inventory.domain.repository.InventoryPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: DataStoreInventoryPreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [InventoryPreferences] sobre el DataStore de preferencias existente. Guarda si ya
 * se pidió el permiso de notificaciones del inventario (CA-INV-08-05). El permiso es del teléfono, no de la cuenta,
 * así que la preferencia es global y no se borra al cerrar sesión.
 *
 * @param dataStore Almacén de preferencias provisto por Hilt (`session_prefs`).
 */
class DataStoreInventoryPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : InventoryPreferences {

    override suspend fun wasNotificationPermissionRequested(): Boolean =
        dataStore.data.map { it[NOTIFICATION_PERMISSION_REQUESTED] ?: false }.first()

    override suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[NOTIFICATION_PERMISSION_REQUESTED] = true }
    }

    private companion object {
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
