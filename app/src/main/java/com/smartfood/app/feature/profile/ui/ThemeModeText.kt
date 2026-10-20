package com.smartfood.app.feature.profile.ui

import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.ThemeMode

/**
 * Nombre: ThemeModeText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único mapeo de [ThemeMode] a `strings_profile.xml` (spec 008, RNF-PER-02): el valor que se ve en la fila
 * "Tema de la app" y en las opciones de su hoja (CA-PER-06-01 y 06-02).
 */

/** Texto de cada modo de color. */
val ThemeMode.titleRes: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.SYSTEM -> R.string.theme_system
    }
