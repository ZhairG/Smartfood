/**
 * Nombre: InventoryContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de la lista de inventario (spec 006, HU-INV-01, plan §2): estado de la pantalla, eventos y
 * efectos. La carga, el error y el vacío forman parte del estado, y las secciones ya vienen agrupadas y ordenadas
 * por el dominio (CA-INV-01-01 y CA-INV-01-04).
 */
package com.smartfood.app.feature.inventory.ui.list

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventorySection

/**
 * Estado de la lista.
 *
 * @property isLoading `true` mientras no llega el primer inventario o al reintentar.
 * @property sections Secciones que se muestran, en el orden del dominio y sin secciones vacías (CA-INV-01-01).
 * @property error Fallo al observar el inventario, o `null` si todo va bien.
 */
data class InventoryUiState(
    val isLoading: Boolean = true,
    val sections: List<InventorySection> = emptyList(),
    val error: InventoryError? = null,
) {
    /** No hay productos que mostrar una vez terminada la carga (CA-INV-01-05). */
    val isEmpty: Boolean
        get() = !isLoading && sections.isEmpty() && error == null
}

/**
 * Eventos que la UI envía al ViewModel.
 */
sealed interface InventoryEvent {
    /** El usuario toca "+" para añadir el primero o uno más (CA-INV-01-05). */
    data object AddClicked : InventoryEvent

    /** El usuario toca la lupa, que abre la búsqueda. */
    data object SearchClicked : InventoryEvent

    /** El usuario toca la fila o su lápiz (CA-INV-01-02). */
    data class EditClicked(val itemId: String) : InventoryEvent

    /** El usuario reintenta después de un error. */
    data object RetryClicked : InventoryEvent
}

/**
 * Efectos de un solo uso de la pantalla.
 */
sealed interface InventoryEffect {
    /** Abrir el editor sin producto para añadir, o con [itemId] para editar uno existente. */
    data class NavigateToEditor(val itemId: String? = null) : InventoryEffect

    /** Abrir la búsqueda del inventario (HU-INV-06). */
    data object NavigateToSearch : InventoryEffect
}
