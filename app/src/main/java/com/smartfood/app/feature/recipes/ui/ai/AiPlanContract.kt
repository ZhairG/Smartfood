package com.smartfood.app.feature.recipes.ui.ai

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.domain.model.Store

/**
 * Nombre: AiPlanContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UDF del flujo de la IA (spec 009, HU-RIA-02, T014 a T017; plan §5): una sola ruta con cuatro
 * pasos (contexto, analizando, plan y sin coincidencias) para que el plan no pase entre pantallas.
 */

/** Paso del flujo que se ve. */
enum class AiStep { Context, Loading, Plan, NoPlan }

/** Hoja de opciones abierta en el contexto (desviación aprobada, spec §9). */
enum class AiSheet { Servings, Time }

/**
 * Estado del flujo de la IA.
 *
 * @property isLoadingContext Se está leyendo el inventario y el Perfil.
 * @property step Paso visible.
 * @property detected Productos del inventario que usan las recetas, del que vence antes al que vence después.
 * @property expiringIds Los que vencen en 0 a 3 días: su chip va resaltado.
 * @property firstExpiring El que vence antes, si vence en 0 a 3 días: "⚠ El pollo vence mañana." (CA-RIA-02-01).
 * @property firstExpiringDays Días que le faltan a [firstExpiring] (0 = hoy).
 * @property restrictions Restricciones del Perfil para "Según tus preferencias" (no se cambian aquí).
 * @property store Supermercado de los precios, para "COP $50.000 · D1".
 * @property budgetCop Presupuesto máximo (CA-RIA-02-01: de $2.000 a $100.000).
 * @property servings Porciones de cada receta.
 * @property maxMinutes Tiempo máximo por receta, o `null` para "Sin límite".
 * @property noSpicy "Sin picante" activo.
 * @property sheet Hoja de opciones abierta.
 * @property plan Plan encontrado (paso [AiStep.Plan]).
 * @property reasons Razones de "¿Por qué te conviene?" (CA-RIA-02-06).
 * @property suggestions Sugerencias de "Sin coincidencias" (CA-RIA-02-07).
 * @property ingredients Ingredientes del catálogo por `id`, para escribir las razones con su nombre y unidad.
 */
@Immutable
data class AiUiState(
    val isLoadingContext: Boolean = true,
    val step: AiStep = AiStep.Context,
    val detected: List<InventoryItem> = emptyList(),
    val expiringIds: Set<String> = emptySet(),
    val firstExpiring: InventoryItem? = null,
    val firstExpiringDays: Int = 0,
    val restrictions: Set<DietaryRestriction> = emptySet(),
    val store: Store = Store.D1,
    val budgetCop: Long = DEFAULT_BUDGET_COP,
    val servings: Int = DEFAULT_SERVINGS,
    val maxMinutes: Int? = null,
    val noSpicy: Boolean = false,
    val sheet: AiSheet? = null,
    val plan: MealPlan? = null,
    val reasons: List<PlanExplainer.Reason> = emptyList(),
    val suggestions: List<PlanSuggestion> = emptyList(),
    val ingredients: Map<String, Ingredient> = emptyMap(),
) {
    companion object {
        const val DEFAULT_BUDGET_COP = 50_000L
        const val DEFAULT_SERVINGS = 4
        const val MIN_BUDGET_COP = 2_000L
        const val MAX_BUDGET_COP = 100_000L
        const val BUDGET_STEP_COP = 1_000L

        /** Opciones de la hoja de porciones. */
        val SERVINGS_OPTIONS = (1..12).toList()

        /** Opciones de la hoja de tiempo; `null` es "Sin límite". */
        val TIME_OPTIONS: List<Int?> = listOf(null, 20, 35, 45, 60)
    }
}

sealed interface AiEvent {
    data class BudgetChanged(val cop: Long) : AiEvent
    data object ServingsClicked : AiEvent
    data class ServingsPicked(val servings: Int) : AiEvent
    data object TimeClicked : AiEvent
    data class TimePicked(val minutes: Int?) : AiEvent
    data object SpicyToggled : AiEvent
    data object DismissSheet : AiEvent
    data object GenerateClicked : AiEvent
    data object CancelClicked : AiEvent

    /** "Ajustar" del plan y "Ajustar contexto": vuelve al contexto con los mismos datos (CA-RIA-02-08). */
    data object AdjustClicked : AiEvent
    data class RecipeClicked(val recipeId: String) : AiEvent
    data object EconomicRecipesClicked : AiEvent
    data object HelpClicked : AiEvent
}

sealed interface AiEffect {
    data class OpenRecipe(val recipeId: String) : AiEffect
    data object OpenEconomicRecipes : AiEffect
    data object OpenHelp : AiEffect
}
