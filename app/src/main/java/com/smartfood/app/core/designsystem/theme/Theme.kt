package com.smartfood.app.core.designsystem.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Nombre: Theme.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Configuración del Tema de Material 3 para la aplicación Smartfood. El modo de color lo decide
 * [ThemeMode] (claro por defecto, spec 002 HU-UI-05); los esquemas de color no cambian.
 */

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,

    secondary = OrangeMain,
    onSecondary = White,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = OrangeDark,

    background = GreenDark,
    onBackground = White,

    surface = SurfaceLight,
    onSurface = GreenDark,
    surfaceVariant = White,
    onSurfaceVariant = GreyNeutral,

    outline = GreyDivider,

    error = ErrorRed,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenMain,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,

    secondary = OrangeMain,
    onSecondary = White,
    secondaryContainer = OrangeDark,
    onSecondaryContainer = OrangeLight,

    background = GreenDark,
    onBackground = White,

    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = InputDark,
    onSurfaceVariant = TextMuted,

    outline = BorderDark,

    error = ErrorRed,
    onError = White
)

/**
 * Tema principal de la aplicación Smartfood.
 *
 * @param themeMode Modo de color; por defecto [ThemeMode.Default] (claro), aunque el teléfono esté en modo oscuro.
 * @param dynamicColor Indica si se deben utilizar colores dinámicos (Android 12+).
 * @param content Contenido Composable que será renderizado dentro del tema.
 */
@Composable
fun SmartfoodTheme(
    themeMode: ThemeMode = ThemeMode.Default,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.isDark()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
