package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUse
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.Store
import kotlin.math.ceil

/**
 * Nombre: RecipeEvaluator.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Evalúa una receta contra lo que queda del inventario (spec 009 §2, plan §3.2, T010 y T031): qué sale
 * del inventario, qué falta comprar y cuánto cuesta, qué productos por vencer rescata y su aporte a la utilidad. También
 * calcula el desperdicio y la utilidad de un plan completo.
 *
 * Lo que falta se compra **por empaques completos** al precio de la tienda elegida, y el sobrante queda para los días
 * siguientes del plan sin volver a pagarlo. Los básicos de despensa no se compran ni cuentan como faltantes.
 *
 * Propiedades que usa el agente (#114, T011): el rescate de una receta **solo puede bajar** a medida que se asignan días
 * (los productos del inventario se gastan y los comprados no vencen), y el costo nunca es negativo. Como el sobrante de
 * un empaque puede abaratar los días siguientes, [MealPlanner] acota esos días solo con el rescate.
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
     * Evalúa [recipe] escalada a [servings] porciones, cocinada el día [day] (1 = hoy) con el inventario [stock], con los
     * precios de [store].
     */
    fun evaluate(recipe: Recipe, servings: Int, day: Int, stock: PantryStock, store: Store = Store.D1): Evaluation {
        val scale = servings.toDouble() / recipe.servings
        var remaining = stock
        val uses = recipe.ingredients.map { ri ->
            val needed = ri.quantity * scale
            val (use, after) = take(ri.ingredientId, needed, day, remaining, store)
            remaining = after
            use
        }
        val rescue = uses.sumOf { use -> use.rescuedItems.sumOf { id -> rescueOf(id, use, stock) } }
        val cost = uses.sumOf { it.buyCostCop }
        // Cubierto: no hay que comprar nada ni se usa lo comprado otro día (un básico se asume en casa).
        val covered = uses.count { !it.isMissing && it.fromPurchase <= EPSILON }
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
     * en buen estado el día [day]; después, del sobrante de empaques comprados otros días. Lo que todavía falta se compra
     * en empaques completos en [store] (o en la tienda más barata que lo tenga) y el sobrante queda para después. Un
     * básico de despensa no se compra.
     */
    private fun take(
        ingredientId: String,
        needed: Double,
        day: Int,
        stock: PantryStock,
        store: Store,
    ): Pair<IngredientUse, PantryStock> {
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
        val fromPurchase = minOf(current.purchasedOf(ingredientId), pending.coerceAtLeast(0.0))
        if (fromPurchase > EPSILON) {
            current = current.withPurchased(ingredientId, current.purchasedOf(ingredientId) - fromPurchase)
            pending -= fromPurchase
        }
        if (pending <= EPSILON || ingredient.pantryBasic) {
            return IngredientUse(ingredientId, needed, fromPantry, 0.0, 0L, rescued, fromPurchase) to current
        }
        val packs = ceil(pending / ingredient.pack - EPSILON).toInt()
        val price = ingredient.priceAt(store)
        val leftover = packs * ingredient.pack - pending
        current = current.withPurchased(ingredientId, current.purchasedOf(ingredientId) + leftover)
        val use = IngredientUse(
            ingredientId, needed, fromPantry, pending, packs * price.priceCop, rescued, fromPurchase, packs, price.store,
        )
        return use to current
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
 * @property purchased Sobrante de los empaques comprados en el plan, por `ingredientId` (no vence ni cuenta como
 *   desperdicio).
 */
class PantryStock private constructor(
    val items: List<PantryItem>,
    private val remaining: Map<String, Double>,
    private val usedWhole: Set<String>,
    private val purchased: Map<String, Double> = emptyMap(),
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
        if (item(itemId).quantity == null || amount == null) PantryStock(items, remaining, usedWhole + itemId, purchased)
        else PantryStock(
            items,
            remaining + (itemId to ((remaining[itemId] ?: 0.0) - amount).coerceAtLeast(0.0)),
            usedWhole,
            purchased,
        )

    /** Lo que sobra de los empaques comprados de [ingredientId] en el plan. */
    fun purchasedOf(ingredientId: String): Double = purchased[ingredientId] ?: 0.0

    /** Estado con [amount] de sobrante comprado de [ingredientId]. */
    fun withPurchased(ingredientId: String, amount: Double): PantryStock =
        PantryStock(items, remaining, usedWhole, purchased + (ingredientId to amount.coerceAtLeast(0.0)))

    companion object {
        /** Inventario completo, sin nada usado. */
        fun of(items: List<PantryItem>): PantryStock =
            PantryStock(items, items.filter { it.quantity != null }.associate { it.itemId to it.quantity!! }, emptySet())
    }
}
