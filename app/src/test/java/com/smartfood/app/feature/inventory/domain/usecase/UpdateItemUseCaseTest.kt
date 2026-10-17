package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
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
    private val photoSync = FakePhotoSync(log)
    private val updateItem = UpdateItemUseCase(repository, photoSync, ValidateItemUseCase(fixedClock(today)))

    private val oldPhoto = ItemPhoto("users/uid/inventory/carne/vieja.jpg", "https://fotos.test/vieja")
    private val original = inventoryItem(
        id = "carne",
        name = "Carne",
        category = FoodCategory.PROTEINS,
        expiresOn = today.plusDays(5),
        photo = oldPhoto,
    )
    private val draft = ItemDraft("Carne molida", original.expiresOn, FoodCategory.PROTEINS)
    private val newPhoto = LocalPhoto("/files/photos/nueva.jpg")

    init {
        repository.items.value = listOf(original)
    }

    @Test
    fun `CA-INV-03-02 guardar con datos validos actualiza el producto sin tocar su foto`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Keep).getOrThrow()

        assertEquals("Carne molida", updated.item.name)
        assertEquals(oldPhoto, updated.item.photo)
        assertEquals(WriteOutcome.SYNCED, updated.outcome)
        assertEquals(listOf(false), repository.photoChangedFlags)
        assertEquals(listOf("updateItem:carne"), log.entries)
    }

    @Test
    fun `CA-INV-03-03 la edicion conserva una fecha ya vencida`() = runTest {
        val expired = original.copy(expiresOn = today.minusDays(2))

        val updated = updateItem(expired, draft.copy(expiresOn = expired.expiresOn), PhotoEdit.Keep).getOrThrow()

        assertEquals(expired.expiresOn, updated.item.expiresOn)
    }

    @Test
    fun `CA-INV-05-04 quitar la foto la borra del producto y encola su borrado despues de guardar`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Remove).getOrThrow()

        assertNull(updated.item.photo)
        assertEquals(listOf(true), repository.photoChangedFlags)
        assertEquals(
            listOf("updateItem:carne", "cancelUpload:carne", "enqueueDelete:${oldPhoto.path}"),
            log.entries
        )
    }

    @Test
    fun `CA-INV-05-05 una foto nueva se encola para reemplazar a la anterior despues de subirse`() = runTest {
        val updated = updateItem(original, draft, PhotoEdit.Replace(newPhoto)).getOrThrow()

        assertEquals(listOf("updateItem:carne", "enqueueUpload:carne"), log.entries)
        assertEquals(listOf(Triple("carne", newPhoto, oldPhoto.path)), photoSync.uploads)
        // Mientras la nueva se sube, el producto conserva la anterior y no se borra nada todavía.
        assertEquals(listOf(false), repository.photoChangedFlags)
        assertEquals(oldPhoto, repository.items.value.single().photo)
        assertTrue(photoSync.deletes.isEmpty())
        assertTrue(updated.item.isPendingSync)
    }

    @Test
    fun `CA-INV-05-05 agregar foto a un producto sin foto no reemplaza nada`() = runTest {
        updateItem(original.copy(photo = null), draft, PhotoEdit.Replace(newPhoto)).getOrThrow()

        assertNull(photoSync.uploads.single().third)
    }

    @Test
    fun `CA-INV-10-02 sin conexion la edicion queda guardada en el telefono`() = runTest {
        repository.outcome = WriteOutcome.PENDING

        val updated = updateItem(original, draft, PhotoEdit.Keep).getOrThrow()

        assertEquals(WriteOutcome.PENDING, updated.outcome)
        assertTrue(updated.item.isPendingSync)
    }

    @Test
    fun `CA-INV-02-07 si el guardado falla no se encola ni se borra ninguna foto`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.PermissionDenied)

        val replace = updateItem(original, draft, PhotoEdit.Replace(newPhoto))
        val remove = updateItem(original, draft, PhotoEdit.Remove)

        assertEquals(InventoryError.PermissionDenied, replace.inventoryError)
        assertEquals(InventoryError.PermissionDenied, remove.inventoryError)
        assertTrue(photoSync.uploads.isEmpty())
        assertTrue(photoSync.deletes.isEmpty())
        assertEquals(listOf(original), repository.items.value)
    }
}
