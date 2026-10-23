package com.smartfood.app.feature.recipes.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.smartfood.app.R
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
import com.smartfood.app.feature.recipes.ui.cooking.TimerAlarm
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverEffect
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverScreen
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverViewModel
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.ui.ai.AiFlowScreen
import com.smartfood.app.feature.recipes.ui.ai.AiHelpScreen
import com.smartfood.app.feature.recipes.ui.ai.AiPlanViewModel
import com.smartfood.app.feature.recipes.ui.ai.AiEffect

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
            RecipesDiscoverScreen(state = state, onEvent = viewModel::onEvent, onSuggestRecipes = { navController.navigate(Route.AiPlan) })
        }
    }
    // Flujo de la IA (#115, plan §5): una sola ruta con su ViewModel; todas las navegaciones del prototipo son
    // instantáneas y la flecha atrás vuelve a Descubrir.
    composable<Route.AiPlan> {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Recipes>(inclusive = false) }
        ) {
            val viewModel: AiPlanViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is AiEffect.OpenRecipe -> navController.navigate(Route.RecipeDetail(effect.recipeId))
                    // "Ver recetas económicas" (CA-RIA-02-07): Descubrir lee el filtro de su savedStateHandle.
                    AiEffect.OpenEconomicRecipes -> {
                        navController.getBackStackEntry<Route.Recipes>().savedStateHandle[RecipesDiscoverViewModel.FILTER_KEY] =
                            RecipeFilter.ECONOMIC.name
                        navController.popBackStack<Route.Recipes>(inclusive = false)
                    }
                    AiEffect.OpenHelp -> navController.navigate(Route.AiHelp)
                }
            }
            AiFlowScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBack = { navController.popBackStack<Route.Recipes>(inclusive = false) }
            )
        }
    }
    composable<Route.AiHelp> {
        MainTabScaffold(
            currentTab = MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = { navController.popBackStack<Route.Recipes>(inclusive = false) }
        ) {
            val context = LocalContext.current
            val subject = stringResource(id = R.string.recipes_ai_support_subject)
            val email = stringResource(id = R.string.recipes_ai_support_email)
            AiHelpScreen(
                onBack = { navController.popBackStack() },
                // "Contactar soporte" (CA-RIA-03-01). Sin app de correo, no hace nada en lugar de cerrar la app.
                onContactSupport = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                .putExtra(Intent.EXTRA_SUBJECT, subject)
                        )
                    }
                }
            )
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
                    CookingEffect.PlayTimerFinishedAlert, CookingEffect.StopTimerAlert -> Unit
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
            // La alarma vive mientras la guía está en pantalla y se corta al salir (CA-RIA-04-04).
            val alarm = remember { TimerAlarm(context.applicationContext) }
            DisposableEffect(alarm) { onDispose { alarm.stop() } }
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    // "Salir" y el cierre vuelven al detalle (Figma 557:2786), sin pasar por "Antes de empezar".
                    CookingEffect.ExitToDetail -> navController.popBackStack<Route.RecipeDetail>(inclusive = false)
                    CookingEffect.PlayTimerFinishedAlert -> alarm.play()
                    CookingEffect.StopTimerAlert -> alarm.stop()
                    CookingEffect.OpenGuide -> Unit
                }
            }
            CookingScreen(state = state, onEvent = viewModel::onEvent, onBack = navController::popBackStack)
        }
    }
}
