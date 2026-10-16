/**
 * Nombre: InventorySearchViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [InventorySearchViewModel] (spec 006, HU-INV-06). Comprueban que el estado sale
 * del dominio (CA-INV-06-01 a 06-03, 06-07) y que navegar es un efecto de un solo uso (CA-INV-06-08).
 */
package com.smartfood.app.feature.inventory.ui.search

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class InventorySearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val repository = FakeInventoryRepository()

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
        searchInventoryUseCase = SearchInventoryUseCase()
    )

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
    fun `la tarjeta de voz todavia no inicia escucha`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEvent(InventorySearchEvent.VoiceClicked)

        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
    }
}
