package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.FoodTag
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import kotlin.random.Random

/**
 * Nombre: RecipeTestData.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Constructores de ingredientes, recetas e inventarios para las pruebas del agente (spec 009, #114), y un
 * generador de catálogos aleatorios con semilla fija para comparar el agente contra la fuerza bruta.
 */

/** Ingrediente de prueba: [priceCop] por cada 1000 unidades base (g o ml) o por unidad, con sus [tags]. */
fun ingredient(
    id: String,
    priceCop: Long = 10_000,
    unit: IngredientUnit = IngredientUnit.GRAM,
    vararg tags: FoodTag,
) = Ingredient(
    id = id,
    name = id.replaceFirstChar { it.uppercase() },
    section = "Otros",
    unit = unit,
    pack = if (unit == IngredientUnit.UNIT) 1.0 else 1000.0,
    prices = mapOf(Store.D1 to priceCop),
    tags = tags.toSet(),
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
        // Etiquetas, tiendas y básicos fijos por número (no consumen la semilla): carne, gluten y lácteos en algunos
        // ingredientes; Éxito con otro precio y sin algunos productos; el ingrediente 12 es básico de despensa.
        .map {
            val n = it.id.removePrefix("ing").toInt()
            val d1 = it.prices.getValue(Store.D1)
            it.copy(
                tags = RANDOM_TAGS[n % RANDOM_TAGS.size],
                prices = if (n % 3 == 0) mapOf(Store.D1 to d1) else mapOf(Store.D1 to d1, Store.EXITO to d1 * (8 + n % 5) / 10),
                pantryBasic = n == 12,
            )
        }
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

/**
 * Escenario aleatorio que fuerza los casos difíciles del agente: recetas con más de 3 ingredientes que no están en casa
 * (pueden entrar otro día gracias al sobrante de un empaque) y varios productos del mismo ingrediente que vencen pronto,
 * con cantidades distintas (el rescate de una receta puede subir de un día a otro). Siempre igual para la misma [seed].
 */
fun randomScenarioExigente(seed: Int): RandomScenario {
    val random = Random(seed + 10_000)
    val ingredients = (1..8).map { i -> ingredient("ing$i", priceCop = 2_000L + random.nextLong(0, 15_000)) }
        .associateBy { it.id }
    val ids = ingredients.keys.toList()
    val recipes = (1..random.nextInt(5, 9)).map { r ->
        val used = ids.shuffled(random).take(random.nextInt(2, 6))
        recipe("r%02d".format(r), *used.map { it to (50.0 + random.nextInt(0, 400)) }.toTypedArray())
    }
    // Solo los 4 primeros ingredientes tienen productos en casa, así que se repiten y al resto le faltan cosas.
    val pantry = (0 until random.nextInt(2, 7)).map { i ->
        val quantity = if (random.nextInt(6) == 0) null else 50.0 + random.nextInt(0, 900)
        pantry("p$i", ids[random.nextInt(0, 4)], quantity, random.nextInt(0, 4))
    }
    return RandomScenario(ingredients, recipes, pantry)
}

/**
 * Escenario pequeño con varios productos de los mismos 2 ingredientes, con cantidades muy distintas: el rescate de una
 * receta puede subir si otra se lleva antes un producto. Con él se encontraron las semillas 3598 y 9545, en las que la
 * cota anterior podaba el mejor plan. Siempre igual para la misma [seed].
 */
fun randomScenarioRepetidos(seed: Int): RandomScenario {
    val random = Random(seed)
    val ingredients = (1..5).map { i -> ingredient("ing$i", priceCop = 1_000L + random.nextLong(0, 30_000)) }
        .associateBy { it.id }
    val ids = ingredients.keys.toList()
    val recipes = (1..random.nextInt(4, 7)).map { r ->
        val used = ids.shuffled(random).take(random.nextInt(1, 4))
        recipe("r%02d".format(r), *used.map { it to (20.0 + random.nextInt(0, 1200)) }.toTypedArray())
    }
    val pantry = (0 until random.nextInt(2, 6)).map { i ->
        val quantity = if (random.nextInt(8) == 0) null else 20.0 + random.nextInt(0, 1500)
        pantry("p$i", ids[random.nextInt(0, 2)], quantity, random.nextInt(0, 4))
    }
    return RandomScenario(ingredients, recipes, pantry)
}

/** Etiquetas de los ingredientes aleatorios según su número: uno de cada cuatro sin etiquetas. */
private val RANDOM_TAGS = listOf(
    emptySet(),
    setOf(FoodTag.MEAT, FoodTag.ANIMAL_PRODUCT),
    setOf(FoodTag.GLUTEN),
    setOf(FoodTag.LACTOSE, FoodTag.ANIMAL_PRODUCT),
)
