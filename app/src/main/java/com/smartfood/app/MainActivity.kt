package com.smartfood.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartfood.app.core.navigation.AppNavHost
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Nombre: MainActivity.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Actividad principal de la aplicación Smartfood.
 * Habilita el diseño de borde a borde (Edge-to-Edge) y renderiza el árbol de navegación.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartfoodTheme(darkTheme = false) {
                AppNavHost()
            }
        }
    }
}
