package com.smartfood.app.feature.profile

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
 * Nombre: ProfileNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Grafo principal de la app (usuario con sesión). Las features de inventario y recetas
 * agregarán aquí sus destinos.
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<Route.MainGraph>(startDestination = Route.Profile) {
        composable<Route.Profile> {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    ProfileEffect.NavigateToLogin -> navController.navigateToLoginClearingBackStack()
                }
            }

            ProfileScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
