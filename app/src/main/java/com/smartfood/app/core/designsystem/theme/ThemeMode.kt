package com.smartfood.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Nombre: ThemeMode.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fuente única del modo de color de la app (spec 002, HU-UI-05). Por defecto la app es clara aunque el
 * teléfono esté en modo oscuro (CA-UI-05-01). Este es el único archivo que consulta el modo del sistema (RNF-UI-05),
 * así que un selector futuro solo tiene que cambiar el [ThemeMode] que recibe `SmartfoodTheme`.
 */
enum class ThemeMode {
    /** Siempre claro. */
    LIGHT,

    /** Siempre oscuro. */
    DARK,

    /** Sigue el modo del sistema. */
    SYSTEM;

    companion object {
        /** Modo por defecto de la app (CA-UI-05-03). */
        val Default = LIGHT
    }
}

/**
 * Indica si el modo resulta oscuro, dado el modo del sistema.
 *
 * @param systemIsDark Si el teléfono está en modo oscuro.
 */
fun ThemeMode.resolveIsDark(systemIsDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemIsDark
}

/** Indica si el modo resulta oscuro con el modo actual del sistema. */
@Composable
fun ThemeMode.isDark(): Boolean = resolveIsDark(systemIsDark = isSystemInDarkTheme())

/**
 * Indica si el tema vigente de la app es oscuro. Lo provee `SmartfoodTheme`; los componentes lo leen en lugar de
 * consultar el modo del sistema.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }
