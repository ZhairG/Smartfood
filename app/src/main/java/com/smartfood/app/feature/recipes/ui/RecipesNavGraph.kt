package com.smartfood.app.feature.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import com.smartfood.app.feature.recipes.ui.detail.RecipeDetailEffect
import com.smartfood.app.feature.recipes.ui.detail.RecipeDetailViewModel
import com.smartfood.app.feature.recipes.ui.cooking.CookingEffect
import com.smartfood.app.feature.recipes.ui.cooking.CookingIntroScreen
import com.smartfood.app.feature.recipes.ui.cooking.CookingScreen
import com.smartfood.app.feature.recipes.ui.cooking.CookingViewModel
import com.smartfood.app.feature.recipes.ui.cooking.playTimerFinishedAlert
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverEffect
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverScreen
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverViewModel

/**
 * Nombre: RecipesNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos de recetas dentro de `MainGraph` (spec 009, plan §5, T006 a T008). Descubrir y el detalle
 * muestran la barra inferior con Recetas resaltado, como el Figma (`557:3028` y `557:2856`); tocar Recetas desde el
 * detalle vuelve a Descubrir. La cocina guiada (`557:2819` y `557:2781`, #116) va del detalle a "Antes de empezar" y
 * a los pasos; todas las navegaciones del prototipo son instantáneas. El flujo de la IA se agrega en su tarea.
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
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is RecipeDetailEffect.OpenCookingIntro -> navController.navigate(
                        Route.CookingIntro(effect.recipeId, effect.servings)
                    )
                }
            }
            RecipeDetailScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBack = { navController.popBackStack() }
            )
        }
    }
    composable<Route.CookingIntro> {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Recipes>(inclusive = false) },
        ) {
            val viewModel: CookingViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    CookingEffect.OpenGuide -> navController.navigate(Route.Cooking(state.recipeId, state.servings))
                    CookingEffect.ExitToDetail -> navController.popBackStack<Route.RecipeDetail>(inclusive = false)
                    CookingEffect.PlayTimerFinishedAlert -> Unit
                }
            }
            CookingIntroScreen(state = state, onEvent = viewModel::onEvent, onBack = navController::popBackStack)
        }
    }
    composable<Route.Cooking> {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Recipes>(inclusive = false) },
        ) {
            val viewModel: CookingViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    // "Salir" y el cierre vuelven al detalle (Figma 557:2786), sin pasar por "Antes de empezar".
                    CookingEffect.ExitToDetail -> navController.popBackStack<Route.RecipeDetail>(inclusive = false)
                    CookingEffect.PlayTimerFinishedAlert -> playTimerFinishedAlert(context)
                    CookingEffect.OpenGuide -> Unit
                }
            }
            CookingScreen(state = state, onEvent = viewModel::onEvent, onBack = navController::popBackStack)
        }
    }
}
