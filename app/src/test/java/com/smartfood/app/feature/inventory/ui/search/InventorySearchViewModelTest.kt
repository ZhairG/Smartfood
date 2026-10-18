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
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ParseSpokenIngredientsUseCase
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeSpeechToText
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
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
        observeInventoryUseCase = ObserveInventoryUseCase(repository, FakePhotoSync(), ClassifyExpiryUseCase(fixedClock(today))),
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
    fun `CA-INV-06-01 CA-INV-07-08 al abrir muestra todo el inventario con su estado y la tarjeta de voz si hay servicio`() = runTest {
        inventarioDePrueba()

        val state = crearViewModel().uiState.first { !it.isLoading }

        assertEquals("", state.query)
        assertNull(state.selectedCategory)
        assertEquals(listOf("item-1", "item-2", "item-3"), state.results.map { it.item.id })
        assertEquals(ExpiryStatus.EXPIRED, state.results.first { it.item.id == "item-1" }.status)
        assertEquals(ExpiryStatus.FRESH, state.results.first { it.item.id == "item-2" }.status)
        assertTrue("con servicio de voz", state.isVoiceAvailable)

        speech.available = false
        assertFalse("sin servicio de voz", crearViewModel().uiState.first { !it.isLoading }.isVoiceAvailable)
    }

    @Test
    fun `CA-INV-06-02 CA-INV-06-03 CA-INV-06-07 el texto y la categoria filtran y limpiar el texto conserva la categoria`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()

        // CA-INV-06-02: por nombre, sin importar mayúsculas.
        viewModel.onEvent(InventorySearchEvent.QueryChanged("MANZANA"))
        var state = viewModel.uiState.first { !it.isLoading && it.query == "MANZANA" }
        assertEquals(listOf("item-2"), state.results.map { it.item.id })

        // CA-INV-06-03: el filtro de categoría deja solo esa categoría.
        viewModel.onEvent(InventorySearchEvent.ClearQuery)
        viewModel.onEvent(InventorySearchEvent.CategorySelected(FoodCategory.FRUITS))
        state = viewModel.uiState.first { !it.isLoading && it.query.isEmpty() && it.selectedCategory != null }
        assertEquals(FoodCategory.FRUITS, state.selectedCategory)
        assertEquals(listOf("item-2", "item-3"), state.results.map { it.item.id })

        // CA-INV-06-07: limpiar el texto conserva el filtro elegido.
        viewModel.onEvent(InventorySearchEvent.QueryChanged("manz"))
        viewModel.uiState.first { !it.isLoading && it.query == "manz" }
        viewModel.onEvent(InventorySearchEvent.ClearQuery)
        state = viewModel.uiState.first { !it.isLoading && it.query.isEmpty() }
        assertEquals(FoodCategory.FRUITS, state.selectedCategory)
        assertEquals(listOf("item-2", "item-3"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-06-08 tocar un resultado abre su editor y la flecha atras vuelve`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.ResultClicked("item-2"))
        assertEquals(InventorySearchEffect.NavigateToEditor("item-2"), viewModel.effects.first())

        viewModel.onEvent(InventorySearchEvent.BackClicked)
        assertEquals(InventorySearchEffect.NavigateBack, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-07-01 CA-INV-07-06 el microfono pide permiso, con permiso escucha y sin permiso lo explica`() = runTest {
        val conPermiso = crearViewModel()
        conPermiso.onEvent(InventorySearchEvent.VoiceClicked)
        assertEquals(InventorySearchEffect.RequestMicPermission, conPermiso.effects.first())
        conPermiso.onEvent(InventorySearchEvent.VoicePermissionResult(granted = true))
        assertEquals(VoiceUiState.Listening, conPermiso.uiState.first { it.voice != VoiceUiState.Hidden }.voice)
        assertEquals(1, speech.listenCalls)

        val sinPermiso = crearViewModel()
        sinPermiso.onEvent(InventorySearchEvent.VoiceClicked)
        assertEquals(InventorySearchEffect.RequestMicPermission, sinPermiso.effects.first())
        sinPermiso.onEvent(InventorySearchEvent.VoicePermissionResult(granted = false))
        assertEquals(InventorySearchEffect.ShowPermissionRationale, sinPermiso.effects.first())
        assertEquals("negar el permiso no escucha", 1, speech.listenCalls)
    }

    @Test
    fun `CA-INV-07-02 CA-INV-07-03 tras hablar se procesa y el dictado llena la barra con lo que coincide`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Processing)
        viewModel.uiState.first { it.voice == VoiceUiState.Processing }

        speech.send(SpeechEvent.Result(listOf("carne, pollo y leche")))
        val state = viewModel.uiState.first { it.query == "carne, pollo, leche" }
        assertEquals(VoiceUiState.Hidden, state.voice)
        // Solo "Leche Entera" coincide con alguno de los tres ingredientes.
        assertEquals(listOf("item-1"), state.results.map { it.item.id })
    }

    @Test
    fun `CA-INV-07-05 sin entender nada o sin red se muestra el error, la barra no cambia y reintentar vuelve a escuchar`() = runTest {
        inventarioDePrueba()
        val viewModel = crearViewModel()
        empezarEscucha(viewModel)

        speech.send(SpeechEvent.Result(emptyList()))
        val state = viewModel.uiState.first { it.voice is VoiceUiState.Failed }
        assertEquals(VoiceUiState.Failed(SpeechError.NO_MATCH), state.voice)
        assertEquals("", state.query)

        viewModel.onEvent(InventorySearchEvent.VoiceRetry)
        assertEquals(2, speech.listenCalls)
        viewModel.uiState.first { it.voice == VoiceUiState.Listening }

        speech.send(SpeechEvent.Failed(SpeechError.NETWORK))
        assertEquals(
            VoiceUiState.Failed(SpeechError.NETWORK),
            viewModel.uiState.first { it.voice == VoiceUiState.Failed(SpeechError.NETWORK) }.voice
        )
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

    @Test
    fun `CA-INV-06-01 si el inventario no se puede leer la busqueda queda vacia sin cerrar la app`() = runTest {
        // Sin sesión, el repositorio real falla con NotAuthenticated al observar (MainNavigationTest.caInv0904).
        val sinSesion = object : InventoryRepository by repository {
            override fun observeItems(): Flow<List<InventoryItem>> =
                flow { throw InventoryException(InventoryError.NotAuthenticated) }
        }
        val viewModel = InventorySearchViewModel(
            observeInventoryUseCase = ObserveInventoryUseCase(sinSesion, FakePhotoSync(), ClassifyExpiryUseCase(fixedClock(today))),
            searchInventoryUseCase = SearchInventoryUseCase(),
            speechToText = speech,
            parseSpokenIngredientsUseCase = ParseSpokenIngredientsUseCase()
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.results.isEmpty())
    }
}
