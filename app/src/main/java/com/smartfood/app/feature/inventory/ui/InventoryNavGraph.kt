package com.smartfood.app.feature.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.navigation.MainTab
import com.smartfood.app.core.navigation.MainTabScaffold
import com.smartfood.app.core.navigation.NavTransitions
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToTab

/**
 * Nombre: InventoryNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos del inventario dentro de `MainGraph` (spec 006, plan §7). Por ahora son **pantallas
 * provisionales**: cada Issue reemplaza la suya sin tocar las rutas (lista → #39, editor → #40, cámara → #41,
 * búsqueda → #42). "Mi Inventario" es una pestaña con barra inferior; las demás son pantallas internas, con empuje
 * y sin barra (CA-INV-09-04).
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.inventoryDestinations(navController: NavHostController) {
    composable<Route.Inventory>(
        enterTransition = NavTransitions.tabEnter,
        exitTransition = NavTransitions.tabExit,
        popEnterTransition = NavTransitions.tabPopEnter,
        popExitTransition = NavTransitions.tabPopExit
    ) {
        MainTabScaffold(currentTab = MainTab.INVENTORY, onTabSelected = navController::navigateToTab) {
            // Provisional: la lista llega con el #39.
            PlaceholderScreen(title = stringResource(id = R.string.inventory_title))
        }
    }
    composable<Route.InventorySearch> {
        PlaceholderScreen(title = stringResource(id = R.string.placeholder_search_title)) // #42
    }
    composable<Route.ProductEditor> {
        PlaceholderScreen(title = stringResource(id = R.string.placeholder_editor_title)) // #40
    }
    composable<Route.ProductCamera> {
        PlaceholderScreen(title = stringResource(id = R.string.placeholder_camera_title)) // #41
    }
}

/** Pantalla provisional con el título del destino y "Próximamente". Se borra cuando no quede ningún destino que la use. */
@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(MaterialTheme.spacing.paddingForm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
