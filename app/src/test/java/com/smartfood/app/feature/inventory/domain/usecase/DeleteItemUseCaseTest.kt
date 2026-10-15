package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoRepository
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
    private val photos = FakePhotoRepository(log)
    private val deleteItem = DeleteItemUseCase(repository, photos)

    private val photo = ItemPhoto("users/uid/inventory/carne/foto.jpg", "https://fotos.test/foto")
    private val withPhoto = inventoryItem(id = "carne", name = "Carne", photo = photo)

    @Test
    fun `CA-INV-04-02 eliminar borra el documento y despues su foto`() = runTest {
        repository.items.value = listOf(withPhoto)

        deleteItem(withPhoto).getOrThrow()

        assertTrue(repository.items.value.isEmpty())
        assertEquals(listOf("deleteItem:carne", "deletePhoto:${photo.path}"), log.entries)
    }

    @Test
    fun `CA-INV-04-02 un producto sin foto no borra nada en la nube`() = runTest {
        deleteItem(withPhoto.copy(photo = null)).getOrThrow()

        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-04-02 si el documento no se borra la foto se conserva`() = runTest {
        repository.deleteResult = inventoryFailure(InventoryError.Network)

        val result = deleteItem(withPhoto)

        assertEquals(InventoryError.Network, result.inventoryError)
        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-04-02 si falla el borrado de la foto el producto queda eliminado`() = runTest {
        photos.deleteResult = inventoryFailure(InventoryError.Network)

        assertTrue(deleteItem(withPhoto).isSuccess)
    }
}
