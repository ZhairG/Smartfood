package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.data.catalog.AssetsRecipeCatalog
import com.smartfood.app.feature.recipes.data.catalog.RecipeAssetSource
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.Recipe
import java.io.File
import kotlin.math.round
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Nombre: RealCatalog.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Catálogo real de `src/main/assets` para las pruebas del agente en la JVM (spec 009, T013 y T037), y los
 * inventarios sintéticos de la evaluación del Avance 2: cada uno parte de una receta al azar, de la que toma de 1 a 3
 * ingredientes que vencen en 3 días o menos, y suma productos que vencen más tarde. Con semilla fija, siempre son los
 * mismos.
 */
internal object RealCatalog {

    private val assetsDir: File = listOf(
        File("src/main/assets"),
        File("app/src/main/assets"),
    ).firstOrNull { it.isDirectory }
        ?: error("No se encontró app/src/main/assets desde ${File("").absolutePath}")

    private val catalog = AssetsRecipeCatalog(
        source = object : RecipeAssetSource {
            override fun read(name: String): String = assetsDir.resolve(name).readText()
        },
        json = Json { ignoreUnknownKeys = true },
    )

    val recipes: List<Recipe> = runBlocking { catalog.recipes() }

    val ingredients: Map<String, Ingredient> = runBlocking { catalog.ingredients() }

    /** Ingredientes que se compran (no básicos) y que alguna receta usa. */
    private val shoppable: List<String> = recipes
        .flatMap { recipe -> recipe.ingredients.map { it.ingredientId } }
        .distinct()
        .filter { !ingredients.getValue(it).pantryBasic }
        .sorted()

    /**
     * [count] inventarios sintéticos con la semilla [seed]. En 3 de cada 10, aproximadamente, se agrega otro producto por
     * vencer al azar, que quizás ninguna receta usa junto con los demás.
     */
    fun syntheticInventories(count: Int = 30, seed: Int = 2026): List<List<PantryItem>> {
        val random = Random(seed)
        return List(count) { n ->
            val base = recipes[random.nextInt(recipes.size)]
            val fromBase = base.ingredients.map { it.ingredientId }
                .filter { !ingredients.getValue(it).pantryBasic }
                .shuffled(random)
            val expiring = fromBase.take(1 + random.nextInt(minOf(3, fromBase.size)))
            val extraExpiring = if (random.nextDouble() < 0.3) {
                listOf(shoppable.filterNot { it in expiring }.random(random))
            } else {
                emptyList()
            }
            val later = shoppable.filterNot { it in expiring || it in extraExpiring }
                .shuffled(random)
                .take(4 + random.nextInt(5))
            (expiring + extraExpiring).map { item(n, it, random.nextInt(0, 4), random) } +
                later.map { item(n, it, random.nextInt(4, 21), random) }
        }
    }

    private fun item(scenario: Int, ingredientId: String, daysToExpire: Int, random: Random): PantryItem {
        val pack = ingredients.getValue(ingredientId).pack
        val quantity = round(pack * random.nextDouble(0.3, 1.0) * 10) / 10
        return PantryItem("s$scenario-$ingredientId", ingredientId, quantity, daysToExpire)
    }
}
