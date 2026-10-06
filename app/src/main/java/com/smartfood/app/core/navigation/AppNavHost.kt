package com.smartfood.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfood.app.feature.auth.ui.authGraph
import com.smartfood.app.feature.home.mainGraph
import com.smartfood.app.feature.splash.SplashDestination
import com.smartfood.app.feature.splash.SplashScreen
import com.smartfood.app.feature.splash.SplashViewModel

/**
 * <build>
 * <b>Nombre:</b> AppNavHost.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Raíz del árbol de navegación type-safe (plan.md §7). Splash decide el destino inicial<br>
 * (HU-03); cada feature declara su propio grafo anidado.<br>
 * </build>
 *
 * @param modifier Modificador opcional para el [NavHost].
 * @param navController Controlador de navegación.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        modifier = modifier,
        enterTransition = NavTransitions.enter,
        exitTransition = NavTransitions.exit,
        popEnterTransition = NavTransitions.popEnter,
        popExitTransition = NavTransitions.popExit
    ) {
        composable<Route.Splash> {
            val viewModel: SplashViewModel = hiltViewModel()
            val destination by viewModel.destination.collectAsStateWithLifecycle()

            SplashScreen(
                destination = destination,
                onNavigate = { resolved ->
                    when (resolved) {
                        SplashDestination.HOME -> navController.navigateToMainGraph()
                        SplashDestination.LOGIN -> navController.navigate(Route.Login) {
                            popUpTo<Route.Splash> { inclusive = true }
                        }
                    }
                }
            )
        }

        authGraph(navController)
        mainGraph(navController)
    }
}

/** Entra a la app (Home) y elimina de la pila todo lo anterior: "atrás" no regresa al login (CA-02-05). */
fun NavHostController.navigateToMainGraph() {
    navigate(Route.MainGraph) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** Vuelve al inicio de sesión y elimina de la pila todo lo anterior (CA-01-08, CA-04-11, CA-05-03). */
fun NavHostController.navigateToLoginClearingBackStack() {
    navigate(Route.Login) {
        popUpTo(graph.id) { inclusive = true }
    }
}
