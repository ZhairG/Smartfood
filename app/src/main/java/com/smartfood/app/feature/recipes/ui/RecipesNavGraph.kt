package com.smartfood.app.feature.recipes.ui

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
import com.smartfood.app.core.navigation.navigateToTab
import com.smartfood.app.feature.recipes.ui.detail.RecipeDetailScreen
import com.smartfood.app.feature.recipes.ui.detail.RecipeDetailViewModel
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverEffect
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverScreen
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverViewModel

/**
 * Nombre: RecipesNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos de recetas dentro de `MainGraph` (spec 009, plan §5, T006 a T008). Descubrir y el detalle
 * muestran la barra inferior con Recetas resaltado, como el Figma (`557:3028` y `557:2856`); tocar Recetas desde el
 * detalle vuelve a Descubrir. El flujo de la IA y la cocina se agregan en sus tareas.
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.recipesDestinations(navController: NavHostController) {
    composable<Route.Recipes>(
        enterTransition = NavTransitions.tabEnter,
        exitTransition = NavTransitions.tabExit,
        popEnterTransition = NavTransitions.tabPopEnter,
        popExitTransition = NavTransitions.tabPopExit
    ) {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab
        ) {
            val viewModel: RecipesDiscoverViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is RecipesDiscoverEffect.OpenRecipe ->
                        navController.navigate(Route.RecipeDetail(effect.recipeId))
                }
            }
            RecipesDiscoverScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
    composable<Route.RecipeDetail> {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Recipes>(inclusive = false) }
        ) {
            val viewModel: RecipeDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            RecipeDetailScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
