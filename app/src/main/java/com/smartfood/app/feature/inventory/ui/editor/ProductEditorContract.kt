/**
 * Nombre: ProductEditorContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato del editor de producto (spec 006, HU-INV-02 a HU-INV-05, plan §2): estado de la pantalla,
 * eventos y efectos. Una sola pantalla sirve para añadir y para editar (ADR-INV-01).
 */
package com.smartfood.app.feature.inventory.ui.editor

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import java.time.LocalDate

/** Si el editor añade un producto nuevo o edita uno existente. */
enum class EditorMode { ADD, EDIT }

/** Lo que muestra la zona de foto (CA-INV-05-02). */
sealed interface PhotoUiState {
    /** Sin foto: la zona queda vacía con el botón de cámara. */
    data object None : PhotoUiState

    /** Foto recién tomada, todavía en el teléfono. */
    data class Local(val path: String) : PhotoUiState

    /** Foto ya guardada en la nube. */
    data class Remote(val url: String) : PhotoUiState
}

/** Hoja, calendario o diálogo abierto sobre el editor. */
sealed interface EditorDialog {
    /** Hoja "Agregar foto" con "Galería" y "Cámara" (CA-INV-05-07). */
    data object PhotoSourceSheet : EditorDialog

    /** Hoja "Seleccione una categoría" (CA-INV-02-05). */
    data object CategorySheet : EditorDialog

    /** Hoja de la unidad de la cantidad (CA-INV-02-08). */
    data object UnitSheet : EditorDialog

    /** Calendario de la fecha de vencimiento (CA-INV-02-04). */
    data object DatePicker : EditorDialog

    /** "¿Desea eliminar el producto?" (CA-INV-04-01). */
    data object ConfirmDelete : EditorDialog

    /** "¿Desea eliminar la imagen?" (CA-INV-05-04). */
    data object ConfirmRemovePhoto : EditorDialog

    /** "Imagen eliminada" (CA-INV-05-04). */
    data object PhotoRemoved : EditorDialog
}

/**
 * Estado del editor. Los campos en rojo se **derivan** de [error], como en autenticación.
 *
 * @property mode Añadir o editar.
 * @property isLoading `true` mientras se carga el producto a editar.
 * @property name Nombre tal como se escribió.
 * @property expiresOn Fecha de vencimiento elegida.
 * @property originalExpiresOn Fecha guardada del producto que se edita; se puede conservar aunque ya pasó
 *   (CA-INV-03-03).
 * @property category Categoría elegida.
 * @property quantityText Cantidad tal como se escribió; vacía si no tiene (CA-INV-02-08).
 * @property unit Unidad elegida; sin elegir se usa "unidades".
 * @property priceText Precio tal como se escribió; vacío si no tiene (CA-INV-02-10).
 * @property photo Foto que muestra la zona de imagen.
 * @property isSaveEnabled `true` cuando están el nombre, la fecha y la categoría (CA-INV-02-02).
 * @property isSaving `true` mientras se guarda o se elimina.
 * @property showMissingFields `true` después de tocar el botón gris con campos vacíos (CA-INV-02-02).
 * @property error Último error, de validación o del guardado.
 * @property dialog Hoja, calendario o diálogo abierto.
 */
data class ProductEditorUiState(
    val mode: EditorMode = EditorMode.ADD,
    val isLoading: Boolean = false,
    val name: String = "",
    val expiresOn: LocalDate? = null,
    val originalExpiresOn: LocalDate? = null,
    val category: FoodCategory? = null,
    val quantityText: String = "",
    val unit: QuantityUnit? = null,
    val priceText: String = "",
    val photo: PhotoUiState = PhotoUiState.None,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val showMissingFields: Boolean = false,
    val error: InventoryError? = null,
    val dialog: EditorDialog? = null,
) {
    /** Campo marcado en rojo, si el error es de validación. */
    val errorField: InventoryField?
        get() = (error as? InventoryError.Validation)?.field

    /** Campos obligatorios vacíos, marcados en rojo después de tocar el botón gris (CA-INV-02-02). */
    val missingFields: Set<InventoryField>
        get() = if (!showMissingFields) emptySet() else buildSet {
            if (name.isBlank()) add(InventoryField.NAME)
            if (expiresOn == null) add(InventoryField.EXPIRY)
            if (category == null) add(InventoryField.CATEGORY)
        }

    /** Error que no es de un campo (red, permisos…), para mostrarlo bajo el formulario (CA-INV-02-07). */
    val generalError: InventoryError?
        get() = error.takeIf { it != null && it !is InventoryError.Validation }
}

/** Lo que el usuario hace en el editor. */
sealed interface ProductEditorEvent {
    data class NameChanged(val value: String) : ProductEditorEvent
    data object ExpiryClicked : ProductEditorEvent
    data class ExpiryPicked(val date: LocalDate) : ProductEditorEvent
    data object CategoryClicked : ProductEditorEvent
    data class CategoryPicked(val category: FoodCategory) : ProductEditorEvent
    data class QuantityChanged(val value: String) : ProductEditorEvent
    data object UnitClicked : ProductEditorEvent
    data class UnitPicked(val unit: QuantityUnit) : ProductEditorEvent
    data class PriceChanged(val value: String) : ProductEditorEvent
    data object AddPhotoClicked : ProductEditorEvent
    data object GalleryChosen : ProductEditorEvent
    data object CameraChosen : ProductEditorEvent
    data class PhotoCaptured(val path: String) : ProductEditorEvent
    data object RemovePhotoClicked : ProductEditorEvent
    data object RemovePhotoConfirmed : ProductEditorEvent
    data object SaveClicked : ProductEditorEvent
    data object DeleteClicked : ProductEditorEvent
    data object DeleteConfirmed : ProductEditorEvent
    data object DialogDismissed : ProductEditorEvent
    data object BackClicked : ProductEditorEvent
}

/** Efectos de un solo uso del editor. */
sealed interface ProductEditorEffect {
    /** Abre el visor de la cámara (CA-INV-05-01). */
    data object NavigateToCamera : ProductEditorEffect

    /** Abre el selector de fotos del teléfono (CA-INV-05-07). */
    data object OpenGallery : ProductEditorEffect

    /** Vuelve a la lista. */
    data object NavigateBack : ProductEditorEffect

    /** Vuelve a la lista y le pide mostrar el resultado encima (Figma `476:3851`, CA-INV-02-06, 03-02, 04-02). */
    data class NavigateBackWithResult(val result: EditorResult) : ProductEditorEffect

    /** Borra del teléfono una foto que ya no se va a guardar (descartada o reemplazada). */
    data class DeleteLocalPhoto(val path: String) : ProductEditorEffect

    /**
     * Pide el permiso de notificaciones después del primer guardado (CA-INV-08-05). Está declarado en el contrato,
     * pero lo emite y lo atiende la T039 (#44).
     */
    data object RequestNotificationPermission : ProductEditorEffect
}

/** Resultado del editor que la lista muestra encima al volver (spec 006, T055). */
sealed interface EditorResult {
    /** "Producto Guardado" (CA-INV-02-06). */
    data object Saved : EditorResult

    /** "Guardado: «nombre» se actualizó" (CA-INV-03-02). */
    data class Updated(val name: String) : EditorResult

    /** "Producto eliminado" (CA-INV-04-02). */
    data object Deleted : EditorResult
}
