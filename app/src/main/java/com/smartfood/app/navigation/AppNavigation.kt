package com.smartfood.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.smartfood.app.R
import com.smartfood.app.ui.auth.AuthViewModel
import com.smartfood.app.ui.auth.login.LoginScreen
import com.smartfood.app.ui.auth.register.RegisterScreen
import com.smartfood.app.ui.components.ActionSuccessScreen
import com.smartfood.app.ui.splash.SplashScreen

/**
 * Nombre: AppNavigation.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Contenedor y gestor principal del árbol de navegación Jetpack Compose (Type-Safe).
 * Configurado con transición de empuje (Push) fluida de 600 ms e interpolación Ease in and out.
 *
 * @param modifier Modificador opcional para el contenedor [NavHost].
 * @param navController Controlador de navegación [NavHostController].
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
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
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            ) {
                SplashScreen(
                    onNavigateNext = {
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
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
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

            composable<Route.Register>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            ) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                RegisterScreen(
                    state = authState,
                    onEvent = authViewModel::onEvent,
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToSuccess = {
                        navController.navigate(Route.RegisterSuccess) {
                            popUpTo(Route.Register) { inclusive = true }
                        }
                    }
                )
            }

            composable<Route.RegisterSuccess>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            ) {
                ActionSuccessScreen(
                    title = stringResource(id = R.string.register_success_title),
                    body = stringResource(id = R.string.register_success_body),
                    buttonText = stringResource(id = R.string.register_success_login_button),
                    onButtonClick = {
                        navController.navigate(Route.Login) {
                            popUpTo(Route.AuthGraph) { inclusive = false }
                        }
                    }
                )
            }

            composable<Route.PasswordRecovery>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            ) {
                PlaceholderScreen(
                    title = stringResource(id = R.string.title_recovery_screen),
                    onNavigate = { navController.popBackStack() },
                    buttonText = stringResource(id = R.string.action_back_to_login)
                )
            }
        }

        // Grafo Principal
        navigation<Route.MainGraph>(startDestination = Route.Home) {
            composable<Route.Home> {
                PlaceholderScreen(
                    title = stringResource(id = R.string.title_home_screen),
                    onNavigate = { 
                        navController.navigate(Route.AuthGraph) {
                            popUpTo(Route.MainGraph) { inclusive = true }
                        }
                    },
                    buttonText = stringResource(id = R.string.action_logout)
                )
            }
        }
    }
}

/**
 * Pantalla temporal utilizada para prototipar rutas antes de la implementación de la UI completa.
 *
 * @param title Título visible de la pantalla.
 * @param onNavigate Callback al presionar el botón.
 * @param buttonText Texto a mostrar en el botón de acción.
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
