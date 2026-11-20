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
 * [ThemeMode] (claro por defecto, spec 002 HU-UI-05). El esquema claro no cambia; el oscuro se ajustó en la spec 013
 * para que sea legible, y los roles propios de la app están en [SmartFoodColors].
 */

internal val LightColorScheme = lightColorScheme(
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

    // Roles alineados con los frames "_Claro" de Figma (spec 002, CA-UI-07-02); los tokens no cambian de valor.
    surface = GreenLight,
    onSurface = Black,
    surfaceVariant = White,
    onSurfaceVariant = GreyLight,

    outline = GreyNeutral35,

    error = ErrorRed,
    onError = White
)

/**
 * Esquema oscuro. Spec 013 (RF-OSC-03): `onPrimary` es [GreenDark2] porque el blanco sobre [GreenMain] solo llega a
 * 1,6:1, y `primaryContainer` es [GreenDark] para que los contenedores no queden claros sobre el fondo oscuro.
 */
internal val DarkColorScheme = darkColorScheme(
    primary = GreenMain,
    onPrimary = GreenDark2,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenMain,

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
        LocalIsDarkTheme provides darkTheme,
        LocalSmartFoodColors provides if (darkTheme) DarkSmartFoodColors else LightSmartFoodColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
