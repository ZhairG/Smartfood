package com.smartfood.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.smartfood.app.navigation.AppNavigation
import com.smartfood.app.ui.theme.SmartfoodTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Esto permite dibujar detrás de las barras del sistema
        setContent {
            SmartfoodTheme(darkTheme = false) {
                // Removemos el modifier = Modifier.padding(innerPadding) del NavHost
                // para que los fondos llenen toda la pantalla.
                // Usaremos WindowInsets en cada pantalla si es necesario.
                AppNavigation()
            }
        }
    }
}
