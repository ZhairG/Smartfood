package com.smartfood.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodBottomBar
import com.smartfood.app.core.designsystem.components.SmartFoodTab

/**
 * Nombre: MainTabScaffold.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pestañas de la navegación principal y su barra inferior (spec 006, CA-INV-09-02). Solo las pantallas
 * de pestaña (Inventario y Perfil) usan [MainTabScaffold]; las pantallas internas y las de autenticación no lo usan,
 * así que no muestran la barra (CA-INV-09-04) y sus márgenes de teclado y de barras del sistema no cambian
 * (spec 002). Los íconos son de Material hasta que el #37 exporte los del Figma.
 */

/** Pestañas de la barra inferior, en orden. */
/**
 * Pestañas de la barra inferior del Figma (`476:4176`), en orden. Recetas y Guardadas todavía no existen: se ven
 * deshabilitadas (spec 006, CA-INV-09-02, T055).
 */
enum class MainTab(val enabled: Boolean = true) { INVENTORY, RECIPES(enabled = false), SAVED(enabled = false), PROFILE }

/**
 * Pantalla de pestaña con la barra inferior.
 *
 * @param currentTab Pestaña que se está mostrando (resaltada en la barra).
 * @param onTabSelected Acción al tocar una pestaña.
 * @param onCurrentTabReselected Acción al volver a tocar la pestaña resaltada desde una pantalla interna: vuelve a la
 * pantalla principal de la pestaña (CA-INV-09-04). En las pantallas de pestaña no hace nada.
 * @param content Contenido de la pestaña. Recibe el margen de la barra ya marcado como consumido, así que su propio
 * `windowInsetsPadding(systemBars)` no suma dos veces la barra de navegación.
 */
@Composable
fun MainTabScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onCurrentTabReselected: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val labels = mapOf(
        MainTab.INVENTORY to stringResource(id = R.string.tab_inventory),
        MainTab.RECIPES to stringResource(id = R.string.tab_recipes),
        MainTab.SAVED to stringResource(id = R.string.tab_saved),
        MainTab.PROFILE to stringResource(id = R.string.tab_profile),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            SmartFoodBottomBar(
                tabs = MainTab.entries.map { tab ->
                    SmartFoodTab(
                        label = labels.getValue(tab),
                        icon = tab.icon,
                        selected = tab == currentTab,
                        onClick = { if (tab != currentTab) onTabSelected(tab) else onCurrentTabReselected() },
                        enabled = tab.enabled
                    )
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            content()
        }
    }
}

private val MainTab.icon
    get() = when (this) {
        // Íconos del Figma (barra 476:4177): inventario y perfil, teñidos según la pestaña.
        MainTab.INVENTORY -> R.drawable.ic_figma_tab_inventory
        MainTab.RECIPES -> R.drawable.ic_figma_tab_recipes
        MainTab.SAVED -> R.drawable.ic_figma_tab_saved
        MainTab.PROFILE -> R.drawable.ic_figma_tab_profile
    }

/**
 * Cambia de pestaña sin apilar: vuelve a "Mi Inventario" como base, conserva el estado de cada pestaña y no repite
 * el destino si ya está arriba (spec 006, plan §7, CA-INV-09-02).
 */
fun NavHostController.navigateToTab(tab: MainTab) {
    val route: Route = when (tab) {
        MainTab.INVENTORY -> Route.Inventory
        MainTab.PROFILE -> Route.Profile
        // Todavía no tienen pantalla: la pestaña está deshabilitada y nunca llega aquí.
        MainTab.RECIPES, MainTab.SAVED -> return
    }
    navigate(route) {
        popUpTo<Route.Inventory> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
