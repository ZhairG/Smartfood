package com.smartfood.app.feature.recipes.ui.saved

import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard
import com.smartfood.app.feature.recipes.domain.model.SavedSort

/**
 * Nombre: SavedRecipesContract.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Estado, eventos y efectos de la pantalla Guardadas (spec 010, HU-SAV-02, T007).
 * Los campos derivados (tarjetas, filtro activo, vacío, aviso "Sin conexión") los calcula
 * el ViewModel; la pantalla solo dibuja y avisa.
 */

/**
 * Estado de la pantalla Guardadas.
 *
 * @property filter Chip elegido.
 * @property query Texto de búsqueda.
 * @property sort Orden elegido.
 * @property cards Tarjetas visibles, ya filtradas, buscadas y ordenadas.
 * @property isEmptyOverall `true` si no hay recetas guardadas válidas.
 * @property hasPendingSync `true` si alguna favorita visible espera sincronizarse (CA-SAV-02-09).
 * @property isSortMenuOpen `true` si el menú "Ordenar" está abierto.
 * @property deleted Receta borrada pendiente de deshacer, o `null`.
 * @property undoSecondsLeft Segundos restantes de la cuenta regresiva de deshacer.
 */
data class SavedRecipesUiState(
    val filter: SavedFilter = SavedFilter.FAVORITES,
    val query: String = "",
    val sort: SavedSort = SavedSort.RECENT,
    val cards: List<SavedRecipeCard> = emptyList(),
    val isEmptyOverall: Boolean = true,
    val hasPendingSync: Boolean = false,
    val isSortMenuOpen: Boolean = false,
    val deleted: DeletedOwnRecipe? = null,
    val undoSecondsLeft: Int = 0,
)

/** Acciones de la persona en Guardadas. */
sealed interface SavedRecipesEvent {

    /** Cambió el chip elegido. */
    data class FilterChanged(val filter: SavedFilter) : SavedRecipesEvent

    /** Escribió en la búsqueda. */
    data class QueryChanged(val query: String) : SavedRecipesEvent

    /** Cambió el orden elegido. */
    data class SortChanged(val sort: SavedSort) : SavedRecipesEvent

    /** Marcó o quitó una receta como favorita. */
    data class FavoriteToggled(val recipeId: String) : SavedRecipesEvent

    /** Tocó una tarjeta para abrir el detalle o el editor. */
    data class CardClicked(val recipeId: String) : SavedRecipesEvent

    /** Tocó "+" o "Crear receta". */
    object AddClicked : SavedRecipesEvent

    /** Tocó "Deshacer" en el aviso de eliminación. */
    object UndoClicked : SavedRecipesEvent
}
