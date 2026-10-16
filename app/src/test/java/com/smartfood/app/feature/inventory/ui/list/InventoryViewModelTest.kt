/**
 * Nombre: InventoryViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [InventoryViewModel] para la lista de inventario (spec 006, HU-INV-01,
 * CA-INV-01-01, CA-INV-01-05 y CA-INV-01-07) y de sus efectos de navegación.
 */
package com.smartfood.app.feature.inventory.ui.list

import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
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

class InventoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val repository = FakeInventoryRepository()
    private val observeInventoryUseCase = ObserveInventoryUseCase(
        repository = repository,
        classifyExpiry = ClassifyExpiryUseCase(fixedClock(today))
    )

    private fun createViewModel() = InventoryViewModel(observeInventoryUseCase)

    @Test
    fun `CA-INV-01-01 cuando hay productos se cargan las secciones en el estado`() = runTest {
        repository.items.value = listOf(
            inventoryItem(id = "1", name = "Leche", expiresOn = today.plusDays(1)),
            inventoryItem(id = "2", name = "Manzana", expiresOn = today.plusDays(10))
        )

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(2, state.sections.size)
    }

    @Test
    fun `CA-INV-01-05 cuando el inventario esta vacio isEmpty es true`() = runTest {
        repository.items.value = emptyList()

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.sections.isEmpty())
    }

    @Test
    fun `CA-INV-01-07 un cambio en el repositorio se refleja sin recargar`() = runTest {
        repository.items.value = listOf(
            inventoryItem(id = "1", name = "Leche", expiresOn = today.plusDays(1))
        )
        val viewModel = createViewModel()
        assertEquals(1, viewModel.uiState.value.sections.first().items.size)

        repository.items.value = listOf(
            inventoryItem(id = "1", name = "Leche", expiresOn = today.plusDays(1)),
            inventoryItem(id = "2", name = "Manzana", expiresOn = today.plusDays(10))
        )

        val state = viewModel.uiState.first { it.sections.sumOf { section -> section.items.size } == 2 }
        assertEquals(2, state.sections.sumOf { section -> section.items.size })
    }

    @Test
    fun `AddClicked emite efecto NavigateToEditor con null`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryEvent.AddClicked)

        val effect = viewModel.effects.first()
        assertTrue(effect is InventoryEffect.NavigateToEditor)
        assertNull((effect as InventoryEffect.NavigateToEditor).itemId)
    }

    @Test
    fun `SearchClicked emite efecto NavigateToSearch`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryEvent.SearchClicked)

        val effect = viewModel.effects.first()
        assertEquals(InventoryEffect.NavigateToSearch, effect)
    }

    @Test
    fun `EditClicked emite efecto NavigateToEditor con el id del producto`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryEvent.EditClicked("item-123"))

        val effect = viewModel.effects.first()
        assertTrue(effect is InventoryEffect.NavigateToEditor)
        assertEquals("item-123", (effect as InventoryEffect.NavigateToEditor).itemId)
    }
}
