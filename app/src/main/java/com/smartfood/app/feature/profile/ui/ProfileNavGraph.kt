package com.smartfood.app.feature.profile.ui

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
import com.smartfood.app.feature.profile.ui.edit.EditProfileRoute
import com.smartfood.app.feature.profile.ui.preferences.FoodPreferencesRoute

/**
 * Nombre: ProfileNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos del Perfil dentro de `MainGraph` (spec 006, CA-INV-09-02 y 09-03; spec 008): la pestaña,
 * "Editar información" y "Preferencias alimentarias", todas con la barra inferior. Cerrar sesión limpia toda la pila y
 * vuelve al inicio de sesión (CA-05-03).
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
                ProfileEffect.NavigateToEditProfile -> navController.navigate(Route.EditProfile)
                ProfileEffect.NavigateToFoodPreferences -> navController.navigate(Route.FoodPreferences)
            }
        }

        MainTabScaffold(currentTab = MainTab.PROFILE, onTabSelected = navController::navigateToTab) {
            ProfileScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
    // Pantallas internas del Perfil: empuje, con la barra inferior y "Perfil" resaltado (CA-PER-01-03). Volver a tocar
    // "Perfil" regresa al Perfil (CA-PER-01-04).
    composable<Route.EditProfile> {
        MainTabScaffold(
            currentTab = MainTab.PROFILE,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Profile>(inclusive = false) }
        ) {
            EditProfileRoute(onBack = { navController.popBackStack() })
        }
    }
    composable<Route.FoodPreferences> {
        MainTabScaffold(
            currentTab = MainTab.PROFILE,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Profile>(inclusive = false) }
        ) {
            FoodPreferencesRoute(onBack = { navController.popBackStack() })
        }
    }
}
