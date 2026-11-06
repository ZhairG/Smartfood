package com.smartfood.app.feature.recipes.ui.own

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory

/**
 * Nombre: OwnRecipeEditorContract.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Estado, eventos y efectos del editor de recetas propias (spec 010, HU-SAV-03 y 04, T013). Los campos en
 * rojo se derivan de [OwnRecipeEditorUiState.error].
 */

/**
 * Hoja para agregar o cambiar un ingrediente (CA-SAV-03-03).
 *
 * @property editingId Ingrediente que se está cambiando, o `null` si es nuevo.
 * @property query Texto de búsqueda.
 * @property selectedId Ingrediente elegido.
 * @property quantity Cantidad escrita.
 * @property isQuantityInvalid `true` si se intentó confirmar sin una cantidad válida.
 */
data class IngredientSheetState(
    val editingId: String? = null,
    val query: String = "",
    val selectedId: String? = null,
    val quantity: String = "",
    val isQuantityInvalid: Boolean = false,
)

/**
 * Estado del editor.
 *
 * @property recipeId Receta que se edita, o `null` si es nueva.
 * @property isLoading `true` mientras se lee la receta.
 * @property draft Lo que escribió la persona.
 * @property error Error del último intento de guardar.
 * @property isSaving `true` mientras se guarda.
 * @property isCategorySheetOpen `true` si está abierta la hoja de categoría.
 * @property ingredientSheet Hoja de ingrediente abierta, o `null`.
 * @property catalog Ingredientes del catálogo, por nombre.
 */
data class OwnRecipeEditorUiState(
    val recipeId: String? = null,
    val isLoading: Boolean = false,
    val draft: OwnRecipeDraft = OwnRecipeDraft(),
    val error: OwnRecipeError? = null,
    val isSaving: Boolean = false,
    val isCategorySheetOpen: Boolean = false,
    val ingredientSheet: IngredientSheetState? = null,
    val catalog: List<Ingredient> = emptyList(),
) {
    /** `true` si la receta todavía no existe. */
    val isNew: Boolean get() = recipeId == null

    /** Campo en rojo (CA-SAV-03-06). */
    val errorField: OwnRecipeField? get() = (error as? OwnRecipeError.Invalid)?.field
}

/** Acciones de la persona en el editor. */
sealed interface OwnRecipeEditorEvent {
    data class NameChanged(val value: String) : OwnRecipeEditorEvent
    data object CategoryClicked : OwnRecipeEditorEvent
    data object CategoryDismissed : OwnRecipeEditorEvent
    data class CategoryPicked(val value: RecipeCategory) : OwnRecipeEditorEvent
    data class CookMinutesChanged(val value: String) : OwnRecipeEditorEvent
    data class PreparationChanged(val value: String) : OwnRecipeEditorEvent
    data object AddIngredientClicked : OwnRecipeEditorEvent
    data class IngredientClicked(val ingredientId: String) : OwnRecipeEditorEvent
    data class IngredientQueryChanged(val value: String) : OwnRecipeEditorEvent
    data class IngredientPicked(val ingredientId: String) : OwnRecipeEditorEvent
    data class IngredientQuantityChanged(val value: String) : OwnRecipeEditorEvent
    data object IngredientConfirmed : OwnRecipeEditorEvent
    data object IngredientRemoved : OwnRecipeEditorEvent
    data object IngredientSheetDismissed : OwnRecipeEditorEvent
    data object SaveClicked : OwnRecipeEditorEvent
    data object DiscardClicked : OwnRecipeEditorEvent
    data object DeleteClicked : OwnRecipeEditorEvent
}

/** Efectos de navegación del editor. */
sealed interface OwnRecipeEditorEffect {

    /** Se guardó: abre "¡Receta guardada!". */
    data class Saved(val recipeId: String, val name: String, val edited: Boolean) : OwnRecipeEditorEffect

    /** Vuelve a Guardadas sin guardar (CA-SAV-04-04). */
    data object Close : OwnRecipeEditorEffect

    /** Abre "Eliminar receta" (CA-SAV-05-01). */
    data class OpenDelete(val recipeId: String, val name: String) : OwnRecipeEditorEffect
}
