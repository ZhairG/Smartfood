package com.smartfood.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.smartfood.app.ui.auth.AuthViewModel
import com.smartfood.app.ui.auth.login.LoginScreen
import com.smartfood.app.ui.splash.SplashScreen

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
        navigation<Route.AuthGraph>(startDestination = Route.Splash) {
            
            composable<Route.Splash>(
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                }
            ) {
                SplashScreen(
                    onNavigateNext = {
                        // Navegación hacia Login vaciando el Splash del BackStack
                        navController.navigate(Route.Login) {
                            popUpTo(Route.Splash) { inclusive = true }
                        }
                    }
                )
            }
            
            composable<Route.Login>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                }
            ) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                LoginScreen(
                    state = authState,
                    onEvent = authViewModel::onEvent,
                    onNavigateToRegister = { navController.navigate(Route.Register) },
                    onNavigateToRecovery = { navController.navigate(Route.PasswordRecovery) }
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
