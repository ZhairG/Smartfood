/**
 * Nombre: ProductEditorViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel del editor de producto (spec 006, HU-INV-02 a HU-INV-05, plan §2). Las reglas viven en
 * los casos de uso: el ViewModel solo arma el borrador, decide qué pasa con la foto y muestra el resultado. Los
 * efectos de un solo uso salen por un `Channel` que la ruta recolecta con `ObserveEffects`.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.inventory.domain.repository.InventoryPreferences
import com.smartfood.app.feature.inventory.domain.usecase.AddItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.GetItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.PhotoEdit
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * @param savedStateHandle Argumentos de `Route.ProductEditor`: `itemId` es `null` al añadir.
 * @param repository Repositorio del que se carga el producto a editar.
 * @param validate Reglas del formulario; aquí solo se usa `isComplete` para habilitar el botón.
 * @param addItem Alta de un producto (CA-INV-02-06).
 * @param updateItem Edición de un producto (CA-INV-03-02).
 * @param deleteItem Eliminación de un producto (CA-INV-04-02).
 * @param preferences Preferencias locales: recuerdan si ya se pidió el permiso de notificaciones (CA-INV-08-05).
 */
@HiltViewModel
class ProductEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItem: GetItemUseCase,
    private val validate: ValidateItemUseCase,
    private val addItem: AddItemUseCase,
    private val updateItem: UpdateItemUseCase,
    private val deleteItem: DeleteItemUseCase,
    private val preferences: InventoryPreferences,
) : ViewModel() {

    private val itemId: String? = savedStateHandle.get<String>(ITEM_ID_KEY)

    private val _uiState = MutableStateFlow(
        ProductEditorUiState(mode = if (itemId == null) EditorMode.ADD else EditorMode.EDIT)
    )
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProductEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Producto tal como está guardado; `null` al añadir. */
    private var original: InventoryItem? = null

    init {
        itemId?.let(::loadItem)
    }

    /**
     * Atiende lo que el usuario hace en el editor.
     *
     * @param event Evento de la UI.
     */
    fun onEvent(event: ProductEditorEvent) {
        when (event) {
            is ProductEditorEvent.NameChanged -> edit { it.copy(name = event.value) }
            ProductEditorEvent.ExpiryClicked -> openDialog(EditorDialog.DatePicker)
            is ProductEditorEvent.ExpiryPicked -> edit { it.copy(expiresOn = event.date, dialog = null) }
            ProductEditorEvent.CategoryClicked -> openDialog(EditorDialog.CategorySheet)
            is ProductEditorEvent.CategoryPicked -> edit { it.copy(category = event.category, dialog = null) }
            is ProductEditorEvent.QuantityChanged -> edit { it.copy(quantityText = event.value) }
            ProductEditorEvent.UnitClicked -> openDialog(EditorDialog.UnitSheet)
            is ProductEditorEvent.UnitPicked -> edit { it.copy(unit = event.unit, dialog = null) }
            is ProductEditorEvent.PriceChanged -> edit { it.copy(priceText = event.value) }
            ProductEditorEvent.AddPhotoClicked -> openDialog(EditorDialog.PhotoSourceSheet)
            ProductEditorEvent.GalleryChosen -> {
                _uiState.update { it.copy(dialog = null) }
                sendEffect(ProductEditorEffect.OpenGallery)
            }
            ProductEditorEvent.CameraChosen -> {
                _uiState.update { it.copy(dialog = null) }
                sendEffect(ProductEditorEffect.NavigateToCamera)
            }
            is ProductEditorEvent.PhotoCaptured -> onPhotoCaptured(event.path)
            ProductEditorEvent.RemovePhotoClicked -> openDialog(EditorDialog.ConfirmRemovePhoto)
            ProductEditorEvent.RemovePhotoConfirmed -> removePhoto()
            ProductEditorEvent.SaveClicked -> save()
            ProductEditorEvent.DeleteClicked -> openDialog(EditorDialog.ConfirmDelete)
            ProductEditorEvent.DeleteConfirmed -> delete()
            ProductEditorEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            ProductEditorEvent.BackClicked -> discardAndLeave()
        }
    }

    /** Carga el producto a editar con sus datos (CA-INV-03-01). */
    private fun loadItem(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getItem(id)
                .onSuccess { item ->
                    original = item
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = item.name,
                            expiresOn = item.expiresOn,
                            originalExpiresOn = item.expiresOn,
                            category = item.category,
                            quantityText = item.quantity?.let { quantity -> formatAmount(quantity.amount) }.orEmpty(),
                            unit = item.quantity?.unit,
                            priceText = item.price?.toString().orEmpty(),
                            photo = item.toPhotoUiState(),
                        ).withSaveEnabled()
                    }
                }
                .onFailure { cause -> _uiState.update { it.copy(isLoading = false, error = cause.toInventoryError()) } }
        }
    }

    /** Aplica un cambio del formulario: borra el error anterior y recalcula el botón (CA-INV-02-02). */
    private fun edit(change: (ProductEditorUiState) -> ProductEditorUiState) {
        _uiState.update { change(it).copy(error = null).withSaveEnabled() }
    }

    private fun openDialog(dialog: EditorDialog) {
        _uiState.update { it.copy(dialog = dialog) }
    }

    /** La foto nueva reemplaza a la anterior; si la anterior tampoco se guardó, se borra del teléfono. */
    private fun onPhotoCaptured(path: String) {
        (_uiState.value.photo as? PhotoUiState.Local)?.let { sendEffect(ProductEditorEffect.DeleteLocalPhoto(it.path)) }
        _uiState.update { it.copy(photo = PhotoUiState.Local(path)) }
    }

    /** Quita la foto de la zona; en la edición, la de la nube se borra al guardar (CA-INV-05-04). */
    private fun removePhoto() {
        (_uiState.value.photo as? PhotoUiState.Local)?.let { sendEffect(ProductEditorEffect.DeleteLocalPhoto(it.path)) }
        _uiState.update { it.copy(photo = PhotoUiState.None, dialog = EditorDialog.PhotoRemoved) }
        viewModelScope.launch {
            delay(RESULT_DIALOG_MILLIS)
            _uiState.update { if (it.dialog == EditorDialog.PhotoRemoved) it.copy(dialog = null) else it }
        }
    }

    /** Guarda el alta o la edición. Si falla, el formulario conserva lo escrito (CA-INV-02-07, CA-INV-05-06). */
    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        // El botón gris no guarda: muestra el aviso y marca los campos que faltan (CA-INV-02-02).
        if (!state.isSaveEnabled) {
            _uiState.update { it.copy(showMissingFields = true, error = null) }
            return
        }
        val draft = state.toDraft()
        val localPhoto = (state.photo as? PhotoUiState.Local)?.let { LocalPhoto(it.path) }
        viewModelScope.launch {
            val current = original
            val action = if (current == null) SavingAction.ADD else SavingAction.UPDATE
            _uiState.update { it.copy(savingAction = action, error = null) }
            val result = if (current == null) {
                addItem(draft, localPhoto)
            } else {
                updateItem(current, draft, photoEditFor(current, state.photo))
            }
            result
                .onSuccess { savedItem ->
                    // El diálogo de carga sigue hasta que el editor se va, para no dejar ver el formulario un instante
                    // antes de volver a la lista (CA-INV-02-12).
                    requestNotificationPermissionOnce()
                    // El resultado se ve encima de la lista, como el Figma (476:3851). Sin conexión, el cambio quedó
                    // en el teléfono y se subirá solo (CA-INV-10-02, T050).
                    val saved = when {
                        savedItem.outcome == WriteOutcome.PENDING -> EditorResult.SavedOnPhone
                        current == null -> EditorResult.Saved
                        else -> EditorResult.Updated(savedItem.item.name)
                    }
                    _effects.send(ProductEditorEffect.NavigateBackWithResult(saved))
                }
                .onFailure { cause -> _uiState.update { it.copy(savingAction = null, error = cause.toInventoryError()) } }
        }
    }

    /** Elimina el producto con su foto (CA-INV-04-02). */
    private fun delete() {
        val current = original ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(savingAction = SavingAction.DELETE, dialog = null, error = null) }
            deleteItem(current)
                .onSuccess {
                    (_uiState.value.photo as? PhotoUiState.Local)?.let {
                        sendEffect(ProductEditorEffect.DeleteLocalPhoto(it.path))
                    }
                    _effects.send(ProductEditorEffect.NavigateBackWithResult(EditorResult.Deleted))
                }
                .onFailure { cause -> _uiState.update { it.copy(savingAction = null, error = cause.toInventoryError()) } }
        }
    }

    /**
     * Pide el permiso de notificaciones una sola vez, después del primer guardado exitoso (CA-INV-08-05). La marca
     * se escribe antes de emitir el efecto: si el usuario lo niega, el inventario funciona igual y no se le vuelve a
     * pedir automáticamente (RNF-INV-07).
     */
    private suspend fun requestNotificationPermissionOnce() {
        if (preferences.wasNotificationPermissionRequested()) return
        preferences.markNotificationPermissionRequested()
        _effects.send(ProductEditorEffect.RequestNotificationPermission)
    }

    /** Vuelve atrás sin guardar: el producto queda como estaba (CA-INV-03-04) y la foto nueva se descarta. */
    private fun discardAndLeave() {
        if (_uiState.value.isSaving) return
        (_uiState.value.photo as? PhotoUiState.Local)?.let { sendEffect(ProductEditorEffect.DeleteLocalPhoto(it.path)) }
        sendEffect(ProductEditorEffect.NavigateBack)
    }

    private fun ProductEditorUiState.withSaveEnabled(): ProductEditorUiState {
        val complete = validate.isComplete(toDraft())
        // El aviso de campos vacíos desaparece cuando ya están los tres (CA-INV-02-02).
        return copy(isSaveEnabled = complete, showMissingFields = showMissingFields && !complete)
    }

    private fun ProductEditorUiState.toDraft() = ItemDraft(
        name = name,
        expiresOn = expiresOn,
        category = category,
        quantityText = quantityText,
        unit = unit,
        priceText = priceText,
    )

    private fun sendEffect(effect: ProductEditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        /** Nombre del argumento de `Route.ProductEditor` en el `SavedStateHandle`. */
        const val ITEM_ID_KEY = "itemId"

        /** Tiempo que se ve un diálogo de resultado antes de cerrarse solo. */
        const val RESULT_DIALOG_MILLIS = 2_000L

        /** Qué pasa con la foto al guardar una edición (CA-INV-05-04, CA-INV-05-05). */
        internal fun photoEditFor(original: InventoryItem, photo: PhotoUiState): PhotoEdit = when (photo) {
            is PhotoUiState.Local -> PhotoEdit.Replace(LocalPhoto(photo.path))
            // Quitar una foto que espera subirse también es quitarla: así se cancela su subida.
            PhotoUiState.None ->
                if (original.photo != null || original.localPhotoPath != null) PhotoEdit.Remove else PhotoEdit.Keep
            // La foto pendiente ya está en cola: se conserva (T050).
            is PhotoUiState.Remote, is PhotoUiState.Pending -> PhotoEdit.Keep
        }

        /** Foto que muestra el editor: la del teléfono que espera subirse, la de la nube o ninguna (CA-INV-10-04). */
        internal fun InventoryItem.toPhotoUiState(): PhotoUiState =
            localPhotoPath?.let { PhotoUiState.Pending(it) }
                ?: photo?.let { PhotoUiState.Remote(it.url) }
                ?: PhotoUiState.None

        /** Cantidad guardada como texto, sin ceros decimales sobrantes ("1.5", "2"). */
        internal fun formatAmount(amount: Double): String =
            BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()
    }
}

/** Categorías en el orden del Figma (`476:4113`). */
internal val CategoryOptions: List<FoodCategory> = FoodCategory.entries

/** Unidades en el orden de la spec (CA-INV-02-08). */
internal val UnitOptions: List<QuantityUnit> = QuantityUnit.entries
