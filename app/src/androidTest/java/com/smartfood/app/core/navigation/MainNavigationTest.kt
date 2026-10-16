package com.smartfood.app.core.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.smartfood.app.HiltTestActivity
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.ui.authGraph
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: MainNavigationTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la navegación principal con sesión (spec 006, HU-INV-09, T030): arranque en
 * "Mi Inventario", barra inferior, pestañas sin apilar, pantallas internas sin barra y "atrás" en la pestaña de
 * inicio. No toca "Cerrar sesión": las pruebas comparten los datos de la app instalada y cerrarían la sesión real
 * (CA-05-03 lo cubre `ProfileViewModelTest`).
 */
@HiltAndroidTest
class MainNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: NavHostController

    @Before
    fun mostrarGrafoPrincipal() {
        hiltRule.inject()
        composeTestRule.setContent {
            SmartfoodTheme {
                val controller = rememberNavController()
                navController = controller
                NavHost(navController = controller, startDestination = Route.MainGraph) {
                    mainGraph(controller)
                    authGraph(controller)
                }
            }
        }
    }

    /** Destinos de pantalla en la pila (sin los grafos). */
    private fun pantallasEnLaPila(): Int =
        navController.currentBackStack.value.count { it.destination.navigatorName == "composable" }

    @Test
    fun caInv0901_caInv0902_arrancaEnMiInventarioConLaBarraYLaPestanaResaltada() {
        composeTestRule.onNodeWithText("Mi Inventario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inventario").assertIsSelected()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun caInv0903_laPestanaPerfilMuestraElSaludoYCerrarSesion() {
        composeTestRule.onNodeWithText("Perfil").performClick()

        composeTestRule.onNodeWithText("Perfil").assertIsSelected()
        composeTestRule.onNodeWithText("Cerrar Sesión").assertIsDisplayed()
    }

    @Test
    fun caInv0902_cambiarDePestanaNoApila() {
        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.onNodeWithText("Inventario").performClick()
        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.waitForIdle()

        // Inventario queda como base y Perfil encima: dos pantallas, nunca más.
        assertEquals(2, pantallasEnLaPila())
        composeTestRule.onNodeWithText("Inventario").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, pantallasEnLaPila())
    }

    @Test
    fun caInv0904_laBusquedaMuestraLaBarraConInventarioResaltado() {
        composeTestRule.runOnUiThread { navController.navigate(Route.InventorySearch) }
        composeTestRule.waitForIdle()

        // La búsqueda ya es la pantalla real (T034, #42). Se comprueba un elemento que siempre está (el filtro
        // "Todos"); la tarjeta "Usa tu voz" depende del servicio de reconocimiento del dispositivo (CA-INV-07-08).
        composeTestRule.onNodeWithText("Todos").assertIsDisplayed()
        // Como el Figma, la búsqueda muestra la barra inferior con Inventario resaltado (T055).
        composeTestRule.onNodeWithText("Inventario").assertIsSelected()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun caInv0904_tocarInventarioEnLaBusquedaVuelveALaLista() {
        composeTestRule.runOnUiThread { navController.navigate(Route.InventorySearch) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Inventario").performClick()
        composeTestRule.waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<Route.Inventory>() == true)
        composeTestRule.onNodeWithText("Mi Inventario").assertIsDisplayed()
    }

    @Test
    fun caInv0904_tocarInventarioEnElEditorVuelveALaLista() {
        composeTestRule.runOnUiThread {
            navController.navigate(Route.InventorySearch)
            navController.navigate(Route.ProductEditor(null))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Inventario").performClick()
        composeTestRule.waitForIdle()

        // Pasa por el descarte de "atrás" y llega a la lista, no a la búsqueda de la que vino.
        assertTrue(navController.currentDestination?.hasRoute<Route.Inventory>() == true)
        composeTestRule.onNodeWithText("Mi Inventario").assertIsDisplayed()
    }

    @Test
    fun caInv0905_enMiInventarioNoHayNadaDebajo_atrasCierraLaApp() {
        composeTestRule.onNodeWithText("Mi Inventario").assertIsDisplayed()

        assertTrue(navController.currentDestination?.hasRoute<Route.Inventory>() == true)
        // Sin pantalla anterior, "atrás" del sistema termina la actividad en lugar de volver a la Splash o al login.
        assertNull(navController.previousBackStackEntry?.takeIf { it.destination.navigatorName == "composable" })
    }
}
