package com.smartfood.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartfood.app.navigation.AppNavigation
import com.smartfood.app.ui.theme.SmartfoodTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Nombre: MainActivity.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Actividad principal y punto de entrada de la interfaz de usuario de Smartfood.
 * Anotada con [AndroidEntryPoint] para habilitar la inyección de dependencias de Hilt.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Inicializa la actividad configurando la renderización Edge-to-Edge y el tema principal.
     *
     * @param savedInstanceState Estado previo guardado de la actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartfoodTheme(darkTheme = false) {
                AppNavigation()
            }
        }
    }
}
