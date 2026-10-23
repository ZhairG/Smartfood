package com.smartfood.app.feature.recipes.ui.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.smartfood.app.R
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.ui.components.formatCop
import com.smartfood.app.feature.recipes.ui.components.formatQuantity

/**
 * Nombre: AiReasonText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único lugar que convierte las razones del agente ([PlanExplainer.Reason], CA-RIA-02-06) y las
 * sugerencias de "Sin coincidencias" ([PlanSuggestion], CA-RIA-02-07) en texto de `strings_recipes.xml`, como
 * `InventoryErrorText` hace con los errores (spec 009, T017). El dominio no lleva textos.
 */

/**
 * Frase de "¿Por qué te conviene?".
 *
 * @param ingredients Ingredientes del catálogo por `id`, para el nombre y la unidad.
 * @param items Productos del inventario, para nombrar los que no alcanzan a usarse.
 */
@Composable
fun PlanExplainer.Reason.text(ingredients: Map<String, Ingredient>, items: List<InventoryItem>): String = when (this) {
    is PlanExplainer.Reason.RescuesExpiring -> {
        val name = ingredientName(ingredientId, ingredients)
        val whenText = expiresText(daysToExpire)
        val amount = quantity
        if (amount == null) {
            stringResource(id = R.string.recipes_ai_reason_rescues_whole, name, whenText, day)
        } else {
            stringResource(id = R.string.recipes_ai_reason_rescues, quantityText(amount, ingredients[ingredientId]), name, whenText, day)
        }
    }
    is PlanExplainer.Reason.UsesPantry -> stringResource(id = R.string.recipes_ai_reason_pantry, covered, total)
    is PlanExplainer.Reason.NeedsToBuy ->
        stringResource(id = R.string.recipes_ai_reason_buy, ingredientName(ingredientId, ingredients), formatCop(costCop))
    PlanExplainer.Reason.NothingToBuy -> stringResource(id = R.string.recipes_ai_reason_nothing)
    is PlanExplainer.Reason.BudgetLeft -> stringResource(id = R.string.recipes_ai_reason_budget, formatCop(cop))
    is PlanExplainer.Reason.NotUsed -> {
        val byId = items.associateBy { it.id }
        stringResource(id = R.string.recipes_ai_reason_not_used, itemIds.mapNotNull { byId[it]?.name }.joinToString(", "))
    }
}

/** Sugerencia de "Prueba una de estas opciones". Nunca propone quitar las restricciones alimentarias. */
@Composable
fun PlanSuggestion.text(): String = when (this) {
    is PlanSuggestion.RaiseBudget -> stringResource(id = R.string.recipes_ai_suggest_budget, formatCop(minimumBudgetCop))
    PlanSuggestion.AllowSpicy -> stringResource(id = R.string.recipes_ai_suggest_spicy)
    is PlanSuggestion.AllowMoreTime -> stringResource(id = R.string.recipes_ai_suggest_time, minutes)
    PlanSuggestion.NotEnoughRecipes -> stringResource(id = R.string.recipes_ai_suggest_inventory)
}

/** "hoy", "mañana" o "en N días". */
@Composable
fun expiresText(days: Int): String = when (days) {
    0 -> stringResource(id = R.string.recipes_ai_today)
    1 -> stringResource(id = R.string.recipes_ai_tomorrow)
    else -> stringResource(id = R.string.recipes_ai_in_days, days)
}

private fun ingredientName(id: String, ingredients: Map<String, Ingredient>): String =
    ingredients[id]?.name?.lowercase() ?: id

/** "200 g", "250 ml" o "2 und", con la unidad base del ingrediente. */
@Composable
private fun quantityText(amount: Double, ingredient: Ingredient?): String {
    val value = formatQuantity(amount)
    return when (ingredient?.unit) {
        IngredientUnit.MILLILITER -> stringResource(id = R.string.recipes_unit_milliliters, value)
        IngredientUnit.UNIT -> stringResource(id = R.string.recipes_unit_units, value)
        else -> stringResource(id = R.string.recipes_unit_grams, value)
    }
}
