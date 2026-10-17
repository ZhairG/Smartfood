package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.testutil.CallLog
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val photoSync = FakePhotoSync(log)
    private val scheduler = FakeExpiryReminderScheduler(log)
    private val addItem = AddItemUseCase(repository, photoSync, ValidateItemUseCase(fixedClock(today)), scheduler)

    private val draft = ItemDraft(
        name = " Carne ",
        expiresOn = today.plusDays(5),
        category = FoodCategory.PROTEINS,
        quantityText = "1,5",
        unit = QuantityUnit.KILOGRAMS,
    )
    private val photo = LocalPhoto("/files/photos/carne.jpg")

    @Test
    fun `CA-INV-02-06 sin foto guarda el producto validado`() = runTest {
        val saved = addItem(draft, photo = null).getOrThrow()

        assertEquals("Carne", saved.item.name)
        assertEquals(Quantity(1.5, QuantityUnit.KILOGRAMS), saved.item.quantity)
        assertNull(saved.item.photo)
        assertEquals(WriteOutcome.SYNCED, saved.outcome)
        assertFalse(saved.item.isPendingSync)
        assertEquals(listOf(saved.item), repository.items.value)
        assertTrue(photoSync.uploads.isEmpty())
    }

    @Test
    fun `CA-INV-05-06 con foto guarda el producto y despues encola la foto con el mismo id`() = runTest {
        val saved = addItem(draft, photo).getOrThrow()

        assertEquals(
            listOf("newItemId", "createItem:${saved.item.id}", "enqueueUpload:${saved.item.id}", "schedule"),
            log.entries
        )
        assertEquals(listOf(Triple(saved.item.id, photo, null)), photoSync.uploads)
        // La foto todavía no está en la nube: el producto se guarda sin foto y queda pendiente.
        assertNull(repository.items.value.single().photo)
        assertTrue(saved.item.isPendingSync)
    }

    @Test
    fun `CA-INV-10-02 sin conexion el producto queda guardado en el telefono`() = runTest {
        repository.outcome = WriteOutcome.PENDING

        val saved = addItem(draft, photo = null).getOrThrow()

        assertEquals(WriteOutcome.PENDING, saved.outcome)
        assertTrue(saved.item.isPendingSync)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun `CA-INV-02-03 un borrador invalido no genera id ni encola la foto`() = runTest {
        val result = addItem(draft.copy(name = "  "), photo)

        assertEquals(InventoryError.Validation(InventoryField.NAME, InventoryError.Validation.Reason.EMPTY), result.inventoryError)
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun `CA-INV-02-07 si el guardado falla no se encola la foto ni se programa el aviso`() = runTest {
        repository.saveResult = inventoryFailure(InventoryError.PermissionDenied)

        val result = addItem(draft, photo)

        assertEquals(InventoryError.PermissionDenied, result.inventoryError)
        assertTrue(photoSync.uploads.isEmpty())
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `CA-INV-08-01 guardar un producto programa el aviso diario`() = runTest {
        addItem(draft, photo = null).getOrThrow()
        addItem(draft.copy(name = "Pasta"), photo = null).getOrThrow()

        assertEquals(2, scheduler.scheduleCalls)
    }
}
