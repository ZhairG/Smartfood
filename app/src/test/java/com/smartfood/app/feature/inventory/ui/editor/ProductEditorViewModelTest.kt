/**
 * Nombre: ProductEditorViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ProductEditorViewModel] (spec 006, T032): alta, edición, eliminación, foto,
 * errores y navegación, con los casos de uso reales sobre los fakes de `testutil/`.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.usecase.AddItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.PhotoEdit
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryPreferences
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ProductEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val repository = FakeInventoryRepository()
    private val photos = FakePhotoRepository()
    private val scheduler = FakeExpiryReminderScheduler()
    private val preferences = FakeInventoryPreferences()
    private val validate = ValidateItemUseCase(fixedClock(today))

    private fun createViewModel(itemId: String? = null) = ProductEditorViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ProductEditorViewModel.ITEM_ID_KEY to itemId)),
        repository = repository,
        validate = validate,
        addItem = AddItemUseCase(repository, photos, validate, scheduler),
        updateItem = UpdateItemUseCase(repository, photos, validate),
        deleteItem = DeleteItemUseCase(repository, photos),
        preferences = preferences,
    )

    private fun ProductEditorViewModel.fillValidForm() {
        onEvent(ProductEditorEvent.NameChanged("Carne"))
        onEvent(ProductEditorEvent.ExpiryPicked(today.plusDays(5)))
        onEvent(ProductEditorEvent.CategoryPicked(FoodCategory.PROTEINS))
    }

    /** Deja pasar los 2 s del diálogo de resultado, que corren en el dispatcher principal de la prueba. */
    private fun advanceResultDialog() = mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `CA-INV-02-02 el boton se habilita solo con nombre fecha y categoria`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(ProductEditorEvent.NameChanged("Carne"))
        viewModel.onEvent(ProductEditorEvent.ExpiryPicked(today.plusDays(5)))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(ProductEditorEvent.CategoryPicked(FoodCategory.PROTEINS))
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `CA-INV-02-08 la cantidad no cambia el estado del boton`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged("1,5"))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged(""))
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `CA-INV-02-06 guardar un alta muestra Producto Guardado y vuelve a la lista`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged("1,5"))
        viewModel.onEvent(ProductEditorEvent.UnitPicked(QuantityUnit.KILOGRAMS))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(EditorDialog.ProductSaved, viewModel.uiState.value.dialog)
        val saved = repository.savedItems.single()
        assertEquals("Carne", saved.name)
        assertEquals(Quantity(1.5, QuantityUnit.KILOGRAMS), saved.quantity)
        assertEquals(1, scheduler.scheduleCalls)

        advanceResultDialog()
        assertNull(viewModel.uiState.value.dialog)
        assertEquals(
            listOf(ProductEditorEffect.RequestNotificationPermission, ProductEditorEffect.NavigateBack),
            viewModel.effects.take(2).toList()
        )
    }

    @Test
    fun `CA-INV-02-07 si el guardado falla se ve el error y el formulario se conserva`() = runTest {
        repository.saveResult = Result.failure(InventoryException(InventoryError.Network))
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        val state = viewModel.uiState.value
        assertEquals(InventoryError.Network, state.generalError)
        assertEquals("Carne", state.name)
        assertFalse(state.isSaving)
        assertNull(state.dialog)
    }

    @Test
    fun `CA-INV-02-09 una cantidad invalida marca el campo en rojo y no guarda`() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged("0"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(InventoryField.QUANTITY, viewModel.uiState.value.errorField)
        assertTrue(repository.savedItems.isEmpty())
    }

    @Test
    fun `CA-INV-03-01 la edicion carga los datos del producto`() {
        repository.items.value = listOf(
            inventoryItem(
                id = "carne",
                name = "Carne",
                category = FoodCategory.PROTEINS,
                expiresOn = today.plusDays(3),
                quantity = Quantity(2.0, QuantityUnit.KILOGRAMS),
                photo = ItemPhoto("users/u/inventory/carne/a.jpg", "https://fotos.test/a.jpg")
            )
        )

        val state = createViewModel(itemId = "carne").uiState.value

        assertEquals(EditorMode.EDIT, state.mode)
        assertEquals("Carne", state.name)
        assertEquals(FoodCategory.PROTEINS, state.category)
        assertEquals("2", state.quantityText)
        assertEquals(QuantityUnit.KILOGRAMS, state.unit)
        assertEquals(PhotoUiState.Remote("https://fotos.test/a.jpg"), state.photo)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun `CA-INV-03-02 guardar una edicion muestra Guardado con el nombre`() {
        repository.items.value = listOf(inventoryItem(id = "carne", name = "Carne", expiresOn = today.plusDays(3)))
        val viewModel = createViewModel(itemId = "carne")
        viewModel.onEvent(ProductEditorEvent.NameChanged("Carne molida"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(EditorDialog.ProductUpdated("Carne molida"), viewModel.uiState.value.dialog)
        assertEquals("Carne molida", repository.savedItems.single().name)
    }

    @Test
    fun `CA-INV-03-04 volver sin guardar no cambia el producto y descarta la foto nueva`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "carne", name = "Carne", expiresOn = today.plusDays(3)))
        val viewModel = createViewModel(itemId = "carne")
        viewModel.onEvent(ProductEditorEvent.NameChanged("Otro nombre"))
        viewModel.onEvent(ProductEditorEvent.PhotoCaptured("/fotos/nueva.jpg"))

        viewModel.onEvent(ProductEditorEvent.BackClicked)

        assertTrue(repository.savedItems.isEmpty())
        assertEquals(
            listOf(ProductEditorEffect.DeleteLocalPhoto("/fotos/nueva.jpg"), ProductEditorEffect.NavigateBack),
            viewModel.effects.take(2).toList()
        )
    }

    @Test
    fun `CA-INV-04-01 Eliminar pide confirmacion antes de borrar`() {
        repository.items.value = listOf(inventoryItem(id = "carne"))
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.DeleteClicked)

        assertEquals(EditorDialog.ConfirmDelete, viewModel.uiState.value.dialog)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `CA-INV-04-02 confirmar elimina el producto con su foto y vuelve a la lista`() = runTest {
        val photo = ItemPhoto("users/u/inventory/carne/a.jpg", "https://fotos.test/a.jpg")
        repository.items.value = listOf(inventoryItem(id = "carne", photo = photo))
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.DeleteClicked)
        viewModel.onEvent(ProductEditorEvent.DeleteConfirmed)

        assertEquals(EditorDialog.ProductDeleted, viewModel.uiState.value.dialog)
        assertEquals(listOf("carne"), repository.deletedIds)
        assertEquals(listOf(photo.path), photos.deletedPaths)
        advanceResultDialog()
        assertEquals(ProductEditorEffect.NavigateBack, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-04-03 Cancelar cierra la confirmacion sin borrar`() {
        repository.items.value = listOf(inventoryItem(id = "carne"))
        val viewModel = createViewModel(itemId = "carne")
        viewModel.onEvent(ProductEditorEvent.DeleteClicked)

        viewModel.onEvent(ProductEditorEvent.DialogDismissed)

        assertNull(viewModel.uiState.value.dialog)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `CA-INV-05-02 la foto tomada aparece en el formulario y se sube al guardar`() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.PhotoCaptured("/fotos/carne.jpg"))
        assertEquals(PhotoUiState.Local("/fotos/carne.jpg"), viewModel.uiState.value.photo)

        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertEquals("/fotos/carne.jpg", photos.uploads.single().second.filePath)
        assertTrue(repository.savedItems.single().photo != null)
    }

    @Test
    fun `CA-INV-05-04 quitar la foto confirma muestra Imagen eliminada y la borra al guardar`() {
        val photo = ItemPhoto("users/u/inventory/carne/a.jpg", "https://fotos.test/a.jpg")
        repository.items.value = listOf(inventoryItem(id = "carne", expiresOn = today.plusDays(3), photo = photo))
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.RemovePhotoClicked)
        assertEquals(EditorDialog.ConfirmRemovePhoto, viewModel.uiState.value.dialog)
        viewModel.onEvent(ProductEditorEvent.RemovePhotoConfirmed)

        assertEquals(PhotoUiState.None, viewModel.uiState.value.photo)
        assertEquals(EditorDialog.PhotoRemoved, viewModel.uiState.value.dialog)
        advanceResultDialog()
        assertNull(viewModel.uiState.value.dialog)

        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertNull(repository.savedItems.single().photo)
        assertEquals(listOf(photo.path), photos.deletedPaths)
    }

    @Test
    fun `CA-INV-05-05 la foto nueva reemplaza a la anterior`() {
        val original = inventoryItem(photo = ItemPhoto("a.jpg", "https://fotos.test/a.jpg"))
        assertEquals(
            PhotoEdit.Replace(com.smartfood.app.feature.inventory.domain.model.LocalPhoto("/fotos/b.jpg")),
            ProductEditorViewModel.photoEditFor(original, PhotoUiState.Local("/fotos/b.jpg"))
        )
        assertEquals(PhotoEdit.Keep, ProductEditorViewModel.photoEditFor(original, PhotoUiState.Remote("u")))
        assertEquals(PhotoEdit.Remove, ProductEditorViewModel.photoEditFor(original, PhotoUiState.None))
        assertEquals(PhotoEdit.Keep, ProductEditorViewModel.photoEditFor(inventoryItem(), PhotoUiState.None))
    }

    @Test
    fun `CA-INV-05-01 la camara navega al visor`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductEditorEvent.CameraClicked)
        assertEquals(ProductEditorEffect.NavigateToCamera, viewModel.effects.first())
    }

    @Test
    fun `CA-INV-02-10 el precio escrito se guarda con el producto y se carga al editar`() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.PriceChanged("12.500"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(12_500L, repository.savedItems.single().price)
        assertEquals("12500", createViewModel(itemId = repository.savedItems.single().id).uiState.value.priceText)
    }

    @Test
    fun `CA-INV-02-11 un precio invalido marca el campo en rojo y no guarda`() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.PriceChanged("0"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(InventoryField.PRICE, viewModel.uiState.value.errorField)
        assertTrue(repository.savedItems.isEmpty())
    }

    @Test
    fun `CA-INV-02-02 tocar el boton gris muestra el aviso y marca los campos que faltan sin guardar`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductEditorEvent.NameChanged("Carne"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(state.showMissingFields)
        assertEquals(setOf(InventoryField.EXPIRY, InventoryField.CATEGORY), state.missingFields)
        assertTrue(repository.savedItems.isEmpty())
    }

    @Test
    fun `CA-INV-02-02 el aviso desaparece cuando estan los tres campos`() {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertTrue(viewModel.uiState.value.showMissingFields)

        viewModel.fillValidForm()

        assertFalse(viewModel.uiState.value.showMissingFields)
        assertTrue(viewModel.uiState.value.missingFields.isEmpty())
    }

    @Test
    fun `CA-INV-08-05 tras el primer guardado se pide el permiso de notificaciones una sola vez`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(ProductEditorEffect.RequestNotificationPermission, viewModel.effects.first())
        assertTrue(preferences.notificationPermissionRequested)

        advanceResultDialog()

        val segundo = createViewModel()
        segundo.fillValidForm()
        segundo.onEvent(ProductEditorEvent.SaveClicked)
        advanceResultDialog()

        assertEquals(ProductEditorEffect.NavigateBack, segundo.effects.first())
    }

    @Test
    fun `CA-INV-08-05 si el guardado falla no se pide el permiso`() {
        repository.saveResult = Result.failure(InventoryException(InventoryError.Network))
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertFalse(preferences.notificationPermissionRequested)
    }
}
