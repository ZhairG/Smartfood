package com.smartfood.app.feature.recipes.data.catalog

import com.smartfood.app.feature.recipes.domain.model.FoodTag
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.model.StorePrice
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: AssetsRecipeCatalogTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [AssetsRecipeCatalog] (spec 009, plan §4.1, T004, RF-RIA-01): mapeo de las
 * `etiquetas` a [FoodTag] y de las unidades, caché de una sola lectura por archivo y error con una etiqueta
 * desconocida. Usa un [RecipeAssetSource] fake, así que corre en la JVM sin Android.
 */
class AssetsRecipeCatalogTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RF-RIA-01 mapea recetas e ingredientes a los modelos del dominio`() = runTest {
        val catalog = catalog()

        val recipes = catalog.recipes()
        assertEquals(1, recipes.size)
        val recipe = recipes.first()
        assertEquals("pollo-guisado", recipe.id)
        assertEquals("Pollo guisado", recipe.name)
        assertEquals("receta_pollo_guisado", recipe.photo)
        assertEquals(4, recipe.servings)
        assertEquals(15, recipe.prepMinutes)
        assertEquals(25, recipe.cookMinutes)
        assertEquals(5, recipe.restMinutes)
        assertEquals(45, recipe.totalMinutes)
        assertEquals("Media", recipe.difficulty)
        assertTrue(recipe.spicy)
        assertEquals(520, recipe.kcal)
        assertEquals(listOf("pollo" to 600.0, "leche" to 200.0), recipe.ingredients.map { it.ingredientId to it.quantity })
        assertEquals(listOf("Corta el pollo en presas"), recipe.beforeStarting)
        assertEquals("Dora el pollo", recipe.steps.first().title)
        assertEquals(8, recipe.steps.first().minutes)
    }

    @Test
    fun `RF-RIA-01 mapea unidades, precios y etiquetas de los ingredientes`() = runTest {
        val ingredients = catalog().ingredients()

        assertEquals(2, ingredients.size)
        val leche = ingredients.getValue("leche")
        assertEquals("Leche", leche.name)
        assertEquals(listOf("leche entera"), leche.synonyms)
        assertEquals("Lácteos", leche.section)
        assertEquals(IngredientUnit.MILLILITER, leche.unit)
        assertEquals(1_100.0, leche.pack, 0.0)
        assertEquals("1,1 L", leche.packLabel)
        assertEquals(mapOf(Store.EXITO to 3_770L, Store.D1 to 3_780L), leche.prices)
        assertFalse(leche.pantryBasic)
        assertEquals(5, leche.shelfLifeDays)
        assertEquals(setOf(FoodTag.ANIMAL_PRODUCT, FoodTag.LACTOSE), leche.tags)

        val pollo = ingredients.getValue("pollo")
        assertEquals(IngredientUnit.GRAM, pollo.unit)
        assertEquals(setOf(FoodTag.MEAT, FoodTag.ANIMAL_PRODUCT), pollo.tags)
        assertNull(pollo.shelfLifeDays)
        assertTrue(pollo.pantryBasic)
    }

    @Test
    fun `CA-RIA-04-07 el precio es el de la tienda elegida o el más barato de otra tienda`() = runTest {
        val leche = catalog().ingredients().getValue("leche")

        assertEquals(StorePrice(Store.D1, 3_780L), leche.priceAt(Store.D1))
        // Carulla no la tiene en línea: se usa la tienda más barata y se dice cuál es.
        assertEquals(StorePrice(Store.EXITO, 3_770L), leche.priceAt(Store.CARULLA))
    }

    @Test
    fun `RF-RIA-01 una tienda desconocida falla con IllegalArgumentException`() = runTest {
        val catalog = AssetsRecipeCatalog(
            FakeRecipeAssetSource(ingredientes = INGREDIENTES.replace("\"d1\"", "\"ara\"")),
            json,
        )

        val error = runCatching { catalog.ingredients() }.exceptionOrNull()

        assertTrue("Se esperaba IllegalArgumentException y fue $error", error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("ara"))
    }

    @Test
    fun `RF-RIA-01 cada archivo se lee una sola vez y se guarda en caché`() = runTest {
        val source = FakeRecipeAssetSource()
        val catalog = AssetsRecipeCatalog(source, json)

        catalog.recipes()
        catalog.recipes()
        assertEquals(1, source.reads("recetas.json"))
        assertEquals(0, source.reads("ingredientes.json"))

        catalog.ingredients()
        catalog.ingredients()
        assertEquals(1, source.reads("ingredientes.json"))
        assertEquals(1, source.reads("recetas.json"))
    }

    @Test
    fun `RF-RIA-01 una etiqueta desconocida falla con IllegalArgumentException`() = runTest {
        val catalog = AssetsRecipeCatalog(
            FakeRecipeAssetSource(ingredientes = INGREDIENTES.replace("\"lactosa\"", "\"dragon\"")),
            json,
        )

        val error = runCatching { catalog.ingredients() }.exceptionOrNull()

        assertTrue("Se esperaba IllegalArgumentException y fue $error", error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("dragon"))
    }

    @Test
    fun `RF-RIA-01 una unidad desconocida falla con IllegalArgumentException`() = runTest {
        val catalog = AssetsRecipeCatalog(
            FakeRecipeAssetSource(ingredientes = INGREDIENTES.replace("\"ml\"", "\"taza\"")),
            json,
        )

        val error = runCatching { catalog.ingredients() }.exceptionOrNull()

        assertTrue("Se esperaba IllegalArgumentException y fue $error", error is IllegalArgumentException)
        assertTrue(error!!.message!!.contains("taza"))
    }

    private fun catalog() = AssetsRecipeCatalog(FakeRecipeAssetSource(), json)

    /** Fuente fake del catálogo que cuenta cuántas veces se lee cada archivo. */
    private class FakeRecipeAssetSource(
        private val recetas: String = RECETAS,
        private val ingredientes: String = INGREDIENTES,
    ) : RecipeAssetSource {

        private val readCount = mutableMapOf<String, Int>()

        fun reads(name: String): Int = readCount[name] ?: 0

        override fun read(name: String): String {
            readCount[name] = reads(name) + 1
            return when (name) {
                "recetas.json" -> recetas
                "ingredientes.json" -> ingredientes
                else -> error("Asset inesperado: $name")
            }
        }
    }

    private companion object {
        /** Incluye un campo desconocido a propósito: el `Json` de la app debe ignorarlo. */
        val RECETAS = """
            [
              {
                "id": "pollo-guisado",
                "nombre": "Pollo guisado",
                "foto": "receta_pollo_guisado",
                "porciones": 4,
                "prepMin": 15,
                "coccionMin": 25,
                "reposoMin": 5,
                "dificultad": "Media",
                "picante": true,
                "kcal": 520,
                "ingredientes": [
                  { "id": "pollo", "cantidad": 600 },
                  { "id": "leche", "cantidad": 200 }
                ],
                "antesDeEmpezar": ["Corta el pollo en presas"],
                "pasos": [
                  { "titulo": "Dora el pollo", "texto": "Dora el pollo por todos lados.", "minutos": 8 }
                ],
                "campoNuevo": "se ignora"
              }
            ]
        """.trimIndent()

        val INGREDIENTES = """
            [
              {
                "id": "pollo",
                "nombre": "Pollo",
                "sinonimos": ["pechuga"],
                "seccion": "Carnes",
                "unidad": "g",
                "etiquetas": ["carne", "origen_animal"],
                "empaque": {"cantidad": 1000, "etiqueta": "1 kg"},
                "precios": {"olimpica": 9590},
                "basico": true
              },
              {
                "id": "leche",
                "nombre": "Leche",
                "sinonimos": ["leche entera"],
                "seccion": "Lácteos",
                "unidad": "ml",
                "vidaUtilDias": 5,
                "etiquetas": ["origen_animal", "lactosa"],
                "empaque": {"cantidad": 1100, "etiqueta": "1,1 L"},
                "precios": {"exito": 3770, "d1": 3780}
              }
            ]
        """.trimIndent()
    }
}
