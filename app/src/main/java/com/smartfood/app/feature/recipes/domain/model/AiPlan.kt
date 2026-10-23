package com.smartfood.app.feature.recipes.domain.model

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer

/**
 * Nombre: AiPlan.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Modelos del flujo de la IA (spec 009, HU-RIA-02, T014): lo que el agente necesita del inventario y del
 * Perfil, y su resultado con la explicación de "¿Por qué te conviene?".
 */

/**
 * Lo que el agente necesita del inventario y del Perfil (CA-RIA-02-01, 02-09).
 *
 * @property pantry Productos del inventario ya emparejados con el catálogo (`IngredientMatcher`).
 * @property items Los mismos productos, para mostrar sus nombres y vencimientos, ordenados por fecha de vencimiento.
 * @property ingredients Ingredientes del catálogo por `id`, para nombrarlos con su unidad en la explicación.
 * @property restrictions Restricciones alimentarias del Perfil, para "Según tus preferencias".
 * @property diet Las mismas restricciones como reglas del agente.
 * @property store Supermercado de los precios ("Mi supermercado").
 */
data class AiContext(
    val pantry: List<PantryItem>,
    val items: List<InventoryItem>,
    val ingredients: Map<String, Ingredient>,
    val restrictions: Set<DietaryRestriction>,
    val diet: Set<DietRule>,
    val store: Store,
)

/** Resultado del agente con su explicación (CA-RIA-02-03, 02-06, 02-07). */
sealed interface AiPlanOutcome {

    /** El plan de 3 días y las razones de "¿Por qué te conviene?". */
    data class Found(val plan: MealPlan, val reasons: List<PlanExplainer.Reason>) : AiPlanOutcome

    /** Ningún plan cumple; [suggestions] dice qué cambiar. */
    data class NoPlan(val suggestions: List<PlanSuggestion>) : AiPlanOutcome
}
