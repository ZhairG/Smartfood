package com.smartfood.app.feature.inventory.ui

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
import com.smartfood.app.feature.inventory.ui.camera.CAPTURED_PHOTO_KEY
import com.smartfood.app.feature.inventory.ui.camera.ProductCameraRoute
import com.smartfood.app.feature.inventory.ui.editor.EDITOR_RESULT_KEY
import com.smartfood.app.feature.inventory.ui.editor.EditorResultHost
import com.smartfood.app.feature.inventory.ui.editor.ProductEditorRoute
import com.smartfood.app.feature.inventory.ui.editor.encode
import com.smartfood.app.feature.inventory.ui.list.InventoryEffect
import com.smartfood.app.feature.inventory.ui.list.InventoryScreen
import com.smartfood.app.feature.inventory.ui.list.InventoryViewModel
import com.smartfood.app.feature.inventory.ui.search.InventorySearchRoute

/**
 * Nombre: InventoryNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos del inventario dentro de `MainGraph` (spec 006, plan §7). La lista (#39), la búsqueda (#42),
 * el editor (#40) y la cámara (#41) son las pantallas reales. "Mi Inventario" es una pestaña con barra inferior; las demás son pantallas
 * internas, con empuje y sin barra (CA-INV-09-04).
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.inventoryDestinations(navController: NavHostController) {
    composable<Route.Inventory>(
        enterTransition = NavTransitions.tabEnter,
        exitTransition = NavTransitions.tabExit,
        popEnterTransition = NavTransitions.tabPopEnter,
        popExitTransition = NavTransitions.tabPopExit
    ) { entry ->
        MainTabScaffold(currentTab = MainTab.INVENTORY, onTabSelected = navController::navigateToTab) {
            val viewModel: InventoryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ObserveEffects(viewModel.effects) { effect ->
                when (effect) {
                    is InventoryEffect.NavigateToEditor ->
                        navController.navigate(Route.ProductEditor(itemId = effect.itemId))
                    InventoryEffect.NavigateToSearch -> navController.navigate(Route.InventorySearch)
                }
            }
            InventoryScreen(state = state, onEvent = viewModel::onEvent)
            // El resultado del editor se ve encima de la lista, como el Figma (476:3851, T055).
            EditorResultHost(savedStateHandle = entry.savedStateHandle)
        }
    }
    // La búsqueda y el editor muestran la barra inferior con Inventario resaltado, como el Figma (CA-INV-09-04, T055).
    composable<Route.InventorySearch> { entry ->
        MainTabScaffold(currentTab = MainTab.INVENTORY, onTabSelected = navController::navigateToTab) {
            InventorySearchRoute(
                onNavigateToEditor = { id -> navController.navigate(Route.ProductEditor(id)) },
                onBack = { navController.popBackStack() }
            )
            EditorResultHost(savedStateHandle = entry.savedStateHandle)
        }
    }
    composable<Route.ProductEditor> { entry ->
        MainTabScaffold(currentTab = MainTab.INVENTORY, onTabSelected = navController::navigateToTab) {
            ProductEditorRoute(
                savedStateHandle = entry.savedStateHandle,
                onNavigateToCamera = { navController.navigate(Route.ProductCamera) },
                onBack = { navController.popBackStack() },
                onBackWithResult = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(EDITOR_RESULT_KEY, result.encode())
                    navController.popBackStack()
                }
            )
        }
    }
    composable<Route.ProductCamera> {
        ProductCameraRoute(
            onPhotoCaptured = { path ->
                navController.previousBackStackEntry?.savedStateHandle?.set(CAPTURED_PHOTO_KEY, path)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
}
