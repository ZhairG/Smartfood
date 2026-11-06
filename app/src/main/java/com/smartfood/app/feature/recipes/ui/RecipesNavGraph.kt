package com.smartfood.app.feature.recipes.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.smartfood.app.feature.recipes.ui.own.DeleteOwnRecipeScreen
import com.smartfood.app.feature.recipes.ui.own.DeleteOwnRecipeViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
import com.smartfood.app.feature.recipes.ui.saved.SavedRecipesEffect
import com.smartfood.app.feature.recipes.ui.saved.SavedRecipesScreen
import com.smartfood.app.feature.recipes.ui.saved.SavedRecipesViewModel
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.ui.ai.AiFlowScreen
import com.smartfood.app.feature.recipes.ui.ai.AiHelpScreen
import com.smartfood.app.feature.recipes.ui.ai.AiPlanViewModel
import com.smartfood.app.feature.recipes.ui.ai.AiEffect
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeEditorEffect
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeEditorScreen
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeEditorViewModel
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeSavedScreen
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeDetailScreen
import com.smartfood.app.feature.recipes.ui.own.OwnRecipeDetailViewModel

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
    composable<Route.RecipeDetail> { entry ->
        // Abierto desde Guardadas, resalta esa pestaña y vuelve a ella (spec 010, plan §4).
        val fromSaved = entry.toRoute<Route.RecipeDetail>().fromSaved
        MainTabScaffold(
            currentTab = if (fromSaved) MainTab.SAVED else MainTab.RECIPES,
            onTabSelected = navController::navigateToTab,
            onCurrentTabReselected = {
                if (fromSaved) {
                    navController.popBackStack<Route.Saved>(inclusive = false)
                } else {
                    navController.popBackStack<Route.Recipes>(inclusive = false)
                }
            }
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

/** Pestaña Guardadas (spec 010, T006 y T007) y el destino de "Nueva receta", que el #130 llena en T013. */
fun NavGraphBuilder.savedDestinations(navController: NavHostController) {
    composable<Route.Saved> {
        MainTabScaffold(
            currentTab = MainTab.SAVED,
            onTabSelected = navController::navigateToTab,
        ) {
            val viewModel: SavedRecipesViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is SavedRecipesEffect.OpenRecipe ->
                        navController.navigate(Route.RecipeDetail(effect.recipeId, fromSaved = true))
                    is SavedRecipesEffect.OpenOwnRecipe ->
                        navController.navigate(Route.OwnRecipeEditor(effect.recipeId))
                    SavedRecipesEffect.OpenNewRecipe -> navController.navigate(Route.OwnRecipeEditor())
                }
            }
            SavedRecipesScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
    composable<Route.OwnRecipeEditor> { entry ->
        val route = entry.toRoute<Route.OwnRecipeEditor>()
        SavedTabScaffold(navController) {
            val viewModel: OwnRecipeEditorViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val toSaved = { navController.popBackStack<Route.Saved>(inclusive = false) }
            LaunchedEffect(route.recipeId) { viewModel.initialize(route.recipeId) }
            BackHandler { toSaved() }
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is OwnRecipeEditorEffect.Saved ->
                        navController.navigate(Route.OwnRecipeSaved(effect.recipeId, effect.name, effect.edited)) {
                            popUpTo<Route.Saved>()
                        }
                    OwnRecipeEditorEffect.Close -> toSaved()
                    is OwnRecipeEditorEffect.OpenDelete ->
                        navController.navigate(Route.DeleteOwnRecipe(effect.recipeId, effect.name))
                }
            }
            OwnRecipeEditorScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
    composable<Route.OwnRecipeSaved> { entry ->
        val route = entry.toRoute<Route.OwnRecipeSaved>()
        SavedTabScaffold(navController) {
            OwnRecipeSavedScreen(
                name = route.name,
                edited = route.edited,
                onView = {
                    navController.navigate(Route.OwnRecipeDetail(route.recipeId)) { popUpTo<Route.Saved>() }
                },
                onCreateAnother = {
                    navController.navigate(Route.OwnRecipeEditor()) { popUpTo<Route.Saved>() }
                },
                onBack = { navController.popBackStack<Route.Saved>(inclusive = false) },
            )
        }
    }
    composable<Route.OwnRecipeDetail> { entry ->
        val route = entry.toRoute<Route.OwnRecipeDetail>()
        SavedTabScaffold(navController) {
            val viewModel: OwnRecipeDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            OwnRecipeDetailScreen(
                state = state,
                onFavoriteToggled = viewModel::onFavoriteToggled,
                onEdit = { navController.navigate(Route.OwnRecipeEditor(route.recipeId)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
    composable<Route.DeleteOwnRecipe> { entry ->
        val route = entry.toRoute<Route.DeleteOwnRecipe>()
        SavedTabScaffold(navController) {
            val viewModel: DeleteOwnRecipeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.deleted) { navController.popBackStack<Route.Saved>(inclusive = false) }
            DeleteOwnRecipeScreen(
                name = route.name,
                state = state,
                onConfirm = viewModel::onConfirm,
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/** Las pantallas internas de recetas propias conservan la barra con Guardadas resaltada, como en el Figma. */
@Composable
private fun SavedTabScaffold(navController: NavHostController, content: @Composable () -> Unit) {
    MainTabScaffold(
        currentTab = MainTab.SAVED,
        onTabSelected = navController::navigateToTab,
        onCurrentTabReselected = { navController.popBackStack<Route.Saved>(inclusive = false) },
        content = content,
    )
}
