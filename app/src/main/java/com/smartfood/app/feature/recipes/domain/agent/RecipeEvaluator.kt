package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUse
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Recipe
import kotlin.math.ceil

/**
 * Nombre: RecipeEvaluator.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Evalúa una receta contra lo que queda del inventario (spec 009 §2, plan §3.2, T010): qué sale del
 * inventario, qué falta comprar y cuánto cuesta, qué productos por vencer rescata y su aporte a la utilidad. También
 * calcula el desperdicio y la utilidad de un plan completo.
 *
 * Propiedad que usa el agente (#114, T011): el inventario solo disminuye al asignar recetas, así que con un inventario
 * menor el costo y los faltantes de una receta **solo pueden subir** y su rescate **solo puede bajar**. Por eso el
 * forward checking y la cota optimista de [MealPlanner] nunca descartan un plan válido.
 *
 * @param ingredients Ingredientes del catálogo por `id`.
 * @param weights Pesos de la función de utilidad.
 */
class RecipeEvaluator(
    private val ingredients: Map<String, Ingredient>,
    private val weights: PlannerWeights = PlannerWeights(),
) {

    /**
     * Resultado de evaluar una receta en un día.
     *
     * @property uses Uso de cada ingrediente.
     * @property costCop Costo de lo que falta, en pesos.
     * @property missingCount Ingredientes que hay que comprar.
     * @property pantryShare Fracción de los ingredientes cubiertos por completo por el inventario.
     * @property rescue Rescate ponderado por urgencia (spec §2).
     * @property utility Aporte a la utilidad: `rescue·rescate − cost·costo/10.000` (el desperdicio es del plan).
     * @property remaining Inventario que queda después de cocinarla.
     */
    data class Evaluation(
        val uses: List<IngredientUse>,
        val costCop: Long,
        val missingCount: Int,
        val pantryShare: Double,
        val rescue: Double,
        val utility: Double,
        val remaining: PantryStock,
    )

    /**
     * Evalúa [recipe] escalada a [servings] porciones, cocinada el día [day] (1 = hoy) con el inventario [stock].
     */
    fun evaluate(recipe: Recipe, servings: Int, day: Int, stock: PantryStock): Evaluation {
        val scale = servings.toDouble() / recipe.servings
        var remaining = stock
        val uses = recipe.ingredients.map { ri ->
            val needed = ri.quantity * scale
            val (use, after) = take(ri.ingredientId, needed, day, remaining)
            remaining = after
            use
        }
        val rescue = uses.sumOf { use -> use.rescuedItems.sumOf { id -> rescueOf(id, use, stock) } }
        val cost = uses.sumOf { it.buyCostCop }
        val covered = uses.count { !it.isMissing }
        return Evaluation(
            uses = uses,
            costCop = cost,
            missingCount = uses.count { it.isMissing },
            pantryShare = if (uses.isEmpty()) 0.0 else covered.toDouble() / uses.size,
            rescue = rescue,
            utility = weights.rescue * rescue - weights.cost * cost / COST_SCALE,
            remaining = remaining,
        )
    }

    /** Productos que vencen en ≤ 3 días y siguen sin usarse en [stock], al final del plan. */
    fun wastedItems(stock: PantryStock): List<String> =
        stock.items.filter { it.isExpiringSoon() && stock.unusedFraction(it.itemId) > 0.0 }.map { it.itemId }

    /** Desperdicio: suma de la fracción sin usar de los productos que vencen en ≤ 3 días (spec §2). */
    fun waste(stock: PantryStock): Double =
        stock.items.filter { it.isExpiringSoon() }.sumOf { stock.unusedFraction(it.itemId) }

    /** Utilidad de un plan: suma de los aportes de cada día menos el desperdicio ponderado. */
    fun planUtility(dayUtilities: List<Double>, finalStock: PantryStock): Double =
        dayUtilities.sum() - weights.waste * waste(finalStock)

    /**
     * Saca [needed] de [ingredientId] del inventario: primero de los productos que vencen antes, solo de los que siguen
     * en buen estado el día [day]. Lo que falta se compra al precio de referencia.
     */
    private fun take(ingredientId: String, needed: Double, day: Int, stock: PantryStock): Pair<IngredientUse, PantryStock> {
        val ingredient = ingredients[ingredientId]
            ?: throw IllegalArgumentException("El ingrediente '$ingredientId' no está en el catálogo")
        var pending = needed
        var current = stock
        val fromPantry = linkedMapOf<String, Double?>()
        val rescued = mutableListOf<String>()
        val candidates = stock.items
            .filter { it.ingredientId == ingredientId && it.isUsableOn(day) && current.isAvailable(it.itemId) }
            .sortedWith(compareBy<PantryItem>({ it.daysToExpire }, { it.itemId }))
        for (item in candidates) {
            if (pending <= EPSILON) break
            val left = current.remainingOf(item.itemId)
            if (left == null) {
                // Producto sin cantidad: alcanza para la receta y se usa completo (spec §7).
                fromPantry[item.itemId] = null
                pending = 0.0
            } else {
                val used = minOf(left, pending)
                fromPantry[item.itemId] = used
                pending -= used
            }
            current = current.consume(item.itemId, fromPantry.getValue(item.itemId))
            if (item.isExpiringSoon()) rescued += item.itemId
        }
        val toBuy = if (pending <= EPSILON) 0.0 else pending
        val cost = if (toBuy == 0.0) 0L else ceil(toBuy * ingredient.pricePerUnitCop()).toLong()
        return IngredientUse(ingredientId, needed, fromPantry, toBuy, cost, rescued) to current
    }

    /** Rescate de un producto en un uso: fracción usada de su cantidad original por la urgencia `1/(días + 1)`. */
    private fun rescueOf(itemId: String, use: IngredientUse, before: PantryStock): Double {
        val item = before.item(itemId)
        val original = item.quantity
        val fraction = when (val taken = use.fromPantry[itemId]) {
            null -> 1.0
            else -> if (original == null || original <= 0.0) 1.0 else (taken / original).coerceAtMost(1.0)
        }
        return fraction / (item.daysToExpire + 1)
    }

    companion object {
        /** El costo entra a la utilidad en decenas de miles de pesos (spec §2). */
        const val COST_SCALE = 10_000.0

        /** Un producto "por vencer" vence en 3 días o menos (spec §2, Avance 2). */
        const val EXPIRING_DAYS = 3

        private const val EPSILON = 1e-9
    }
}

