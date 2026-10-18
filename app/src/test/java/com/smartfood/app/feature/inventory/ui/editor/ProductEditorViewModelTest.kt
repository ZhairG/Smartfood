/**
 * Nombre: ProductEditorViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ProductEditorViewModel] (spec 006, T032): alta, edición, eliminación, foto,
 * errores y navegación, con los casos de uso reales sobre los fakes de `testutil/`. Las variantes de un mismo
 * criterio van en una sola prueba (#105).
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
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.usecase.AddItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.GetItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.PhotoEdit
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import com.smartfood.app.testutil.FakeExpiryReminderScheduler
import com.smartfood.app.testutil.FakeInventoryPreferences
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.CompletableDeferred
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
    private val photoSync = FakePhotoSync()
    private val scheduler = FakeExpiryReminderScheduler()
    private val preferences = FakeInventoryPreferences()
    private val validate = ValidateItemUseCase(fixedClock(today))

    private fun createViewModel(itemId: String? = null) = ProductEditorViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ProductEditorViewModel.ITEM_ID_KEY to itemId)),
        getItem = GetItemUseCase(repository, photoSync),
        validate = validate,
        addItem = AddItemUseCase(repository, photoSync, validate, scheduler),
        updateItem = UpdateItemUseCase(repository, photoSync, validate),
        deleteItem = DeleteItemUseCase(repository, photoSync),
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
    fun `CA-INV-02-02 CA-INV-02-08 el boton se habilita con los tres campos y el boton gris avisa lo que falta`() {
        val viewModel = createViewModel()
        assertFalse("vacío", viewModel.uiState.value.isSaveEnabled)
        // CA-INV-02-08: la cantidad no cambia el estado del botón.
        viewModel.onEvent(ProductEditorEvent.QuantityChanged("1,5"))
        viewModel.onEvent(ProductEditorEvent.NameChanged("Carne"))
        assertFalse("solo nombre y cantidad", viewModel.uiState.value.isSaveEnabled)

        // Tocar el botón gris muestra el aviso y marca los campos que faltan, sin guardar.
        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertTrue(viewModel.uiState.value.showMissingFields)
        assertEquals(setOf(InventoryField.EXPIRY, InventoryField.CATEGORY), viewModel.uiState.value.missingFields)
        assertTrue(repository.savedItems.isEmpty())

        // Con los tres campos se habilita y el aviso desaparece, aunque se borre la cantidad.
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged(""))
        assertTrue("los tres campos", viewModel.uiState.value.isSaveEnabled)
        assertFalse(viewModel.uiState.value.showMissingFields)
        assertTrue(viewModel.uiState.value.missingFields.isEmpty())
    }

    @Test
    fun `CA-INV-02-06 CA-INV-02-12 CA-INV-08-05 guardar un alta muestra Guardando y vuelve a la lista con Producto Guardado`() = runTest {
        val gate = CompletableDeferred<Unit>().also { repository.gate = it }
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductEditorEvent.QuantityChanged("1,5"))
        viewModel.onEvent(ProductEditorEvent.UnitPicked(QuantityUnit.KILOGRAMS))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertEquals(SavingAction.ADD, viewModel.uiState.value.savingAction)
        assertTrue(viewModel.uiState.value.isSaving)
        // CA-INV-02-12: atrás no hace nada mientras se guarda.
        viewModel.onEvent(ProductEditorEvent.BackClicked)

        gate.complete(Unit)
        // Con éxito el diálogo no se cierra: el editor se va con el resultado y no se ve el formulario un instante.
        assertEquals(SavingAction.ADD, viewModel.uiState.value.savingAction)
        val saved = repository.savedItems.single()
        assertEquals("Carne", saved.name)
        assertEquals(Quantity(1.5, QuantityUnit.KILOGRAMS), saved.quantity)
        assertEquals(1, scheduler.scheduleCalls)
        // CA-INV-08-05: tras el primer guardado se pide el permiso de notificaciones; "atrás" no navegó.
        assertEquals(
            listOf(
                ProductEditorEffect.RequestNotificationPermission,
                ProductEditorEffect.NavigateBackWithResult(EditorResult.Saved)
            ),
            viewModel.effects.take(2).toList()
        )
        assertTrue(preferences.notificationPermissionRequested)

        // El permiso se pide una sola vez.
        val segundo = createViewModel()
        segundo.fillValidForm()
        segundo.onEvent(ProductEditorEvent.SaveClicked)
        advanceResultDialog()
        assertEquals(ProductEditorEffect.NavigateBackWithResult(EditorResult.Saved), segundo.effects.first())
    }

    @Test
    fun `CA-INV-02-07 CA-INV-02-12 CA-INV-08-05 si el guardado falla se quita el indicador, queda el error y no se pide el permiso`() {
        val gate = CompletableDeferred<Unit>().also { repository.gate = it }
        repository.saveResult = Result.failure(InventoryException(InventoryError.Network))
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertEquals(SavingAction.ADD, viewModel.uiState.value.savingAction)
        gate.complete(Unit)

        val state = viewModel.uiState.value
        assertNull(state.savingAction)
        assertFalse(state.isSaving)
        assertNull(state.dialog)
        assertEquals(InventoryError.Network, state.generalError)
        assertEquals("Carne", state.name)
        assertFalse(preferences.notificationPermissionRequested)
    }

    @Test
    fun `CA-INV-02-12 la edicion muestra Actualizando y la eliminacion Eliminando hasta volver`() {
        repository.items.value = listOf(inventoryItem(id = "carne", name = "Carne", expiresOn = today.plusDays(3)))

        val edicion = createViewModel(itemId = "carne")
        var gate = CompletableDeferred<Unit>().also { repository.gate = it }
        edicion.onEvent(ProductEditorEvent.SaveClicked)
        assertEquals(SavingAction.UPDATE, edicion.uiState.value.savingAction)
        gate.complete(Unit)
        assertEquals(SavingAction.UPDATE, edicion.uiState.value.savingAction)

        val eliminacion = createViewModel(itemId = "carne")
        gate = CompletableDeferred<Unit>().also { repository.gate = it }
        eliminacion.onEvent(ProductEditorEvent.DeleteClicked)
        eliminacion.onEvent(ProductEditorEvent.DeleteConfirmed)
        assertEquals(SavingAction.DELETE, eliminacion.uiState.value.savingAction)
        gate.complete(Unit)
        assertEquals(SavingAction.DELETE, eliminacion.uiState.value.savingAction)
        assertEquals(listOf("carne"), repository.deletedIds)
    }

    @Test
    fun `CA-INV-02-09 CA-INV-02-11 una cantidad o un precio invalidos marcan su campo y no guardan`() {
        listOf<Pair<ProductEditorEvent, InventoryField>>(
            ProductEditorEvent.QuantityChanged("0") to InventoryField.QUANTITY,
            ProductEditorEvent.PriceChanged("0") to InventoryField.PRICE
        ).forEach { (cambio, campo) ->
            val viewModel = createViewModel()
            viewModel.fillValidForm()
            viewModel.onEvent(cambio)

            viewModel.onEvent(ProductEditorEvent.SaveClicked)

            assertEquals("$cambio", campo, viewModel.uiState.value.errorField)
            assertTrue("$cambio", repository.savedItems.isEmpty())
        }
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
    fun `CA-INV-03-02 guardar una edicion vuelve a la lista con Guardado y el nombre`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "carne", name = "Carne", expiresOn = today.plusDays(3)))
        val viewModel = createViewModel(itemId = "carne")
        viewModel.onEvent(ProductEditorEvent.NameChanged("Carne molida"))

        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(
            ProductEditorEffect.NavigateBackWithResult(EditorResult.Updated("Carne molida")),
            viewModel.effects.take(2).toList().last()
        )
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
    fun `CA-INV-04-01 CA-INV-04-03 Eliminar pide confirmacion y Cancelar la cierra sin borrar`() {
        repository.items.value = listOf(inventoryItem(id = "carne"))
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.DeleteClicked)
        assertEquals(EditorDialog.ConfirmDelete, viewModel.uiState.value.dialog)

        viewModel.onEvent(ProductEditorEvent.DialogDismissed)
        assertNull(viewModel.uiState.value.dialog)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `CA-INV-04-02 confirmar elimina el producto con su foto y vuelve a la lista`() = runTest {
        val photo = ItemPhoto("users/u/inventory/carne/a.jpg", "https://fotos.test/a.jpg")
        repository.items.value = listOf(inventoryItem(id = "carne", photo = photo))
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.DeleteClicked)
        viewModel.onEvent(ProductEditorEvent.DeleteConfirmed)

        assertEquals(listOf("carne"), repository.deletedIds)
        assertEquals(listOf(photo.path), photoSync.deletes)
        assertEquals(ProductEditorEffect.NavigateBackWithResult(EditorResult.Deleted), viewModel.effects.first())
    }

    @Test
    fun `CA-INV-05-02 la foto tomada aparece en el formulario y se encola al guardar`() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductEditorEvent.PhotoCaptured("/fotos/carne.jpg"))
        assertEquals(PhotoUiState.Local("/fotos/carne.jpg"), viewModel.uiState.value.photo)

        viewModel.onEvent(ProductEditorEvent.SaveClicked)
        assertEquals("/fotos/carne.jpg", photoSync.uploads.single().second.filePath)
        assertEquals(repository.savedItems.single().id, photoSync.uploads.single().first)
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
        assertEquals(listOf(photo.path), photoSync.deletes)
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
    fun `CA-INV-05-01 CA-INV-05-07 tocar la foto abre la hoja y cada opcion la cierra y abre la galeria o el visor`() = runTest {
        listOf(
            ProductEditorEvent.GalleryChosen to ProductEditorEffect.OpenGallery,
            ProductEditorEvent.CameraChosen to ProductEditorEffect.NavigateToCamera
        ).forEach { (opcion, efecto) ->
            val viewModel = createViewModel()
            viewModel.onEvent(ProductEditorEvent.AddPhotoClicked)
            assertEquals(EditorDialog.PhotoSourceSheet, viewModel.uiState.value.dialog)

            viewModel.onEvent(opcion)

            assertNull("$opcion", viewModel.uiState.value.dialog)
            assertEquals("$opcion", efecto, viewModel.effects.first())
        }
    }

    @Test
    fun `CA-INV-10-02 el resultado del editor se guarda y se lee como texto para la lista`() {
        listOf(EditorResult.Saved, EditorResult.Updated("Carne molida"), EditorResult.Deleted, EditorResult.SavedOnPhone)
            .forEach { result -> assertEquals(result, decodeEditorResult(result.encode())) }
        assertNull(decodeEditorResult(null))
    }

    // T050: sin conexión y fotos que esperan subirse (CA-INV-10-02 y 10-04).

    @Test
    fun `CA-INV-10-02 sin conexion el alta y la edicion vuelven a la lista con Guardado en tu telefono`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "carne", name = "Carne", expiresOn = today.plusDays(3)))
        repository.outcome = WriteOutcome.PENDING
        val alta = createViewModel().apply { fillValidForm() }
        val edicion = createViewModel(itemId = "carne")

        listOf(alta, edicion).forEach { viewModel ->
            viewModel.onEvent(ProductEditorEvent.SaveClicked)
            // El permiso de notificaciones solo se pide en el primer guardado; se busca el efecto de navegación.
            assertEquals(
                ProductEditorEffect.NavigateBackWithResult(EditorResult.SavedOnPhone),
                viewModel.effects.first { it is ProductEditorEffect.NavigateBackWithResult }
            )
        }
    }

    @Test
    fun `CA-INV-10-04 la foto pendiente se ve desde el telefono, se conserva al guardar y atras no la borra`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "carne", expiresOn = today.plusDays(3)))
        photoSync.pending.value = mapOf("carne" to "/files/pending/carne/a.jpg")

        val atras = createViewModel(itemId = "carne")
        assertEquals(PhotoUiState.Pending("/files/pending/carne/a.jpg"), atras.uiState.value.photo)
        atras.onEvent(ProductEditorEvent.BackClicked)
        // Solo navega: la subida pendiente es la dueña del archivo.
        assertEquals(ProductEditorEffect.NavigateBack, atras.effects.first())

        val guardar = createViewModel(itemId = "carne")
        guardar.onEvent(ProductEditorEvent.NameChanged("Carne molida"))
        guardar.onEvent(ProductEditorEvent.SaveClicked)
        assertTrue(photoSync.uploads.isEmpty())
        assertTrue(photoSync.cancelled.isEmpty())
        assertEquals(listOf(false), repository.photoChangedFlags)
    }

    @Test
    fun `CA-INV-10-04 quitar la foto pendiente cancela su subida al guardar`() {
        repository.items.value = listOf(inventoryItem(id = "carne", expiresOn = today.plusDays(3)))
        photoSync.pending.value = mapOf("carne" to "/files/pending/carne/a.jpg")
        val viewModel = createViewModel(itemId = "carne")

        viewModel.onEvent(ProductEditorEvent.RemovePhotoClicked)
        viewModel.onEvent(ProductEditorEvent.RemovePhotoConfirmed)
        advanceResultDialog()
        viewModel.onEvent(ProductEditorEvent.SaveClicked)

        assertEquals(listOf("carne"), photoSync.cancelled)
    }
}

