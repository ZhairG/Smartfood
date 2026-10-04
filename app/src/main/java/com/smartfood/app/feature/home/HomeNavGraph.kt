package com.smartfood.app.feature.home

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToLoginClearingBackStack

/**
 * Nombre: HomeNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Grafo principal de la app (usuario con sesión). Las features de inventario y recetas
 * agregarán aquí sus destinos.
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<Route.MainGraph>(startDestination = Route.Home) {
        composable<Route.Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    HomeEffect.NavigateToLogin -> navController.navigateToLoginClearingBackStack()
                }
            }

            HomeScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
