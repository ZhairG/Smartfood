package com.smartfood.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nombre: Spacing.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Define el Design System de espaciados para evitar valores hardcodeados en la UI.
 * Esto garantiza que la interfaz mantenga proporciones consistentes en todas las pantallas.
 */
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val paddingForm: Dp = 34.dp,
    val topPanel: Dp = 47.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

/**
 * Extensión para acceder fácilmente al espaciado desde MaterialTheme.
 * Ejemplo de uso: Modifier.padding(MaterialTheme.spacing.medium)
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