/** El producto vence en ≤ 3 días y todavía no ha vencido. */
internal fun PantryItem.isExpiringSoon(): Boolean = daysToExpire in 0..RecipeEvaluator.EXPIRING_DAYS

/** El producto sigue en buen estado el día [day] del plan (1 = hoy). Los vencidos no se usan nunca. */
internal fun PantryItem.isUsableOn(day: Int): Boolean = daysToExpire >= day - 1

/**
 * Lo que queda del inventario mientras el agente arma un plan. Inmutable: cada consumo devuelve un estado nuevo, así la
 * búsqueda puede volver atrás sin deshacer nada.
 *
 * @property items Productos del inventario emparejados con el catálogo.
 * @property remaining Cantidad que queda de cada producto con cantidad, por `itemId`.
 * @property usedWhole Productos sin cantidad que ya se usaron completos.
 */
class PantryStock private constructor(
    val items: List<PantryItem>,
    private val remaining: Map<String, Double>,
    private val usedWhole: Set<String>,
) {
    private val byId = items.associateBy { it.itemId }

    /** El producto con [itemId]. */
    fun item(itemId: String): PantryItem = byId.getValue(itemId)

    /** `true` si al producto todavía le queda algo. */
    fun isAvailable(itemId: String): Boolean {
        val item = item(itemId)
        return if (item.quantity == null) itemId !in usedWhole else (remaining[itemId] ?: 0.0) > 0.0
    }

    /** Lo que queda del producto, o `null` si es un producto sin cantidad todavía sin usar. */
    fun remainingOf(itemId: String): Double? = if (item(itemId).quantity == null) null else remaining[itemId] ?: 0.0

    /** Fracción del producto que sigue sin usarse (1 = intacto). */
    fun unusedFraction(itemId: String): Double {
        val item = item(itemId)
        val original = item.quantity ?: return if (itemId in usedWhole) 0.0 else 1.0
        return if (original <= 0.0) 0.0 else ((remaining[itemId] ?: 0.0) / original).coerceIn(0.0, 1.0)
    }

    /** Estado después de usar [amount] del producto (`null` = el producto completo). */
    fun consume(itemId: String, amount: Double?): PantryStock =
        if (item(itemId).quantity == null || amount == null) PantryStock(items, remaining, usedWhole + itemId)
        else PantryStock(items, remaining + (itemId to ((remaining[itemId] ?: 0.0) - amount).coerceAtLeast(0.0)), usedWhole)

    companion object {
        /** Inventario completo, sin nada usado. */
        fun of(items: List<PantryItem>): PantryStock =
            PantryStock(items, items.filter { it.quantity != null }.associate { it.itemId to it.quantity!! }, emptySet())
    }
}
