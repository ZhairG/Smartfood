package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoRepository
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: AddItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [AddItemUseCase]: orden de las operaciones, rollback de la foto y
 * programación del aviso (HU-INV-02, HU-INV-05).
 */
class AddItemUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val log = CallLog()
    private val repository = FakeInventoryRepository(log)
    private val photos = FakePhotoRepository(log)
    private val scheduler = FakeExpiryReminderScheduler(log)
    private val addItem = AddItemUseCase(repository, photos, ValidateItemUseCase(fixedClock(today)), scheduler)

    private val draft = ItemDraft(
        name = " Carne ",
        expiresOn = today.plusDays(5),
        category = FoodCategory.PROTEINS,
        quantityText = "1,5",
        unit = QuantityUnit.KILOGRAMS,
    )
    private val photo = LocalPhoto("/cache/photos/carne.jpg")

    @Test
    fun `CA-INV-02-06 sin foto guarda el producto validado`() = runTest {
        val saved = addItem(draft, photo = null).getOrThrow()

        assertEquals("Carne", saved.name)
        assertEquals(Quantity(1.5, QuantityUnit.KILOGRAMS), saved.quantity)
        assertNull(saved.photo)
        assertEquals(listOf(saved), repository.items.value)
        assertTrue(photos.uploads.isEmpty())
    }

    @Test
    fun `CA-INV-02-06 con foto sube la foto con el id nuevo antes de guardar`() = runTest {
        val saved = addItem(draft, photo).getOrThrow()

        assertEquals(
            listOf("newItemId", "upload:${saved.id}", "saveItem:${saved.id}", "schedule"),
            log.entries
        )
        assertEquals(listOf(saved.id to photo), photos.uploads)
        assertEquals(saved.photo, repository.items.value.single().photo)
    }

    @Test
    fun `CA-INV-02-03 un borrador invalido no genera id ni sube la foto`() = runTest {
        val result = addItem(draft.copy(name = "  "), photo)

        assertEquals(InventoryError.Validation(InventoryField.NAME, InventoryError.Validation.Reason.EMPTY), result.inventoryError)
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun `CA-INV-05-06 si la foto no se sube el producto no se guarda`() = runTest {
        photos.uploadResult = inventoryFailure(InventoryError.PhotoUploadFailed)

        val result = addItem(draft, photo)

        assertEquals(InventoryError.PhotoUploadFailed, result.inventoryError)
        assertTrue(repository.savedItems.isEmpty())
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `CA-INV-05-06 si el guardado falla se borra la foto recien subida`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.Network)

        val result = addItem(draft, photo)

        assertEquals(InventoryError.Network, result.inventoryError)
        assertEquals(1, photos.deletedPaths.size)
        assertTrue(photos.deletedPaths.single().contains("/inventory/item-new-1/"))
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `CA-INV-02-07 si el guardado falla sin foto no se borra nada`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.Network)

        assertEquals(InventoryError.Network, addItem(draft, photo = null).inventoryError)
        assertTrue(photos.deletedPaths.isEmpty())
    }

    @Test
    fun `CA-INV-08-01 guardar un producto programa el aviso diario`() = runTest {
        addItem(draft, photo = null).getOrThrow()
        addItem(draft.copy(name = "Pasta"), photo = null).getOrThrow()

        assertEquals(2, scheduler.scheduleCalls)
    }
}
