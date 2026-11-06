package com.smartfood.app.feature.recipes.ui.own

import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory

/** Contrato UDF del editor de recetas propias (spec 010, T013). */
data class OwnRecipeEditorUiState(
    val recipeId: String? = null,
    val draft: OwnRecipeDraft = OwnRecipeDraft(),
    val error: OwnRecipeError? = null,
    val isSaving: Boolean = false,
)

sealed interface OwnRecipeEditorEvent {
    data class NameChanged(val value: String) : OwnRecipeEditorEvent
    data class CategoryPicked(val value: RecipeCategory) : OwnRecipeEditorEvent
    data class CookMinutesChanged(val value: String) : OwnRecipeEditorEvent
    data class PreparationChanged(val value: String) : OwnRecipeEditorEvent
    data object SaveClicked : OwnRecipeEditorEvent
}

sealed interface OwnRecipeEditorEffect {
    data class Saved(val recipeId: String, val name: String, val edited: Boolean) : OwnRecipeEditorEffect
}
