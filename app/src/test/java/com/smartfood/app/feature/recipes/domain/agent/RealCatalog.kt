package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.data.catalog.AssetsRecipeCatalog
import com.smartfood.app.feature.recipes.data.catalog.RecipeAssetSource
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.Store
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

    /**
     * Un usuario de la evaluación: sus inventarios y sus datos del plan.
     *
     * @property name Nombre del perfil en la tabla.
     * @property context Presupuesto, porciones y restricciones.
     * @property inventories Inventarios de prueba.
     */
    data class EvaluationProfile(val name: String, val context: PlanContext, val inventories: List<List<PantryItem>>)

    /**
     * Usuarios con características diferentes, reproducibles con [seed] (spec 009, RNF-RIA-03). No cambia
     * [syntheticInventories], para que los resultados de `AgentEvaluationTest` se mantengan.
     */
    fun evaluationProfiles(count: Int = 6, seed: Int = 2026): List<EvaluationProfile> {
        val base = PlanContext(budgetCop = 100_000, servings = 4, store = Store.D1)
        val normal = syntheticInventories(count, seed)
        return listOf(
            EvaluationProfile("Sin restricciones", base, normal),
            EvaluationProfile("Vegetariano", base.copy(diet = setOf(DietRule.VEGETARIAN)), normal),
            EvaluationProfile("Vegano sin lactosa", base.copy(diet = setOf(DietRule.VEGAN, DietRule.LACTOSE_FREE)), normal),
            EvaluationProfile("Alérgico: sin gluten ni frutos secos", base.copy(diet = setOf(DietRule.GLUTEN_FREE, DietRule.NUT_FREE)), normal),
            EvaluationProfile("Poco presupuesto (COP 25.000, 2 porciones)", base.copy(budgetCop = 25_000, servings = 2), normal),
            EvaluationProfile("Poco tiempo (30 min)", base.copy(maxMinutes = 30), normal),
            EvaluationProfile("Inventario enorme", base, largeInventories(count, seed)),
            EvaluationProfile("Nada por vencer", base, normal.map { pantry -> pantry.map { it.copy(daysToExpire = it.daysToExpire + 10) } }),
            EvaluationProfile("Sin plan posible (COP 2.000)", base.copy(budgetCop = 2_000), normal),
        )
    }

    /** Todos los ingredientes que se compran, con 8 productos por vencer. */
    private fun largeInventories(count: Int, seed: Int): List<List<PantryItem>> {
        val random = Random(seed + 1)
        return List(count) { n ->
            val all = shoppable.shuffled(random)
            all.take(8).map { item(n, it, random.nextInt(0, 4), random) } +
                all.drop(8).map { item(n, it, random.nextInt(4, 21), random) }
        }
    }

    private fun item(scenario: Int, ingredientId: String, daysToExpire: Int, random: Random): PantryItem {
        val pack = ingredients.getValue(ingredientId).pack
        val quantity = round(pack * random.nextDouble(0.3, 1.0) * 10) / 10
        return PantryItem("s$scenario-$ingredientId", ingredientId, quantity, daysToExpire)
    }
}
