package com.smartfood.app.feature.recipes.ui.ai

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion

/** Nombre: AiPlanContract.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Contrato UDF del flujo IA. */
data class AiUiState(
    val step: AiStep = AiStep.Context,
    val context: PlanContext = PlanContext(50_000, 4),
    val detected: List<InventoryItem> = emptyList(),
    val firstExpiring: InventoryItem? = null,
    val restrictions: List<String> = emptyList(),
    val plan: MealPlan? = null,
    val suggestions: List<PlanSuggestion> = emptyList(),
)

enum class AiStep { Context, Loading, Plan, NoPlan }

sealed interface AiEvent {
    data object GenerateClicked : AiEvent
    data object CancelClicked : AiEvent
    data object AdjustClicked : AiEvent
    data object HelpClicked : AiEvent
    data class RecipeClicked(val recipeId: String) : AiEvent
    data object EconomicRecipesClicked : AiEvent
    data class BudgetChanged(val value: Long) : AiEvent
    data class ServingsChanged(val value: Int) : AiEvent
    data class NoSpicyChanged(val value: Boolean) : AiEvent
    data class MaxMinutesChanged(val value: Int?) : AiEvent
}

sealed interface AiEffect {
    data class OpenRecipe(val recipeId: String) : AiEffect
    data object OpenEconomicRecipes : AiEffect
    data object OpenHelp : AiEffect
}
