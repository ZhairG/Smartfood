package com.smartfood.app.feature.recipes.ui.saved

import com.smartfood.app.feature.recipes.domain.model.DeletedOwnRecipe
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard
import com.smartfood.app.feature.recipes.domain.model.SavedSort

/**
 * Nombre: SavedRecipesContract.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Estado, eventos y efectos de la pantalla Guardadas (spec 010, HU-SAV-02, T007) y su aviso de deshacer
 * (HU-SAV-05, T019).
 */

/**
 * Estado de la pantalla Guardadas.
 *
 * @property isLoading `true` hasta que llegan los primeros datos; evita que parpadee el estado vacío.
 * @property hasError `true` si las recetas guardadas no se pudieron leer (CA-SAV-02-11).
 * @property filter Chip elegido.
 * @property query Texto de búsqueda.
 * @property sort Orden elegido.
 * @property cards Tarjetas visibles, ya filtradas, buscadas y ordenadas.
 * @property isEmptyOverall `true` si no hay ninguna receta guardada (CA-SAV-02-07).
 * @property hasPendingSync `true` si hay cambios que esperan sincronizarse (CA-SAV-02-09).
 * @property isSortMenuOpen `true` si el menú "Ordenar" está abierto.
 * @property deleted Receta recién eliminada que aún se puede recuperar (CA-SAV-05-03).
 * @property undoSecondsLeft Segundos que quedan para deshacer.
 */
data class SavedRecipesUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val filter: SavedFilter = SavedFilter.FAVORITES,
    val query: String = "",
    val sort: SavedSort = SavedSort.RECENT,
    val cards: List<SavedRecipeCard> = emptyList(),
    val isEmptyOverall: Boolean = false,
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

    /** Tocó la "x" de la búsqueda. */
    data object ClearQuery : SavedRecipesEvent

    /** Tocó "Ordenar". */
    data object SortClicked : SavedRecipesEvent

    /** Cerró el menú "Ordenar" sin confirmar. */
    data object SortDismissed : SavedRecipesEvent

    /** Confirmó un orden en el menú. */
    data class SortChanged(val sort: SavedSort) : SavedRecipesEvent

    /** Marcó o quitó una receta como favorita. */
    data class FavoriteToggled(val recipeId: String) : SavedRecipesEvent

    /** Tocó una tarjeta. */
    data class CardClicked(val recipeId: String) : SavedRecipesEvent

    /** Tocó "+" o "Crear receta". */
    data object AddClicked : SavedRecipesEvent

    /** Tocó "DESHACER" en el aviso (CA-SAV-05-04). */
    data object UndoClicked : SavedRecipesEvent
}

/** Efectos de navegación de Guardadas. */
sealed interface SavedRecipesEffect {

    /** Abre el detalle de una receta del catálogo. */
    data class OpenRecipe(val recipeId: String) : SavedRecipesEffect

    /** Abre "Editar receta" de una receta propia (CA-SAV-02-06). */
    data class OpenOwnRecipe(val recipeId: String) : SavedRecipesEffect

    /** Abre "Nueva receta" (CA-SAV-02-10). */
    data object OpenNewRecipe : SavedRecipesEffect
}
