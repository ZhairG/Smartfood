package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PantryItem

/**
 * Nombre: PlanExplainer.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Explica por qué conviene el plan del agente (spec 009, CA-RIA-02-06, plan §3.3, T012). Devuelve razones
 * estructuradas y no frases: la UI (#115) las convierte en texto con `strings_recipes.xml`, así `domain` no lleva
 * textos (RNF-03). El orden es el de la tarjeta "¿Por qué te conviene?": lo que rescata, lo que aprovecha, lo que falta
 * comprar, lo que queda del presupuesto y lo que no alcanza a usar.
 */
class PlanExplainer {

    /** Una razón de "¿Por qué te conviene?". */
    sealed interface Reason {
        /**
         * Usa [quantity] (en la unidad base; `null` = el producto completo) de [ingredientId], que vence en
         * [daysToExpire] días, el día [day] del plan.
         */
        data class RescuesExpiring(
            val ingredientId: String,
            val quantity: Double?,
            val daysToExpire: Int,
            val day: Int,
        ) : Reason

        /** Aprovecha [covered] de los [total] ingredientes del plan con lo que hay en casa. */
        data class UsesPantry(val covered: Int, val total: Int) : Reason

        /** Falta comprar [quantity] de [ingredientId], por aproximadamente [costCop] pesos. */
        data class NeedsToBuy(val ingredientId: String, val quantity: Double, val costCop: Long) : Reason

        /** No hay que comprar nada. */
        data object NothingToBuy : Reason

        /** Queda [cop] del presupuesto. */
        data class BudgetLeft(val cop: Long) : Reason

        /** Estos productos por vencer no alcanzan a usarse en el plan. */
        data class NotUsed(val itemIds: List<String>) : Reason
    }

    /** Razones del [plan], con los datos de vencimiento de [pantry]. */
    fun explain(plan: MealPlan, pantry: List<PantryItem>): List<Reason> {
        val byItem = pantry.associateBy { it.itemId }
        val reasons = mutableListOf<Reason>()

        // 1. Lo que rescata, en orden de día y de urgencia.
        plan.days.forEach { day ->
            day.uses.forEach { use ->
                use.rescuedItems.mapNotNull(byItem::get).sortedBy { it.daysToExpire }.forEach { item ->
                    reasons += Reason.RescuesExpiring(use.ingredientId, use.fromPantry[item.itemId], item.daysToExpire, day.day)
                }
            }
        }

        // 2. Cuántos ingredientes del plan salen por completo de la casa.
        val uses = plan.days.flatMap { it.uses }
        reasons += Reason.UsesPantry(covered = uses.count { !it.isMissing }, total = uses.size)

        // 3. Lo que falta comprar, sumado por ingrediente y de mayor a menor costo.
        val toBuy = uses.filter { it.isMissing }
            .groupBy { it.ingredientId }
            .map { (id, list) -> Reason.NeedsToBuy(id, list.sumOf { it.toBuy }, list.sumOf { it.buyCostCop }) }
            .sortedWith(compareByDescending<Reason.NeedsToBuy> { it.costCop }.thenBy { it.ingredientId })
        if (toBuy.isEmpty()) reasons += Reason.NothingToBuy else reasons += toBuy

        // 4. Lo que queda del presupuesto.
        reasons += Reason.BudgetLeft(plan.remainingBudgetCop)

        // 5. Lo que vence pronto y el plan no alcanza a usar.
        if (plan.wastedItems.isNotEmpty()) reasons += Reason.NotUsed(plan.wastedItems)
        return reasons
    }
}
