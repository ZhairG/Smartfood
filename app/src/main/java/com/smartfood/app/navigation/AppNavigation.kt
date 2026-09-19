package com.smartfood.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // Aquí puedes añadir la lógica para determinar si el usuario ya está logueado
    // y cambiar el startDestination entre AuthGraph y MainGraph.
    val startDestination = Route.AuthGraph

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Grafo de Autenticación
        navigation<Route.AuthGraph>(startDestination = Route.Login) {
            composable<Route.Login> {
                PlaceholderScreen(
                    title = "Pantalla de Login",
                    onNavigate = { navController.navigate(Route.Home) },
                    buttonText = "Ir a Home (Simular Login)"
                )
            }
            composable<Route.Register> {
                PlaceholderScreen(
                    title = "Pantalla de Registro",
                    onNavigate = { navController.popBackStack() },
                    buttonText = "Volver al Login"
                )
            }
            composable<Route.PasswordRecovery> {
                PlaceholderScreen(
                    title = "Recuperar Contraseña",
                    onNavigate = { navController.popBackStack() },
                    buttonText = "Volver al Login"
                )
            }
        }

        // Grafo Principal (Home / Dashboard)
        navigation<Route.MainGraph>(startDestination = Route.Home) {
            composable<Route.Home> {
                PlaceholderScreen(
                    title = "Pantalla de Home",
                    onNavigate = { 
                        // Cerrar sesión y volver al login
                        navController.navigate(Route.AuthGraph) {
                            popUpTo(Route.MainGraph) { inclusive = true }
                        }
                    },
                    buttonText = "Cerrar Sesión"
                )
            }
        }
    }
}

/**
 * Pantalla temporal para visualizar la navegación antes de construir las UIs reales.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onNavigate: () -> Unit,
    buttonText: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigate) {
                Text(text = buttonText)
            }
        }
    }
}
