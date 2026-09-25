package com.smartfood.app.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Nombre: Theme.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Configuración del Tema de Material 3 para la aplicación Smartfood.
 * Configura la barra de estado con fondo semitransparente oscuro e íconos blancos permanentes.
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
 * Función de extensión recursiva para desempaquetar ContextWrappers de Hilt o Compose y obtener la [ComponentActivity] real.
 */
private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Tema principal de la aplicación Smartfood.
 *
 * @param darkTheme Indica si se debe aplicar el esquema de colores en modo oscuro.
 * @param dynamicColor Indica si se deben utilizar colores dinámicos (Android 12+).
 * @param content Contenido Composable que será renderizado dentro del tema.
 */
@Composable
fun SmartfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val context = LocalContext.current

    DisposableEffect(darkTheme) {
        val activity = context.findActivity()
        val statusBarScrim = AndroidColor.argb(0x66, 0, 0, 0) // Fondo semitransparente oscuro al 40%

        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(statusBarScrim), // Siempre íconos blancos con velo oscuro
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
        )
        onDispose {}
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
