package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: SearchRecipesUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la búsqueda de Descubrir (spec 009, CA-RIA-01-04, T007): nombre o ingredientes, sin importar
 * mayúsculas ni tildes, y por sinónimos del catálogo. La consulta vacía devuelve todas las recetas.
 */
class SearchRecipesUseCaseTest {

    private val search = SearchRecipesUseCase()

    private val ingredients = mapOf(
        "pollo" to ingredient("pollo", "Pollo", synonyms = listOf("pechuga")),
        "arroz" to ingredient("arroz", "Arroz"),
        "atun" to ingredient("atun", "Atún"),
        "pasta" to ingredient("pasta", "Pasta"),
        "espinaca" to ingredient("espinaca", "Espinaca"),
    )

    private val recipes = listOf(
        recipe("pollo-guisado", "Pollo guisado con arroz", "pollo", "arroz"),
        recipe("arroz-atun", "Arroz con atún", "arroz", "atun"),
        recipe("pasta-verde", "Pasta verde rápida", "pasta", "espinaca"),
    )

    @Test
    fun `CA-RIA-01-04 una búsqueda vacía devuelve todas las recetas`() {
        assertEquals(recipes, search(recipes, ingredients, ""))
        assertEquals(recipes, search(recipes, ingredients, "   "))
    }

    @Test
    fun `CA-RIA-01-04 encuentra por el nombre sin importar mayúsculas`() {
        val found = search(recipes, ingredients, "PASTA")

        assertEquals(listOf("pasta-verde"), found.map { it.id })
    }

    @Test
    fun `CA-RIA-01-04 encuentra por un ingrediente del catálogo`() {
        val found = search(recipes, ingredients, "espinaca")

        assertEquals(listOf("pasta-verde"), found.map { it.id })
    }

    @Test
    fun `CA-RIA-01-04 encuentra por sinónimo del ingrediente`() {
        val found = search(recipes, ingredients, "pechuga")

        assertEquals(listOf("pollo-guisado"), found.map { it.id })
    }

    @Test
    fun `CA-RIA-01-04 no importan las tildes ni en la consulta ni en el catálogo`() {
        assertEquals(listOf("arroz-atun"), search(recipes, ingredients, "atun").map { it.id })
        assertEquals(listOf("arroz-atun"), search(recipes, ingredients, "ATÚN").map { it.id })
    }

    @Test
    fun `CA-RIA-01-04 un texto sin coincidencias no devuelve recetas`() {
        assertTrue(search(recipes, ingredients, "lasaña").isEmpty())
    }

    private fun ingredient(
        id: String,
        name: String,
        synonyms: List<String> = emptyList(),
    ) = Ingredient(
        id = id,
        name = name,
        synonyms = synonyms,
        section = "Otros",
        unit = IngredientUnit.GRAM,
        pack = 1_000.0,
        prices = mapOf(Store.D1 to 10_000L),
    )

    private fun recipe(id: String, name: String, vararg ingredientIds: String) = Recipe(
        id = id,
        name = name,
        servings = 4,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = "Fácil",
        kcal = 500,
        ingredients = ingredientIds.map { RecipeIngredient(it, 100.0) },
        steps = emptyList(),
    )
}
