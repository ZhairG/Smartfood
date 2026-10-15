package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoRepository
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryError
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: UpdateItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [UpdateItemUseCase]: cambios de datos, reemplazo y borrado de la foto, y
 * rollback (HU-INV-03, HU-INV-05).
 */
class UpdateItemUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val log = CallLog()
    private val repository = FakeInventoryRepository(log)
    private val photos = FakePhotoRepository(log)
    private val updateItem = UpdateItemUseCase(repository, photos, ValidateItemUseCase(fixedClock(today)))

    private val oldPhoto = ItemPhoto("users/uid/inventory/carne/vieja.jpg", "https://fotos.test/vieja")
    private val original = inventoryItem(
        id = "carne",
        name = "Carne",
        category = FoodCategory.PROTEINS,
        expiresOn = today.plusDays(5),
        photo = oldPhoto,
    )
    private val draft = ItemDraft("Carne molida", original.expiresOn, FoodCategory.PROTEINS)
    private val newPhoto = LocalPhoto("/cache/photos/nueva.jpg")

    init {
        repository.items.value = listOf(original)
    }

    @Test
    fun `CA-INV-03-02 guardar con datos validos actualiza el producto y conserva su foto`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Keep).getOrThrow()

        assertEquals("Carne molida", updated.name)
        assertEquals(oldPhoto, updated.photo)
        assertEquals(listOf(updated), repository.items.value)
        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-03-03 la edicion conserva una fecha ya vencida`() = runTest {
        val expired = original.copy(expiresOn = today.minusDays(2))

        val updated = updateItem(expired, draft.copy(expiresOn = expired.expiresOn), PhotoEdit.Keep).getOrThrow()

        assertEquals(expired.expiresOn, updated.expiresOn)
    }

    @Test
    fun `CA-INV-05-04 quitar la foto la borra de la nube despues de guardar`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Remove).getOrThrow()

        assertNull(updated.photo)
        assertEquals(listOf("saveItem:carne", "deletePhoto:${oldPhoto.path}"), log.entries)
    }

    @Test
    fun `CA-INV-05-05 una foto nueva reemplaza a la anterior y la anterior se borra al guardar`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Replace(newPhoto)).getOrThrow()

        val uploadedPath = updated.photo!!.path
        assertEquals(
            listOf("upload:carne", "saveItem:carne", "deletePhoto:${oldPhoto.path}"),
            log.entries
        )
        assertTrue(uploadedPath != oldPhoto.path)
    }

    @Test
    fun `CA-INV-05-05 agregar foto a un producto sin foto no borra nada`() = runTest {
        updateItem(original.copy(photo = null), draft, PhotoEdit.Replace(newPhoto)).getOrThrow()

        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-05-06 si el guardado falla se borra la foto nueva y se conserva la anterior`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.Network)

        val result = updateItem(original, draft, PhotoEdit.Replace(newPhoto))

        assertEquals(InventoryError.Network, result.inventoryError)
        assertEquals(1, photos.deletedPaths.size)
        assertTrue(photos.deletedPaths.single() != oldPhoto.path)
        assertEquals(listOf(original), repository.items.value)
    }

    @Test
    fun `CA-INV-05-06 si la foto nueva no se sube no se guarda nada`() = runTest {
        photos.uploadResult = inventoryFailure(InventoryError.PhotoUploadFailed)

        val result = updateItem(original, draft, PhotoEdit.Replace(newPhoto))

        assertEquals(InventoryError.PhotoUploadFailed, result.inventoryError)
        assertTrue(repository.savedItems.isEmpty())
        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-05-04 si quitar la foto falla en el guardado la foto anterior no se borra`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.Network)

        updateItem(original, draft, PhotoEdit.Remove)

        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-05-05 si falla el borrado de la foto anterior el cambio se guarda igual`() = runTest {
        photos.deleteResult = inventoryFailure(InventoryError.Network)

        val result = updateItem(original, draft, PhotoEdit.Replace(newPhoto))

        assertTrue(result.isSuccess)
        assertEquals("Carne molida", repository.items.value.single().name)
    }
}
