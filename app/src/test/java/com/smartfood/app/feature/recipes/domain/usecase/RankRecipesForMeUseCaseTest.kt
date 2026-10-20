package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Nombre: RankRecipesForMeUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas del orden de Descubrir (spec 009, CA-RIA-01-02 y 01-03, T007): "Para ti" por utilidad con el
 * inventario actual, el aviso del ingrediente por vencer que rescata, el costo de lo que falta y los filtros "≤ 30 min"
 * y "Económicas".
 */
class RankRecipesForMeUseCaseTest {

    private val hoy = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(hoy.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val rank = RankRecipesForMeUseCase(IngredientMatcher(clock))

    private val ingredients = mapOf(
        "pollo" to ingredient("pollo", "Pollo", priceCop = 14_500, synonyms = listOf("pechuga")),
        "arroz" to ingredient("arroz", "Arroz", priceCop = 4_000),
        "carne" to ingredient("carne", "Carne de res", priceCop = 10_000),
    )

    private val polloGuisado = recipe(
        id = "pollo-guisado",
        name = "Pollo guisado con arroz",
        minutes = 35,
        ingredients = listOf(RecipeIngredient("pollo", 500.0), RecipeIngredient("arroz", 200.0)),
    )

    private val carneAsada = recipe(
        id = "carne-asada",
        name = "Carne asada",
        minutes = 20,
        ingredients = listOf(RecipeIngredient("carne", 500.0)),
    )

    @Test
    fun `CA-RIA-01-02 para ti pone primero la receta que rescata el pollo por vencer`() {
        val inventory = listOf(
            item("pollo-1", "Pechuga de pollo", 1_000.0, hoy.plusDays(1)),
            item("arroz-1", "Arroz", 1_000.0, hoy.plusDays(10)),
        )

        val ranked = rank(listOf(polloGuisado, carneAsada), ingredients, inventory, RecipeFilter.FOR_YOU)

        assertEquals(listOf("pollo-guisado", "carne-asada"), ranked.map { it.recipe.id })
        assertEquals("Pollo", ranked.first().expiringIngredientName)
        // El pollo y el arroz están en el inventario: no hay nada que comprar.
        assertEquals(0L, ranked.first().costCop)
        assertNull(ranked.last().expiringIngredientName)
        // La carne falta por completo: 500 g a 10.000/kg.
        assertEquals(5_000L, ranked.last().costCop)
    }

    @Test
    fun `CA-RIA-01-03 economicas ordena de menor a mayor costo de compra`() {
        val inventory = listOf(
            item("pollo-1", "Pechuga de pollo", 1_000.0, hoy.plusDays(1)),
            item("arroz-1", "Arroz", 1_000.0, hoy.plusDays(10)),
        )

        val ranked = rank(listOf(carneAsada, polloGuisado), ingredients, inventory, RecipeFilter.ECONOMIC)

        assertEquals(listOf("pollo-guisado", "carne-asada"), ranked.map { it.recipe.id })
        assertEquals(listOf(0L, 5_000L), ranked.map { it.costCop })
    }

    @Test
    fun `CA-RIA-01-03 menores de 30 minutos deja solo las rápidas`() {
        val ranked = rank(listOf(polloGuisado, carneAsada), ingredients, emptyList(), RecipeFilter.UNDER_30)

        assertEquals(listOf("carne-asada"), ranked.map { it.recipe.id })
    }

    @Test
    fun `CA-RIA-01-02 sin inventario no rescata nada y el costo es el de todos los ingredientes`() {
        val ranked = rank(listOf(polloGuisado), ingredients, emptyList(), RecipeFilter.FOR_YOU)

        assertNull(ranked.single().expiringIngredientName)
        // 500 g de pollo (7.250) + 200 g de arroz (800).
        assertEquals(8_050L, ranked.single().costCop)
    }

    private fun ingredient(
        id: String,
        name: String,
        priceCop: Long,
        synonyms: List<String> = emptyList(),
    ) = Ingredient(
        id = id,
        name = name,
        synonyms = synonyms,
        section = "Otros",
        unit = IngredientUnit.GRAM,
        priceCop = priceCop,
        priceQuantity = 1_000.0,
    )

    private fun recipe(
        id: String,
        name: String,
        minutes: Int,
        ingredients: List<RecipeIngredient>,
    ) = Recipe(
        id = id,
        name = name,
        servings = 4,
        prepMinutes = 10,
        cookMinutes = minutes - 10,
        difficulty = "Fácil",
        kcal = 500,
        ingredients = ingredients,
        steps = emptyList(),
    )

    private fun item(id: String, name: String, grams: Double, expiresOn: LocalDate) = InventoryItem(
        id = id,
        name = name,
        category = FoodCategory.PROTEINS,
        expiresOn = expiresOn,
        quantity = Quantity(grams, QuantityUnit.GRAMS),
    )
}
