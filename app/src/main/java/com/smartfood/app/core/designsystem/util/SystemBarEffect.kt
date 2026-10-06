package com.smartfood.app.core.designsystem.util

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/**
 * Nombre: SystemBarEffect.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Composable utilitario para adaptar dinámicamente el estilo y la apariencia de los íconos del Status Bar
 * según el fondo (claro u oscuro) de la pantalla activa.
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

    DisposableEffect(isLightBackground) {
        val activity = context.findActivity()
        if (activity != null) {
            val statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
                detectDarkMode = { !isLightBackground }
            )

            activity.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            )

            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars = isLightBackground
        }
        onDispose {}
    }
}
