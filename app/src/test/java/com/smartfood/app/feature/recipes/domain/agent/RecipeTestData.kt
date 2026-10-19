package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import kotlin.random.Random

/**
 * Nombre: RecipeTestData.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Constructores de ingredientes, recetas e inventarios para las pruebas del agente (spec 009, #114), y un
 * generador de catálogos aleatorios con semilla fija para comparar el agente contra la fuerza bruta.
 */

/** Ingrediente de prueba: [priceCop] por cada 1000 unidades base (g o ml) o por unidad. */
fun ingredient(id: String, priceCop: Long = 10_000, unit: IngredientUnit = IngredientUnit.GRAM) = Ingredient(
    id = id,
    name = id.replaceFirstChar { it.uppercase() },
    section = "Otros",
    unit = unit,
    priceCop = priceCop,
    priceQuantity = if (unit == IngredientUnit.UNIT) 1.0 else 1000.0,
)

/** Receta de prueba para 4 porciones; [ingredients] son pares (ingrediente, cantidad). */
fun recipe(
    id: String,
    vararg ingredients: Pair<String, Double>,
    minutes: Int = 30,
    spicy: Boolean = false,
) = Recipe(
    id = id,
    name = id,
    servings = 4,
    prepMinutes = minutes / 2,
    cookMinutes = minutes - minutes / 2,
    difficulty = "Media",
    spicy = spicy,
    kcal = 400,
    ingredients = ingredients.map { (ing, qty) -> RecipeIngredient(ing, qty) },
    steps = listOf(RecipeStep("Paso", "Cocinar")),
)

/** Producto del inventario emparejado con [ingredientId]. */
fun pantry(itemId: String, ingredientId: String, quantity: Double?, daysToExpire: Int) =
    PantryItem(itemId, ingredientId, quantity, daysToExpire)

/**
 * Catálogo y despensa aleatorios con semilla fija.
 *
 * @property ingredients Ingredientes por id.
 * @property recipes Recetas.
 * @property pantry Inventario emparejado.
 */
data class RandomScenario(
    val ingredients: Map<String, Ingredient>,
    val recipes: List<Recipe>,
    val pantry: List<PantryItem>,
)

/** Genera un escenario con [recipeCount] recetas sobre 12 ingredientes; siempre igual para la misma [seed]. */
fun randomScenario(seed: Int, recipeCount: Int): RandomScenario {
    val random = Random(seed)
    val ingredients = (1..12).map { i -> ingredient("ing$i", priceCop = 2_000L + random.nextLong(0, 20_000)) }
        .associateBy { it.id }
    val ids = ingredients.keys.toList()
    val recipes = (1..recipeCount).map { r ->
        val used = ids.shuffled(random).take(random.nextInt(2, 6))
        recipe(
            "r%02d".format(r),
            *used.map { it to (50.0 + random.nextInt(0, 400)) }.toTypedArray(),
            minutes = random.nextInt(15, 70),
            spicy = random.nextInt(4) == 0,
        )
    }
    val pantry = ids.shuffled(random).take(random.nextInt(4, 10)).mapIndexed { i, ing ->
        val quantity = if (random.nextInt(5) == 0) null else 50.0 + random.nextInt(0, 500)
        pantry("p$i", ing, quantity, random.nextInt(-1, 8))
    }
    return RandomScenario(ingredients, recipes, pantry)
}
