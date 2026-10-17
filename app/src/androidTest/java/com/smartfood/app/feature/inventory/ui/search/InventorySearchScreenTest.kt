package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
 *
 * Las pruebas con el aviso de voz desactivan `autoAdvance`: las ondas de "Habla ahora" son una animación infinita y,
 * si no, la prueba esperaría a que la pantalla quedara quieta.
 */
class InventorySearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<InventorySearchEvent>()

    private fun mostrar(
        state: InventorySearchUiState = InventorySearchUiState(),
        showPermissionRationale: Boolean = false,
        onOpenSettings: () -> Unit = {},
        onEvent: (InventorySearchEvent) -> Unit = { eventos += it }
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                InventorySearchScreen(
                    state = state,
                    onEvent = onEvent,
                    showPermissionRationale = showPermissionRationale,
                    onOpenSettings = onOpenSettings
                )
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
                isVoiceAvailable = true,
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
    fun caInv0701_elAvisoMuestraHablaAhoraMientrasEscucha() {
        composeTestRule.mainClock.autoAdvance = false
        mostrar(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Listening))

        composeTestRule.onNodeWithText("Habla ahora").assertIsDisplayed()
    }

    @Test
    fun caInv0702_elAvisoMuestraPuntosSuspensivosMientrasProcesa() {
        composeTestRule.mainClock.autoAdvance = false
        mostrar(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Processing))

        composeTestRule.onNodeWithText("…").assertIsDisplayed()
    }

    @Test
    fun caInv0705_elAvisoDeErrorPermiteReintentar() {
        mostrar(
            InventorySearchUiState(
                isVoiceAvailable = true,
                voice = VoiceUiState.Failed(SpeechError.NO_MATCH)
            )
        )

        composeTestRule.onNodeWithText("No se entendió. Reinténtalo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.VoiceRetry), eventos)
    }

    @Test
    fun caInv0707_tocarFueraDelAvisoCancelaLaEscucha() {
        composeTestRule.mainClock.autoAdvance = false
        mostrar(InventorySearchUiState(isVoiceAvailable = true, voice = VoiceUiState.Listening))

        composeTestRule.onNodeWithContentDescription("Cerrar el aviso de voz")
            .performTouchInput { click(Offset(5f, 5f)) }

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.VoiceDismissed), eventos)
    }

    @Test
    fun caInv0708_sinServicioDeVozLaTarjetaNoSeMuestra() {
        mostrar(InventorySearchUiState(isVoiceAvailable = false))

        composeTestRule.onNodeWithText("Usa tu voz").assertDoesNotExist()
    }

    @Test
    fun caInv0708_conServicioDeVozLaTarjetaSeMuestra() {
        mostrar(InventorySearchUiState(isVoiceAvailable = true))

        composeTestRule.onNodeWithText("Usa tu voz").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar por voz").assertIsDisplayed()
    }

    @Test
    fun caInv0706_elAvisoDePermisoExplicaYAbreAjustes() {
        var abrioAjustes = false
        mostrar(showPermissionRationale = true, onOpenSettings = { abrioAjustes = true })

        composeTestRule.onNodeWithText("Necesitamos tu micrófono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abrir ajustes").performClick()

        assertTrue(abrioAjustes)
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

        // El punto de estado anuncia el estado, además del color (RNF-INV-04). La fila del Figma no tiene lápiz:
        // toda la fila es un botón que abre el editor (CA-INV-06-08).
        composeTestRule.onNodeWithContentDescription("Próximo a vencer").assertExists()
        composeTestRule.onNode(hasText("Yogur") and hasClickAction(), useUnmergedTree = false).assertExists()

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(listOf<InventorySearchEvent>(InventorySearchEvent.BackClicked), eventos)
    }

    @Test
    fun caInv1002_unResultadoPendienteMuestraElIcono() {
        val pendiente = resultado().let { it.copy(item = it.item.copy(isPendingSync = true)) }
        mostrar(InventorySearchUiState(results = listOf(pendiente, resultado(id = "item-2", name = "Manzana"))))

        // Solo el pendiente lleva el ícono.
        composeTestRule.onAllNodesWithContentDescription("Pendiente de sincronizar").assertCountEquals(1)
    }
}

