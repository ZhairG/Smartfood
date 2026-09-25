package com.smartfood.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.smartfood.app.navigation.AppNavigation
import com.smartfood.app.ui.theme.SmartfoodTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Nombre: MainActivity.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Actividad principal de la aplicación Smartfood.
 * Configura la barra de estado con un fondo semitransparente oscuro e íconos blancos permanentes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val statusBarScrim = AndroidColor.argb(0x66, 0, 0, 0)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(statusBarScrim),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            SmartfoodTheme(darkTheme = false) {
                AppNavigation()
            }
        }
    }
}
