package com.smartfood.app.feature.profile

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.smartfood.app.core.navigation.MainTab
import com.smartfood.app.core.navigation.MainTabScaffold
import com.smartfood.app.core.navigation.NavTransitions
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToLoginClearingBackStack
import com.smartfood.app.core.navigation.navigateToTab

/**
 * Nombre: ProfileNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destino de la pestaña Perfil dentro de `MainGraph` (spec 006, CA-INV-09-02 y 09-03), con la barra
 * inferior. Cerrar sesión limpia toda la pila y vuelve al inicio de sesión (CA-05-03).
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.profileDestination(navController: NavHostController) {
    composable<Route.Profile>(
        enterTransition = NavTransitions.tabEnter,
        exitTransition = NavTransitions.tabExit,
        popEnterTransition = NavTransitions.tabPopEnter,
        popExitTransition = NavTransitions.tabPopExit
    ) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        ObserveEffects(viewModel.effects) { effect ->
            when (effect) {
                ProfileEffect.NavigateToLogin -> navController.navigateToLoginClearingBackStack()
            }
        }

        MainTabScaffold(currentTab = MainTab.PROFILE, onTabSelected = navController::navigateToTab) {
            ProfileScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
