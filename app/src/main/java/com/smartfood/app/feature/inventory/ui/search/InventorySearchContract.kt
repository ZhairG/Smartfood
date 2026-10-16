/**
 * Nombre: InventorySearchContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de la búsqueda del inventario (spec 006, HU-INV-06): estado de la pantalla, eventos y efectos.
 * Los nombres siguen `plan.md` §2 para que el #43 (voz, HU-INV-07) solo los amplíe, sin renombrar nada.
 */
package com.smartfood.app.feature.inventory.ui.search

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem

/**
 * Un resultado de la búsqueda con el estado de vencimiento del producto, que pinta su estado y su descripción
 * accesible (CA-INV-06-01, RNF-INV-04).
 *
 * @property item Producto encontrado.
 * @property status Estado de vencimiento del producto.
 */
data class SearchResultUi(
    val item: InventoryItem,
    val status: ExpiryStatus,
)

/**
 * Estado de la búsqueda (CA-INV-06-01).
 *
 * @property query Texto de la barra de búsqueda.
 * @property selectedCategory Categoría elegida en los filtros, o `null` para "Todos" (CA-INV-06-03).
 * @property results Productos que cumplen el texto y la categoría a la vez (CA-INV-06-04).
 * @property isVoiceAvailable Si el micrófono está disponible. En el #42 siempre es `false`: el botón queda visible y
 *   desactivado hasta que el #43 (T035) enlaza la voz.
 * @property isLoading `true` hasta que llega el primer resultado del inventario.
 */
data class InventorySearchUiState(
    val query: String = "",
    val selectedCategory: FoodCategory? = null,
    val results: List<SearchResultUi> = emptyList(),
    val isVoiceAvailable: Boolean = false,
    val isLoading: Boolean = false,
) {
    /** No hay nada que mostrar una vez terminada la carga (CA-INV-06-06). */
    val isEmptyResult: Boolean
        get() = !isLoading && results.isEmpty()
}

/**
 * Eventos que la UI envía al ViewModel.
 */
sealed interface InventorySearchEvent {
    /** El usuario escribió en la barra de búsqueda (CA-INV-06-02). */
    data class QueryChanged(val query: String) : InventorySearchEvent

    /** El usuario tocó la "x" de la barra (CA-INV-06-07). */
    data object ClearQuery : InventorySearchEvent

    /** El usuario eligió un filtro de categoría; `null` es "Todos" (CA-INV-06-03). */
    data class CategorySelected(val category: FoodCategory?) : InventorySearchEvent

    /** El usuario tocó el micrófono de "Usa tu voz". La escucha llega con el #43 (T035). */
    data object VoiceClicked : InventorySearchEvent

    /** El usuario tocó un resultado (CA-INV-06-08). */
    data class ResultClicked(val id: String) : InventorySearchEvent

    /** El usuario tocó la flecha atrás. */
    data object BackClicked : InventorySearchEvent
}

/**
 * Efectos de un solo uso de la pantalla.
 */
sealed interface InventorySearchEffect {
    /** Abrir "Editar Producto" del producto tocado (CA-INV-06-08). */
    data class NavigateToEditor(val id: String) : InventorySearchEffect

    /** Volver a la pantalla anterior. */
    data object NavigateBack : InventorySearchEffect
}
