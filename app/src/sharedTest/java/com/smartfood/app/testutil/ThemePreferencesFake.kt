package com.smartfood.app.testutil

import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.core.preferences.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nombre: ThemePreferencesFake.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fake de [ThemePreferences] en memoria para las pruebas del tema (spec 008, HU-PER-06). Emite cada
 * cambio y registra lo guardado.
 */
class FakeThemePreferences(
    initial: ThemeMode = ThemeMode.Default
) : ThemePreferences {

    private val state = MutableStateFlow(initial)

    /** Modos guardados, en orden. */
    val saved = mutableListOf<ThemeMode>()

    override val themeMode: Flow<ThemeMode> = state.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        saved += mode
        state.value = mode
    }
}
