package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.inventoryError
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: GetItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [GetItemUseCase] (spec 006 §13, CA-INV-10-04, T050): el producto que se edita
 * llega con su foto pendiente, si tiene.
 */
class GetItemUseCaseTest {

    private val repository = FakeInventoryRepository()
    private val photoSync = FakePhotoSync()
    private val getItem = GetItemUseCase(repository, photoSync)

    @Test
    fun `CA-INV-10-04 un producto con foto pendiente llega con la foto del telefono`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "leche"))
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")

        val item = getItem("leche").getOrThrow()

        assertEquals("/files/pending/leche/a.jpg", item.localPhotoPath)
        assertTrue(item.isPendingSync)
    }

    @Test
    fun `CA-INV-03-01 un producto sin foto pendiente llega como esta guardado`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "leche"))

        val item = getItem("leche").getOrThrow()

        assertNull(item.localPhotoPath)
        assertFalse(item.isPendingSync)
    }

    @Test
    fun `CA-INV-03-01 un producto que no existe falla con NotFound`() = runTest {
        assertEquals(InventoryError.NotFound, getItem("no-existe").inventoryError)
    }
}
