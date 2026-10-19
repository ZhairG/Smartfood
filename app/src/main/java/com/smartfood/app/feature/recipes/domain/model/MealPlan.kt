package com.smartfood.app.feature.recipes.domain.model

/**
 * Nombre: MealPlan.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Entrada y salida del agente de plan de 3 días (spec 009 §2, plan §3, T009). El agente recibe el
 * inventario ya emparejado con el catálogo ([PantryItem]) y el contexto elegido ([PlanContext]), y devuelve un
 * [PlanResult]: el plan de mayor utilidad o, si ninguno cumple las restricciones, sugerencias para ajustar.
 */

/**
 * Producto del inventario ya emparejado con un ingrediente del catálogo (lo produce `IngredientMatcher`, #113).
 *
 * @property itemId Identificador del producto en el inventario, para descontarlo al cocinar.
 * @property ingredientId Identificador del [Ingredient] del catálogo.
 * @property quantity Cantidad disponible en la unidad base del ingrediente, o `null` si el producto no tiene cantidad:
 *   en ese caso alcanza para una receta y se usa completo (spec §7).
 * @property daysToExpire Días que faltan para vencer (0 = vence hoy; negativo = ya venció).
 */
data class PantryItem(
    val itemId: String,
    val ingredientId: String,
    val quantity: Double?,
    val daysToExpire: Int,
)

/**
 * Lo que la persona eligió en "Sugerencia con IA" (CA-RIA-02-01).
 *
 * @property budgetCop Presupuesto máximo en pesos para las compras de todo el plan.
 * @property servings Porciones de cada receta.
 * @property maxMinutes Tiempo total máximo de cada receta, o `null` si no hay límite.
 * @property noSpicy `true` si eligió "Sin picante".
 * @property maxMissing Máximo de ingredientes por comprar en cada receta (spec §2: 3).
 * @property days Días del plan (spec §2: 3).
 */
data class PlanContext(
    val budgetCop: Long,
    val servings: Int,
    val maxMinutes: Int? = null,
    val noSpicy: Boolean = false,
    val maxMissing: Int = 3,
    val days: Int = 3,
)

/**
 * Pesos de la función de utilidad `U = rescue·rescate − cost·(costo / 10.000) − waste·desperdicio` (spec §2). Son
 * heurísticos y se justifican con la prueba de sensibilidad (RNF-RIA-03).
 */
data class PlannerWeights(
    val rescue: Double = 1.0,
    val cost: Double = 0.3,
    val waste: Double = 0.5,
)

/**
 * Uso de un ingrediente en una receta del plan.
 *
 * @property ingredientId Ingrediente del catálogo.
 * @property needed Cantidad que pide la receta escalada a las porciones, en la unidad base.
 * @property fromPantry Cantidad que sale de cada producto del inventario, por `itemId` (`null` = el producto completo,
 *   cuando no tenía cantidad).
 * @property toBuy Cantidad que hay que comprar, en la unidad base.
 * @property buyCostCop Costo de [toBuy] con el precio de referencia, en pesos.
 * @property rescuedItems Productos que vencen en ≤ 3 días y esta receta usa antes de que venzan.
 */
data class IngredientUse(
    val ingredientId: String,
    val needed: Double,
    val fromPantry: Map<String, Double?>,
    val toBuy: Double,
    val buyCostCop: Long,
    val rescuedItems: List<String> = emptyList(),
) {
    /** `true` si hay que comprar algo de este ingrediente. */
    val isMissing: Boolean get() = toBuy > 0.0
}

/**
 * Una receta del plan en su día.
 *
 * @property day Día del plan, desde 1.
 * @property recipe Receta elegida.
 * @property uses Uso de cada ingrediente de la receta.
 * @property costCop Compras de esta receta, en pesos.
 * @property pantryShare Fracción de los ingredientes de la receta que cubre el inventario (0 a 1), para "92% del
 *   inventario".
 * @property utility Aporte de esta receta a la utilidad del plan.
 */
data class PlannedDay(
    val day: Int,
    val recipe: Recipe,
    val uses: List<IngredientUse>,
    val costCop: Long,
    val pantryShare: Double,
    val utility: Double,
)

/**
 * Plan de varios días.
 *
 * @property days Recetas del plan, en orden de día.
 * @property totalCostCop Compras de todo el plan, en pesos.
 * @property remainingBudgetCop Lo que queda del presupuesto.
 * @property wastedItems Productos que vencen en ≤ 3 días y el plan no usa.
 * @property utility Utilidad total del plan.
 */
data class MealPlan(
    val days: List<PlannedDay>,
    val totalCostCop: Long,
    val remainingBudgetCop: Long,
    val wastedItems: List<String>,
    val utility: Double,
)

/** Sugerencia para conseguir un plan cuando no hay ninguno (CA-RIA-02-07). */
sealed interface PlanSuggestion {
    /** Con este presupuesto sí habría plan. */
    data class RaiseBudget(val minimumBudgetCop: Long) : PlanSuggestion

    /** Quitando "Sin picante" sí habría plan. */
    data object AllowSpicy : PlanSuggestion

    /** Con este tiempo máximo sí habría plan. */
    data class AllowMoreTime(val minutes: Int) : PlanSuggestion

    /** El catálogo no tiene suficientes recetas que cumplan el contexto, aun sin presupuesto. */
    data object NotEnoughRecipes : PlanSuggestion
}

/** Resultado del agente. */
sealed interface PlanResult {
    /** El plan de mayor utilidad entre todos los válidos (CA-RIA-02-04). */
    data class Found(val plan: MealPlan) : PlanResult

    /** Ningún plan cumple las restricciones; [suggestions] dice qué cambiar (CA-RIA-02-07). */
    data class NoPlan(val suggestions: List<PlanSuggestion>) : PlanResult
}
