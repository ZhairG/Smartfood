package com.smartfood.app.feature.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.navigation.MainTab
import com.smartfood.app.core.navigation.MainTabScaffold
import com.smartfood.app.core.navigation.NavTransitions
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.core.navigation.Route
import com.smartfood.app.core.navigation.navigateToTab
import com.smartfood.app.feature.inventory.ui.camera.CAPTURED_PHOTO_KEY
import com.smartfood.app.feature.inventory.ui.camera.ProductCameraRoute
import com.smartfood.app.feature.inventory.ui.list.InventoryEffect
import com.smartfood.app.feature.inventory.ui.list.InventoryScreen
import com.smartfood.app.feature.inventory.ui.list.InventoryViewModel
import java.io.File

/**
 * Nombre: InventoryNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Destinos del inventario dentro de `MainGraph` (spec 006, plan §7). La cámara (#41) y la lista (#39)
 * ya son las reales; el editor y la búsqueda siguen siendo **pantallas provisionales** que cada Issue reemplaza sin
 * tocar las rutas (editor → #40, búsqueda → #42). Los botones provisionales permiten probar la cámara hasta que
 * llegue el editor real. "Mi Inventario" es una pestaña con barra inferior; las demás son pantallas internas, con
 * empuje y sin barra (CA-INV-09-04).
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
        }
    }
    composable<Route.InventorySearch> {
        PlaceholderScreen(title = stringResource(id = R.string.placeholder_search_title)) // #42
    }
    composable<Route.ProductEditor> { entry ->
        // Provisional (#40): recibe la foto de la cámara por el savedStateHandle, como lo hará el editor real.
        val capturedPhoto by entry.savedStateHandle.getStateFlow<String?>(CAPTURED_PHOTO_KEY, null)
            .collectAsStateWithLifecycle()
        PlaceholderScreen(title = stringResource(id = R.string.placeholder_editor_title)) {
            capturedPhoto?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(id = R.string.placeholder_captured_photo),
                    modifier = Modifier.size(PlaceholderPhotoSize)
                )
            }
            SmartFoodPrimaryButton(
                text = stringResource(id = R.string.camera_take_photo),
                onClick = { navController.navigate(Route.ProductCamera) }
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

/**
 * Pantalla provisional con el título del destino, "Próximamente" y, opcionalmente, botones de prueba. Se borra
 * cuando no quede ningún destino que la use.
 */
@Composable
private fun PlaceholderScreen(title: String, content: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(MaterialTheme.spacing.paddingForm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterVertically)
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
        content()
    }
}

/** Tamaño de la miniatura provisional de la foto tomada (solo para probar la cámara hasta el #40). */
private val PlaceholderPhotoSize = 160.dp
