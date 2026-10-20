package com.smartfood.app.core.preferences

import com.smartfood.app.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: ThemePreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato del tema elegido por el usuario en este teléfono (spec 008, HU-PER-06, RF-PER-07). Es una
 * preferencia del dispositivo, no de la cuenta: se conserva al cerrar sesión.
 */
interface ThemePreferences {

    /** Emite el modo de color elegido; la primera vez, [ThemeMode.Default] (CA-PER-06-03). */
    val themeMode: Flow<ThemeMode>

    /** Guarda el modo elegido (CA-PER-06-02). */
    suspend fun setThemeMode(mode: ThemeMode)
}
