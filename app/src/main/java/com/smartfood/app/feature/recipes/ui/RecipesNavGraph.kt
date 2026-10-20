package com.smartfood.app.feature.recipes.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.smartfood.app.core.navigation.MainTab
import com.smartfood.app.core.navigation.MainTabScaffold
import com.smartfood.app.core.navigation.NavTransitions
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToTab
import com.smartfood.app.feature.recipes.ui.discover.RecipesDiscoverScreen

/**
 * Nombre: RecipesNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos de recetas dentro de `MainGraph` (spec 009, plan §5, T006). La pestaña Recetas muestra
 * Descubrir con la barra inferior; las pantallas internas (detalle, flujo de la IA y cocina) se agregan en sus tareas
 * y no llevan barra.
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
            RecipesDiscoverScreen()
        }
    }
}
