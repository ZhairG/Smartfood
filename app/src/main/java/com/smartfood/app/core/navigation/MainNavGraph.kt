package com.smartfood.app.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import com.smartfood.app.feature.inventory.ui.inventoryDestinations
import com.smartfood.app.feature.profile.profileDestination

/**
 * Nombre: MainNavGraph.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Grafo principal de la app (usuario con sesión). Arranca en "Mi Inventario" (spec 006, CA-INV-09-01):
 * como `navigateToMainGraph()` limpia toda la pila, "atrás" en esa pestaña cierra la app (CA-INV-09-05). Cada feature
 * aporta sus destinos: inventario (pestaña y pantallas internas) y perfil.
 *
 * @param navController Controlador de navegación de la app.
 */
fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<Route.MainGraph>(startDestination = Route.Inventory) {
        inventoryDestinations(navController)
        profileDestination(navController)
    }
}
