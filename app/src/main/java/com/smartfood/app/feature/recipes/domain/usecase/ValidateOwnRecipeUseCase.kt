package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import javax.inject.Inject

/**
 * Nombre: ValidateOwnRecipeUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Valida los campos de una receta propia en el orden del formulario (spec 010, T011).
 */
class ValidateOwnRecipeUseCase @Inject constructor() {
    operator fun invoke(draft: OwnRecipeDraft): Result<Unit> = runCatching {
        if (draft.name.trim().length !in MIN_NAME..MAX_NAME) invalid(OwnRecipeField.NAME)
        if (draft.category == null) invalid(OwnRecipeField.CATEGORY)
        if (draft.ingredients.size > MAX_INGREDIENTS || draft.ingredients.hasDuplicates() || draft.ingredients.any { it.quantity <= 0 }) {
            invalid(OwnRecipeField.INGREDIENTS)
        }
        val minutes = draft.cookMinutes.toIntOrNull()
        if (minutes == null || minutes !in MIN_MINUTES..MAX_MINUTES) invalid(OwnRecipeField.COOK_MINUTES)
        if (draft.preparation.isBlank() || draft.preparation.length > MAX_PREPARATION) invalid(OwnRecipeField.PREPARATION)
    }.recoverCatching { throwable -> throw throwable as? OwnRecipeException ?: throwable }

    private fun invalid(field: OwnRecipeField): Nothing = throw OwnRecipeException(OwnRecipeError.Invalid(field))
    private fun List<RecipeIngredient>.hasDuplicates() = map { it.ingredientId }.distinct().size != size

    companion object {
        const val MIN_NAME = 2
        const val MAX_NAME = 60
        const val MAX_INGREDIENTS = 20
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 240
        const val MAX_PREPARATION = 2_000
    }
}
