package com.smartfood.app.core.designsystem.util

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme

/**
 * Nombre: SystemBarEffect.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Composable utilitario para adaptar dinámicamente el estilo y la apariencia de los íconos del Status Bar
 * según el fondo (claro u oscuro) de la pantalla activa. La barra de navegación sigue el tema de la app (spec 013,
 * CA-OSC-04-01).
 */

/**
 * Función de extensión recursiva para desempaquetar ContextWrappers y obtener la [ComponentActivity] contenedora.
 */
private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Composable de efecto secundario que ajusta la apariencia del Status Bar al montarse en la composición.
 *
 * @param isLightBackground Si es `true`, aplica íconos oscuros para fondos claros. Si es `false`, aplica íconos blancos para fondos oscuros.
 */
@Composable
fun SystemBarEffect(isLightBackground: Boolean) {
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current
    val currentIsDarkTheme by rememberUpdatedState(isDarkTheme)

    // La barra de estado depende solo del fondo. Si el tema también fuera clave, al leerse el tema oscuro de DataStore
    // (un instante después del primer dibujo) se repetiría el efecto de la Splash, que sigue en la transición, y
    // dejaría íconos oscuros sobre el Login (spec 013, medido en el V2436 el 2026-11-19).
    DisposableEffect(isLightBackground) {
        val activity = context.findActivity()
        if (activity != null) {
            val statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
                detectDarkMode = { !isLightBackground }
            )

            // La barra de navegación sigue el tema vigente al mostrarse la pantalla (CA-OSC-04-01).
            activity.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = if (currentIsDarkTheme) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
            )

            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars = isLightBackground
        }
        onDispose {}
    }
}
