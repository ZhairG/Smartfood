package com.smartfood.app.feature.profile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.smartfood.app.core.di.NotificationPreferencesDataStore
import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: DataStoreNotificationSettings.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [NotificationSettingsRepository] sobre el DataStore `notification_prefs` (spec 008,
 * HU-PER-04). Cada aviso se guarda con su propia clave booleana; si no existe, vale `true` (CA-PER-04-01). El archivo
 * es del teléfono, así que la preferencia no se borra al cerrar sesión (CA-PER-04-02).
 *
 * @param dataStore Almacén de preferencias provisto por Hilt (`notification_prefs`).
 */
class DataStoreNotificationSettings @Inject constructor(
    @NotificationPreferencesDataStore private val dataStore: DataStore<Preferences>
) : NotificationSettingsRepository {

    override fun observe(): Flow<NotificationSettings> = dataStore.data.map { preferences ->
        NotificationSettings(
            expiringProducts = preferences[EXPIRING_PRODUCTS] ?: true,
            suggestedRecipes = preferences[SUGGESTED_RECIPES] ?: true,
            emptyInventory = preferences[EMPTY_INVENTORY] ?: true
        )
    }

    override suspend fun set(settings: NotificationSettings) {
        dataStore.edit { preferences ->
            preferences[EXPIRING_PRODUCTS] = settings.expiringProducts
            preferences[SUGGESTED_RECIPES] = settings.suggestedRecipes
            preferences[EMPTY_INVENTORY] = settings.emptyInventory
        }
    }

    private companion object {
        val EXPIRING_PRODUCTS = booleanPreferencesKey("expiring_products")
        val SUGGESTED_RECIPES = booleanPreferencesKey("suggested_recipes")
        val EMPTY_INVENTORY = booleanPreferencesKey("empty_inventory")
    }
}
