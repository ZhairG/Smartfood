package com.smartfood.app.feature.recipes.ui.own

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.smartfood.app.R
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.ui.components.formatQuantity

/**
 * Nombre: OwnRecipeText.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Único lugar que traduce categorías, unidades y errores de recetas propias a `strings_saved.xml`
 * (spec 010, RNF-SAV-03, T013).
 */

/** Nombre visible de la categoría (spec §7). */
val RecipeCategory.titleRes: Int
    @StringRes get() = when (this) {
        RecipeCategory.AREPAS -> R.string.own_category_arepas
        RecipeCategory.ARROCES -> R.string.own_category_arroces
        RecipeCategory.CARNES -> R.string.own_category_carnes
        RecipeCategory.POLLO -> R.string.own_category_pollo
        RecipeCategory.PESCADOS -> R.string.own_category_pescados
        RecipeCategory.PASTAS -> R.string.own_category_pastas
        RecipeCategory.SOPAS -> R.string.own_category_sopas
        RecipeCategory.ENSALADAS -> R.string.own_category_ensaladas
        RecipeCategory.DESAYUNOS -> R.string.own_category_desayunos
        RecipeCategory.POSTRES -> R.string.own_category_postres
        RecipeCategory.OTRAS -> R.string.own_category_otras
    }

/** Cantidad con su unidad, como "1 und". */
@Composable
fun ingredientQuantityText(quantity: Double, ingredient: Ingredient?): String = stringResource(
    id = when (ingredient?.unit) {
        IngredientUnit.GRAM -> R.string.recipes_unit_grams
        IngredientUnit.MILLILITER -> R.string.recipes_unit_milliliters
        IngredientUnit.UNIT, null -> R.string.recipes_unit_units
    },
    formatQuantity(quantity),
)

/**
 * Motivo del aviso rojo (CA-SAV-03-06).
 *
 * @param error Error del intento de guardar.
 * @param cookMinutes Tiempo escrito: decide si el problema es por poco o por mucho.
 */
@Composable
fun ownRecipeErrorReason(error: OwnRecipeError, cookMinutes: String): String = stringResource(
    id = when (error) {
        is OwnRecipeError.Invalid -> when (error.field) {
            OwnRecipeField.NAME -> R.string.own_error_reason_name
            OwnRecipeField.CATEGORY -> R.string.own_error_reason_category
            OwnRecipeField.INGREDIENTS -> R.string.own_error_reason_ingredients
            OwnRecipeField.COOK_MINUTES ->
                if ((cookMinutes.toIntOrNull() ?: 0) > MAX_MINUTES) {
                    R.string.own_error_reason_time_high
                } else {
                    R.string.own_error_reason_time_low
                }
            OwnRecipeField.PREPARATION -> R.string.own_error_reason_preparation
        }
        OwnRecipeError.NotFound -> R.string.own_error_not_found
        OwnRecipeError.Network -> R.string.own_error_network
        OwnRecipeError.NotAuthenticated, OwnRecipeError.PermissionDenied -> R.string.own_error_session
        is OwnRecipeError.Unknown -> R.string.own_error_unknown
    },
)

/** Mensaje debajo del campo en rojo (Figma `557:2671`). */
val OwnRecipeField.messageRes: Int
    @StringRes get() = when (this) {
        OwnRecipeField.NAME -> R.string.own_error_field_name
        OwnRecipeField.CATEGORY -> R.string.own_error_field_category
        OwnRecipeField.INGREDIENTS -> R.string.own_error_field_ingredients
        OwnRecipeField.COOK_MINUTES -> R.string.own_error_field_time
        OwnRecipeField.PREPARATION -> R.string.own_error_field_preparation
    }

private const val MAX_MINUTES = 240
