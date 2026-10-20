package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Nombre: IngredientMatcher.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Empareja los productos del inventario con los ingredientes del catálogo (spec 009 §2, plan §3.1, T005,
 * RF-RIA-02). Un producto corresponde a un ingrediente si su nombre normalizado contiene el nombre o algún sinónimo del
 * ingrediente; gana el texto más largo. Convierte la cantidad a la unidad base del ingrediente (kg, lb y l incluidos) y
 * calcula los días para vencer. Si las unidades no son compatibles, la cantidad queda `null`: el producto alcanza para
 * una receta (spec §7).
 *
 * @param clock Reloj del teléfono; las pruebas inyectan uno fijo para calcular el vencimiento.
 */
class IngredientMatcher @Inject constructor(
    private val clock: Clock,
) {

    /**
     * Empareja [items] con [ingredients] y devuelve solo los productos que corresponden a algún ingrediente.
     */
    fun match(
        items: List<InventoryItem>,
        ingredients: Map<String, Ingredient>,
    ): List<PantryItem> {
        val candidates = ingredients.values.flatMap { ingredient ->
            candidateTexts(ingredient).map { text -> ingredient.id to text }
        }
        val today = LocalDate.now(clock)
        return items.mapNotNull { item ->
            val name = SearchInventoryUseCase.normalize(item.name)
            val match = candidates
                .filter { (_, text) -> text.isNotEmpty() && name.contains(text) }
                .minWithOrNull(
                    compareByDescending<Pair<String, String>> { it.second.length }.thenBy { it.first }
                )
                ?: return@mapNotNull null
            val ingredient = ingredients.getValue(match.first)
            PantryItem(
                itemId = item.id,
                ingredientId = ingredient.id,
                quantity = item.quantity?.toIngredientUnit(ingredient.unit),
                daysToExpire = ChronoUnit.DAYS.between(today, item.expiresOn).toInt(),
            )
        }
    }

    private fun candidateTexts(ingredient: Ingredient): List<String> =
        (listOf(ingredient.name) + ingredient.synonyms).map(SearchInventoryUseCase::normalize)

    /**
     * Convierte una cantidad del inventario a la unidad base del ingrediente. Si las unidades no son compatibles
     * (por ejemplo, gramos contra unidades, o mililitros contra gramos) devuelve `null`.
     */
    private fun Quantity.toIngredientUnit(
        target: IngredientUnit,
    ): Double? = when (target) {
        IngredientUnit.GRAM -> when (unit) {
            QuantityUnit.GRAMS -> amount
            QuantityUnit.KILOGRAMS -> amount * GRAMS_PER_KILOGRAM
            QuantityUnit.POUNDS -> amount * GRAMS_PER_POUND
            else -> null
        }

        IngredientUnit.MILLILITER -> when (unit) {
            QuantityUnit.MILLILITERS -> amount
            QuantityUnit.LITERS -> amount * MILLILITERS_PER_LITER
            else -> null
        }

        IngredientUnit.UNIT -> if (unit == QuantityUnit.UNITS) amount else null
    }

    private companion object {
        const val GRAMS_PER_KILOGRAM = 1000.0
        const val GRAMS_PER_POUND = 453.59237
        const val MILLILITERS_PER_LITER = 1000.0
    }
}
