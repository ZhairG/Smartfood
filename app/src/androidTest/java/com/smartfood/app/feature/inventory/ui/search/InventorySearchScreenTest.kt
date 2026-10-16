package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: InventorySearchScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la pantalla de búsqueda sin estado (spec 006, HU-INV-06, T034). La pantalla recibe un
 * `InventorySearchUiState` construido a mano, así que no hace falta inventario ni voz. Los resultados reutilizan
 * `InventoryItemRow` y los filtros `CategoryChips` de `ui/components/` (revisión de Zhair).
 */
class InventorySearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<InventorySearchEvent>()

    private fun mostrar(
        state: InventorySearchUiState = InventorySearchUiState(),
        onEvent: (InventorySearchEvent) -> Unit = { eventos += it }
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                InventorySearchScreen(state = state, onEvent = onEvent)
            }
        }
    }

    /** Resultado de prueba: por defecto, leche fresca que vence el 15/09/2026. */
    private fun resultado(
        id: String = "item-1",
        name: String = "Leche Entera",
        category: FoodCategory = FoodCategory.DAIRY,
        expiresOn: LocalDate = LocalDate.of(2026, 9, 15),
        status: ExpiryStatus = ExpiryStatus.FRESH
    ) = SearchResultUi(InventoryItem(id, name, category, expiresOn), status)

    @Test
    fun caInv0601_laBusquedaMuestraLaBarraLaVozLosFiltrosYLosResultados() {
        mostrar(
            InventorySearchUiState(
                results = listOf(
                    resultado(status = ExpiryStatus.EXPIRED, expiresOn = LocalDate.of(2026, 9, 1)),
                    resultado(
                        id = "item-2",
                        name = "Manzana",
                        category = FoodCategory.FRUITS,
                        expiresOn = LocalDate.of(2026, 9, 20)
                    )
                )
            )
        )

        composeTestRule.onNodeWithContentDescription("Buscar producto…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usa tu voz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dicta muchos ingredientes al mismo tiempo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todos").assertIsDisplayed()
        // "Frutas" aparece en el filtro y, además, como etiqueta de la categoría del resultado.
        composeTestRule.onAllNodesWithText("Frutas").onFirst().assertIsDisplayed()

        composeTestRule.onNodeWithText("Leche Entera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vence: 01/09/2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manzana").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vence: 20/09/2026").assertIsDisplayed()
    }

    @Test
    fun caInv0603_elFiltroElegidoSeVeSeleccionado() {
        mostrar(InventorySearchUiState(selectedCategory = FoodCategory.FRUITS))

        composeTestRule.onNodeWithText("Frutas").assertIsSelected()
        composeTestRule.onNodeWithText("Todos").assertIsNotSelected()
    }

    @Test
    fun caInv0603_tocarUnFiltroEmiteLaCategoriaElegida() {
        mostrar()

        composeTestRule.onNodeWithText("Frutas").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS)), eventos)
    }

    @Test
    fun caInv0606_sinCoincidenciasSeAvisaConUnMensaje() {
        mostrar(InventorySearchUiState(query = "zzz"))

        composeTestRule.onNodeWithText("No hay productos que coincidan").assertIsDisplayed()
    }

    @Test
    fun caInv0607_laEquisLimpiaLaBusqueda() {
        mostrar(InventorySearchUiState(query = "carne"))

        composeTestRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.ClearQuery), eventos)
    }

    @Test
    fun sinTextoLaEquisNoAparece() {
        mostrar()

        composeTestRule.onNodeWithContentDescription("Limpiar búsqueda").assertDoesNotExist()
    }

    @Test
    fun caInv0608_tocarUnResultadoEmiteSuProducto() {
        mostrar(
            InventorySearchUiState(
                results = listOf(resultado(id = "item-2", name = "Manzana", category = FoodCategory.FRUITS))
            )
        )

        composeTestRule.onNodeWithText("Manzana").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.ResultClicked("item-2")), eventos)
    }

    @Test
    fun rnfInv04_laFlechaAtrasTieneDescripcionYElEstadoNoSeLeeSoloConColor() {
        mostrar(
            InventorySearchUiState(
                results = listOf(
                    resultado(
                        name = "Yogur",
                        expiresOn = LocalDate.of(2026, 9, 12),
                        status = ExpiryStatus.EXPIRING_SOON
                    )
                )
            )
        )

        // La fila reutilizada anuncia el estado en su descripción, además del color (RNF-INV-04).
        composeTestRule.onNodeWithContentDescription("Próximo a vencer").assertExists()
        composeTestRule.onNodeWithContentDescription("Editar Yogur").assertExists()

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.BackClicked), eventos)
    }
}
