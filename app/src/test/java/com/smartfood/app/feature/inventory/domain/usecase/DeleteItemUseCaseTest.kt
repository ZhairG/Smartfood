package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.inventoryError
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: DeleteItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [DeleteItemUseCase] (HU-INV-04).
 */
class DeleteItemUseCaseTest {

    private val log = CallLog()
    private val repository = FakeInventoryRepository(log)
    private val photoSync = FakePhotoSync(log)
    private val deleteItem = DeleteItemUseCase(repository, photoSync)

    private val photo = ItemPhoto("users/uid/inventory/carne/foto.jpg", "https://fotos.test/foto")
    private val withPhoto = inventoryItem(id = "carne", name = "Carne", photo = photo)

    @Test
    fun `CA-INV-04-02 eliminar borra el documento y despues encola el borrado de su foto`() = runTest {
        repository.items.value = listOf(withPhoto)

        assertEquals(WriteOutcome.SYNCED, deleteItem(withPhoto).getOrThrow())

        assertTrue(repository.items.value.isEmpty())
        assertEquals(listOf("deleteItem:carne", "cancelUpload:carne", "enqueueDelete:${photo.path}"), log.entries)
    }

    @Test
    fun `CA-INV-04-02 un producto sin foto en la nube solo cancela una subida pendiente`() = runTest {
        deleteItem(withPhoto.copy(photo = null)).getOrThrow()

        assertEquals(listOf("carne"), photoSync.cancelled)
        assertTrue(photoSync.deletes.isEmpty())
    }

    @Test
    fun `CA-INV-10-03 sin conexion el producto se borra en el telefono y su foto se borra despues`() = runTest {
        repository.items.value = listOf(withPhoto)
        repository.outcome = WriteOutcome.PENDING

        assertEquals(WriteOutcome.PENDING, deleteItem(withPhoto).getOrThrow())

        assertTrue(repository.items.value.isEmpty())
        assertEquals(listOf(photo.path), photoSync.deletes)
    }

    @Test
    fun `CA-INV-04-02 si el documento no se borra la foto se conserva`() = runTest {
        repository.deleteResult = inventoryFailure(InventoryError.PermissionDenied)

        val result = deleteItem(withPhoto)

        assertEquals(InventoryError.PermissionDenied, result.inventoryError)
        assertTrue(photoSync.deletes.isEmpty())
        assertTrue(photoSync.cancelled.isEmpty())
    }
}
