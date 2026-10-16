/**
 * Nombre: InventorySearchContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de la búsqueda del inventario (spec 006, HU-INV-06 y HU-INV-07): estado de la pantalla,
 * eventos y efectos. Los nombres siguen `plan.md` §2.
 */
package com.smartfood.app.feature.inventory.ui.search

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.SpeechError

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
 * Estado del aviso de voz, dibujado encima de la pantalla (HU-INV-07).
 */
sealed interface VoiceUiState {
    /** No hay aviso de voz en pantalla. */
    data object Hidden : VoiceUiState

    /** El reconocedor escucha: el aviso muestra "Habla ahora" con las ondas (CA-INV-07-01). */
    data object Listening : VoiceUiState

    /** Se procesa el audio: el aviso muestra "…" (CA-INV-07-02). */
    data object Processing : VoiceUiState

    /** Se entendió el dictado: el aviso muestra un momento lo que entendió ("Carne Leche Tomate", CA-INV-07-03). */
    data class Recognized(val text: String) : VoiceUiState

    /** La escucha falló: el aviso muestra el error y, si no se entendió, "Reintentar" (CA-INV-07-05). */
    data class Failed(val error: SpeechError) : VoiceUiState
}

/**
 * Estado de la búsqueda (CA-INV-06-01).
 *
 * @property query Texto de la barra de búsqueda.
 * @property selectedCategory Categoría elegida en los filtros, o `null` para "Todos" (CA-INV-06-03).
 * @property results Productos que cumplen el texto y la categoría a la vez (CA-INV-06-04).
 * @property isVoiceAvailable Si el teléfono tiene servicio de reconocimiento de voz; si no, la tarjeta "Usa tu voz"
 *   no se muestra (CA-INV-07-08).
 * @property voice Estado del aviso de voz (CA-INV-07-01, CA-INV-07-02, CA-INV-07-05).
 * @property isLoading `true` hasta que llega el primer resultado del inventario.
 */
data class InventorySearchUiState(
    val query: String = "",
    val selectedCategory: FoodCategory? = null,
    val results: List<SearchResultUi> = emptyList(),
    val isVoiceAvailable: Boolean = false,
    val voice: VoiceUiState = VoiceUiState.Hidden,
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

    /** El usuario tocó el micrófono de "Usa tu voz" (CA-INV-07-01). */
    data object VoiceClicked : InventorySearchEvent

    /** Resultado de pedir el permiso de micrófono (CA-INV-07-06). */
    data class VoicePermissionResult(val granted: Boolean) : InventorySearchEvent

    /** El usuario tocó "Reintentar" en el aviso de error: vuelve a escuchar (CA-INV-07-05). */
    data object VoiceRetry : InventorySearchEvent

    /** El usuario tocó fuera del aviso o volvió atrás: cancela la escucha (CA-INV-07-07). */
    data object VoiceDismissed : InventorySearchEvent

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

    /** Pedir el permiso de micrófono; la pantalla decide si ya está concedido (CA-INV-07-06). */
    data object RequestMicPermission : InventorySearchEffect

    /** Mostrar el aviso de que el permiso de micrófono fue denegado, con acceso a Ajustes (CA-INV-07-06). */
    data object ShowPermissionRationale : InventorySearchEffect

    /** Volver a la pantalla anterior. */
    data object NavigateBack : InventorySearchEffect
}
