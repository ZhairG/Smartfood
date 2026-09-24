package com.smartfood.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.smartfood.app.navigation.AppNavigation
import com.smartfood.app.ui.theme.SmartfoodTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal y punto de entrada de la aplicación Smartfood.
 *
 * Esta clase hereda de [ComponentActivity] y está anotada con [AndroidEntryPoint]
 * para permitir la inyección de dependencias de Hilt en el grafo de Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Llamado cuando la actividad está iniciando.
     * Configura el comportamiento "Edge to Edge" y establece el contenido de Compose.
     *
     * @param savedInstanceState Si la actividad se está re-inicializando, este Bundle contiene los datos recientes suministrados.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita el dibujo Edge to Edge. Los estilos de las barras del sistema (Status Bar y Navigation Bar)
        // se gestionan dinámicamente dentro de SmartfoodTheme según el tema (Claro/Oscuro).
        enableEdgeToEdge()
        setContent {
            SmartfoodTheme(darkTheme = false) {
                // Instancia el componente de navegación principal de la aplicación.
                AppNavigation()
            }
        }
    }
}