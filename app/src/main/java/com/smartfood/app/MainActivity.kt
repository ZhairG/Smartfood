package com.smartfood.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.core.navigation.AppNavHost
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.preferences.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Nombre: MainActivity.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Actividad principal de la aplicación Smartfood.
 * Habilita el diseño de borde a borde (Edge-to-Edge) y renderiza el árbol de navegación con el tema elegido por el
 * usuario (spec 008, HU-PER-06): [ThemeViewModel] lee el modo guardado en el teléfono y se lo pasa a `SmartfoodTheme`.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La primera pantalla (Splash) es clara: íconos oscuros en ambas barras sin importar el modo del sistema
        // (spec 002, CA-UI-05-02). Cada pantalla ajusta después los suyos con SystemBarEffect.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            SmartfoodTheme(themeMode = themeMode) {
                AppNavHost()
            }
        }
    }
}
