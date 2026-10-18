package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.testutil.PantallaEnFlujo
import com.smartfood.app.testutil.mostrado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: InventorySearchScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la pantalla de búsqueda sin estado (spec 006, HU-INV-06 y HU-INV-07, T034 y T035). La
 * pantalla recibe un `InventorySearchUiState` construido a mano, así que no hace falta inventario ni voz. Los
 * resultados reutilizan `InventoryItemRow` y los filtros `CategoryChips` de `ui/components/` (revisión de Zhair).
 * Cada prueba monta la pantalla una sola vez y recorre varios estados cambiando el `UiState` (#105).
 *
 * La prueba del aviso de voz desactiva `autoAdvance`: las ondas de "Habla ahora" son una animación infinita y, si no,
 * la prueba esperaría a que la pantalla quedara quieta. Por eso avanza un cuadro a mano después de cada cambio.
 */
class InventorySearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pideAjustes = mutableStateOf(false)
    private var abrioAjustes = false

    private val flujo = PantallaEnFlujo<InventorySearchUiState, InventorySearchEvent>(
        composeTestRule,
        InventorySearchUiState()
    ) { state, onEvent ->
        SmartfoodTheme {
            InventorySearchScreen(
                state = state,
                onEvent = onEvent,
                showPermissionRationale = pideAjustes.value,
                onOpenSettings = { abrioAjustes = true }
            )
        }
    }

    /** Resultado de prueba: por defecto, leche fresca que vence el 15/09/2026. */
    private fun resultado(
        id: String = "item-1",
        name: String = "Leche Entera",
        category: FoodCategory = FoodCategory.DAIRY,
        expiresOn: LocalDate = LocalDate.of(2026, 9, 15),
        status: ExpiryStatus = ExpiryStatus.FRESH,
        pendiente: Boolean = false
    ) = SearchResultUi(InventoryItem(id, name, category, expiresOn, isPendingSync = pendiente), status)

    @Test
    fun caInv0601_0603_0606_0607_0608_0708_1002_rnfInv04_buscarFiltrarYAbrirUnResultado() {
        // CA-INV-06-01 y 07-08: la barra, la tarjeta de voz, los filtros y los resultados; CA-INV-10-02: solo el
        // pendiente lleva el ícono.
        flujo.mostrar(
            InventorySearchUiState(
                isVoiceAvailable = true,
                results = listOf(
                    resultado(status = ExpiryStatus.EXPIRED, expiresOn = LocalDate.of(2026, 9, 1), pendiente = true),
                    resultado(id = "item-2", name = "Manzana", category = FoodCategory.FRUITS, expiresOn = LocalDate.of(2026, 9, 20))
                )
            )
        )
        composeTestRule.onNodeWithContentDescription("Buscar producto…").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar por voz").assertIsDisplayed()
        composeTestRule.mostrado("Usa tu voz", "Dicta muchos ingredientes al mismo tiempo", "Todos", "Leche Entera", "Vence: 01/09/2026",
            "Manzana", "Vence: 20/09/2026")
        // "Frutas" aparece en el filtro y, además, como etiqueta de la categoría del resultado.
        composeTestRule.onAllNodesWithText("Frutas").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Pendiente de sincronizar").assertCountEquals(1)
        // CA-INV-06-08: tocar el resultado lo abre; la flecha atrás tiene descripción (RNF-INV-04).
        composeTestRule.onNodeWithText("Manzana").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").performClick()
        assertEquals(
            "abrir el resultado y volver",
            listOf(InventorySearchEvent.ResultClicked("item-2"), InventorySearchEvent.BackClicked),
            flujo.eventos
        )

        // CA-INV-06-03: tocar un filtro emite su categoría; sin texto no hay equis.
        flujo.paso(InventorySearchUiState(isVoiceAvailable = false))
        composeTestRule.onNodeWithContentDescription("Limpiar búsqueda").assertDoesNotExist()
        composeTestRule.onNodeWithText("Usa tu voz").assertDoesNotExist()
        composeTestRule.onNodeWithText("Frutas").performClick()
        assertEquals("elegir Frutas", listOf<InventorySearchEvent>(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS)), flujo.eventos)

        // CA-INV-06-03, 06-06 y 06-07: el filtro elegido se ve seleccionado, sin coincidencias se avisa y la equis limpia.
        flujo.paso(InventorySearchUiState(query = "zzz", selectedCategory = FoodCategory.FRUITS))
        composeTestRule.onNodeWithText("Frutas").assertIsSelected()
        composeTestRule.onNodeWithText("Todos").assertIsNotSelected()
        composeTestRule.onNodeWithText("No hay productos que coincidan").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        assertEquals("limpiar", listOf<InventorySearchEvent>(InventorySearchEvent.ClearQuery), flujo.eventos)

        // RNF-INV-04: el punto de estado anuncia el estado, además del color. La fila del Figma no tiene lápiz:
        // toda la fila es un botón que abre el editor (CA-INV-06-08).
        flujo.paso(
            InventorySearchUiState(
                results = listOf(resultado(name = "Yogur", expiresOn = LocalDate.of(2026, 9, 12), status = ExpiryStatus.EXPIRING_SOON))
            )
        )
        composeTestRule.onNodeWithContentDescription("Próximo a vencer").assertExists()
        composeTestRule.onNode(hasText("Yogur") and hasClickAction()).assertExists()
    }

    @Test
    fun caInv0701_0702_0707_elAvisoDeVozEscuchaProcesaYSeCierraAlTocarFuera() {
        composeTestRule.mainClock.autoAdvance = false
        flujo.mostrar(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Listening))
        composeTestRule.onNodeWithText("Habla ahora").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Cerrar el aviso de voz").performTouchInput { click(Offset(5f, 5f)) }
        assertEquals("tocar fuera del aviso", listOf<InventorySearchEvent>(InventorySearchEvent.VoiceDismissed), flujo.eventos)

        flujo.paso(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Processing))
        composeTestRule.onNodeWithText("…").assertIsDisplayed()
    }

    @Test
    fun caInv0705_0706_elErrorDeVozReintentaYElPermisoAbreAjustes() {
        flujo.mostrar(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Failed(SpeechError.NO_MATCH)))
        composeTestRule.onNodeWithText("No se entendió. Reinténtalo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").performClick()
        assertEquals("reintentar", listOf<InventorySearchEvent>(InventorySearchEvent.VoiceRetry), flujo.eventos)

        pideAjustes.value = true
        flujo.paso(InventorySearchUiState(isVoiceAvailable = true))
        composeTestRule.onNodeWithText("Necesitamos tu micrófono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abrir ajustes").performClick()
        assertTrue("abrir los ajustes", abrioAjustes)
    }
}
