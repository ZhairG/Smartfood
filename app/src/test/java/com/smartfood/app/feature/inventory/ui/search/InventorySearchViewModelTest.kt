/**
 * Nombre: InventorySearchViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [InventorySearchViewModel] (spec 006, HU-INV-06 y HU-INV-07). Comprueban que el
 * estado sale del dominio (CA-INV-06-01 a 06-03, 06-07), que navegar es un efecto de un solo uso (CA-INV-06-08) y
 * que la voz pide permiso, escucha, llena la barra, cancela y respeta el filtro (CA-INV-07-01 a 07-09).
 */
package com.smartfood.app.feature.inventory.ui.search

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ParseSpokenIngredientsUseCase
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakeSpeechToText
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class InventorySearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val repository = FakeInventoryRepository()
    private val speech = FakeSpeechToText()

    /** Inventario de prueba: un lácteo vencido y dos frutas frescas con la misma fecha. */
    private fun inventarioDePrueba() {
        repository.items.value = listOf(
            inventoryItem(
                id = "item-1",
                name = "Leche Entera",
                category = FoodCategory.DAIRY,
                expiresOn = LocalDate.of(2026, 9, 1)
            ),
            inventoryItem(
                id = "item-2",
                name = "Manzana",
                category = FoodCategory.FRUITS,
                expiresOn = LocalDate.of(2026, 9, 20)
            ),
            inventoryItem(
                id = "item-3",
                name = "Pera",
                category = FoodCategory.FRUITS,
                expiresOn = LocalDate.of(2026, 9, 21)
            )
        )
    }

    private fun crearViewModel() = InventorySearchViewModel(
        observeInventoryUseCase = ObserveInventoryUseCase(repository, ClassifyExpiryUseCase(fixedClock(today))),
        searchInventoryUseCase = SearchInventoryUseCase(),
        speechToText = speech,
        parseSpokenIngredientsUseCase = ParseSpokenIngredientsUseCase()
    )

    /** Concede el permiso y deja la escucha activa, como al tocar el micrófono (CA-INV-07-01). */
    private fun empezarEscucha(viewModel: InventorySearchViewModel) {
        viewModel.onEvent(InventorySearchEvent.VoiceClicked)
        viewModel.onEvent(InventorySearchEvent.VoicePermissionResult(granted = true))
    }

    @Test
    fun `CA-INV-06-01 al abrir muestra todo el inventario con su estado`() = runTest {
        inventarioDePrueba()

        val state = crearViewModel().uiState.first { !it.isLoading }

        assertEquals("", state.query)
        assertNull(state.selectedCategory)
        assertEquals(listOf("item-1", "item-2", "item-3"), state.results.map { it.item.id })
        assertEquals(ExpiryStatus.EXPIRED, state.results.first { it.item.id == "item-1" }.status)
        assertEquals(ExpiryStatus.FRESH, state.results.first { it.item.id == "item-2" }.status)
    }

    @Test
    fun `CA-INV-06-02 escribir en la barra filtra por nombre sin importar mayusculas`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.QueryChanged("MANZANA"))

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals("MANZANA", state.query)
        assertEquals(listOf("item-2"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-06-03 el filtro de categoria deja solo esa categoria`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS))

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(FoodCategory.FRUITS, state.selectedCategory)
        assertEquals(listOf("item-2", "item-3"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-06-07 limpiar el texto conserva el filtro elegido`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        viewModel.onEvent(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS))
        viewModel.onEvent(InventorySearchEvent.QueryChanged("manz"))
        viewModel.uiState.first { !it.isLoading && it.query == "manz" }

        viewModel.onEvent(InventorySearchEvent.ClearQuery)

        val state = viewModel.uiState.first { !it.isLoading && it.query.isEmpty() }
        assertEquals(FoodCategory.FRUITS, state.selectedCategory)
        assertEquals(listOf("item-2", "item-3"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-06-08 tocar un resultado navega al editor de ese producto`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.ResultClicked("item-2"))

        assertEquals(InventorySearchEffect.NavigateToEditor("item-2"), viewModel.effects.first())
    }

    @Test
    fun `al tocar la flecha atras vuelve a la pantalla anterior`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.BackClicked)

        assertEquals(InventorySearchEffect.NavigateBack, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-07-01 el microfono pide permiso y con el permiso escucha`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.VoiceClicked)
        assertEquals(InventorySearchEffect.RequestMicPermission, viewModel.effects.first())

        viewModel.onEvent(InventorySearchEvent.VoicePermissionResult(granted = true))

        assertEquals(VoiceUiState.Listening, viewModel.uiState.first { it.voice != VoiceUiState.Hidden }.voice)
        assertEquals(1, speech.listenCalls)
    }

    @Test
    fun `CA-INV-07-02 tras hablar el aviso muestra los puntos suspensivos`() = runTest {
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Processing)

        assertEquals(
            VoiceUiState.Processing,
            viewModel.uiState.first { it.voice == VoiceUiState.Processing }.voice
        )
    }

    @Test
    fun `CA-INV-07-03 el dictado llena la barra y muestra lo que coincide`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Result(listOf("carne, pollo y leche")))

        val state = viewModel.uiState.first { it.query == "carne, pollo, leche" }
        assertEquals("carne, pollo, leche", state.query)
        assertEquals(VoiceUiState.Hidden, state.voice)
        // Solo "Leche Entera" coincide con alguno de los tres ingredientes.
        assertEquals(listOf("item-1"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-07-05 sin entender nada muestra el error y no cambia la barra`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Result(emptyList()))

        val state = viewModel.uiState.first { it.voice is VoiceUiState.Failed }
        assertEquals(VoiceUiState.Failed(SpeechError.NO_MATCH), state.voice)
        assertEquals("", state.query)
    }

    @Test
    fun `CA-INV-07-05 el error de red del reconocedor se muestra`() = runTest {
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Failed(SpeechError.NETWORK))

        assertEquals(
            VoiceUiState.Failed(SpeechError.NETWORK),
            viewModel.uiState.first { it.voice is VoiceUiState.Failed }.voice
        )
    }

    @Test
    fun `CA-INV-07-05 reintentar vuelve a escuchar`() = runTest {
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)
        speech.send(SpeechEvent.Result(emptyList()))
        viewModel.uiState.first { it.voice is VoiceUiState.Failed }

        viewModel.onEvent(InventorySearchEvent.VoiceRetry)

        assertEquals(2, speech.listenCalls)
        assertEquals(VoiceUiState.Listening, viewModel.uiState.first { it.voice == VoiceUiState.Listening }.voice)
    }

    @Test
    fun `CA-INV-07-06 negar el permiso muestra la explicacion y no escucha`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.VoiceClicked)
        assertEquals(InventorySearchEffect.RequestMicPermission, viewModel.effects.first())

        viewModel.onEvent(InventorySearchEvent.VoicePermissionResult(granted = false))

        assertEquals(InventorySearchEffect.ShowPermissionRationale, viewModel.effects.first())
        assertEquals(0, speech.listenCalls)
    }

    @Test
    fun `CA-INV-07-07 cerrar el aviso cancela la escucha y deja la barra igual`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)
        viewModel.uiState.first { it.voice == VoiceUiState.Listening }

        viewModel.onEvent(InventorySearchEvent.VoiceDismissed)

        val state = viewModel.uiState.first { it.voice == VoiceUiState.Hidden }
        assertEquals(VoiceUiState.Hidden, state.voice)
        assertEquals("", state.query)
        assertEquals(1, speech.cancellations)
        assertFalse(speech.isListening)
    }

    @Test
    fun `CA-INV-07-08 sin servicio de voz la tarjeta no se muestra`() = runTest {
        speech.available = false

        val state = crearViewModel().uiState.first { !it.isLoading }

        assertFalse(state.isVoiceAvailable)
    }

    @Test
    fun `CA-INV-07-08 con servicio de voz la tarjeta se muestra`() = runTest {
        val state = crearViewModel().uiState.first { !it.isLoading }

        assertTrue(state.isVoiceAvailable)
    }

    @Test
    fun `CA-INV-07-09 el dictado respeta el filtro de categoria`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        viewModel.onEvent(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS))
        viewModel.uiState.first { it.selectedCategory == FoodCategory.FRUITS }
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Result(listOf("manzana, pera")))

        val state = viewModel.uiState.first { it.query == "manzana, pera" }
        assertEquals(listOf("item-2", "item-3"), state.results.map { it.item.id })
        assertEquals(VoiceUiState.Hidden, state.voice)
    }
}
