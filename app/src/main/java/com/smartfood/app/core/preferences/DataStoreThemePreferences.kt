package com.smartfood.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.core.di.ThemePreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: DataStoreThemePreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [ThemePreferences] sobre el DataStore `theme_prefs` (spec 008, HU-PER-06). Guarda el
 * nombre del enum; si el valor falta o no se reconoce, el tema es el de por defecto, claro (CA-PER-06-03 y 06-04).
 *
 * @param dataStore Almacén de preferencias provisto por Hilt (`theme_prefs`).
 */
class DataStoreThemePreferences @Inject constructor(
    @ThemePreferencesDataStore private val dataStore: DataStore<Preferences>
) : ThemePreferences {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.Default
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
