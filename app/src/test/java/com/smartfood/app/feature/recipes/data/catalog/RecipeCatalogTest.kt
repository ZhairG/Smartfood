package com.smartfood.app.feature.recipes.data.catalog

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: RecipeCatalogTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de contenido del catálogo de recetas (spec 009, T003, RNF-RIA-05, RF-RIA-01). Lee los JSON de
 * `src/main/assets` y comprueba ids únicos, ingredientes existentes, cantidades, pasos, foto de cada receta y su fuente
 * en `docs/licenses/fotos-recetas.md`. No necesita Android: corre en la JVM leyendo los archivos del repositorio.
 */
class RecipeCatalogTest {

    private val mainDir: File = listOf(
        File("src/main"),
        File("app/src/main"),
    ).firstOrNull { it.isDirectory }
        ?: error("No se encontró app/src/main desde ${File("").absolutePath}")

    private val ingredients: List<JsonObject> = readArray(mainDir.resolve("assets/ingredientes.json"))
    private val recipes: List<JsonObject> = readArray(mainDir.resolve("assets/recetas.json"))
    private val photoDir: File = mainDir.resolve("res/drawable-nodpi")
    private val licenses: String = listOf(
        File("../docs/licenses/fotos-recetas.md"),
        File("docs/licenses/fotos-recetas.md"),
    ).firstOrNull { it.isFile }?.readText()
        ?: error("No se encontró docs/licenses/fotos-recetas.md desde ${File("").absolutePath}")

    private val ingredientIds = ingredients.map { it.text("id") }
    private val sectionNames = setOf("Verduras", "Frutas", "Cereales", "Carnes", "Lácteos", "Otros")
    private val unitNames = setOf("g", "ml", "und")
    private val tagNames = setOf("carne", "marisco", "origen_animal", "gluten", "lactosa", "frutos_secos")
    private val difficulties = setOf("Fácil", "Media", "Difícil")

    @Test
    fun `RNF-RIA-05 el catálogo tiene entre 30 y 50 recetas con ids únicos`() {
        assertTrue("Se esperaban al menos 30 recetas y hay ${recipes.size}", recipes.size in 30..50)
        assertEquals("Los ids de receta no pueden repetirse", recipes.size, recipes.map { it.text("id") }.toSet().size)
        assertEquals("Los ids de ingrediente no pueden repetirse", ingredients.size, ingredientIds.toSet().size)
    }

    @Test
    fun `RF-RIA-01 cada ingrediente de las recetas existe y todo el catálogo se usa`() {
        val used = mutableSetOf<String>()
        recipes.forEach { recipe ->
            val ingredientsOfRecipe = recipe.array("ingredientes")
            assertTrue("${recipe.text("id")} no tiene ingredientes", ingredientsOfRecipe.isNotEmpty())
            ingredientsOfRecipe.forEach { item ->
                val ingredientId = item.jsonObject.text("id")
                used += ingredientId
                assertTrue(
                    "${recipe.text("id")} referencia el ingrediente inexistente '$ingredientId'",
                    ingredientId in ingredientIds,
                )
            }
        }
        val unused = ingredientIds.filterNot { it in used }
        assertTrue("Ingredientes sin uso en el catálogo: $unused", unused.isEmpty())
    }

    @Test
    fun `RNF-RIA-05 cantidades, porciones y calorías son positivas`() {
        recipes.forEach { recipe ->
            val id = recipe.text("id")
            assertTrue("$id: porciones debe ser mayor que 0", recipe.int("porciones") > 0)
            assertTrue("$id: kcal debe ser mayor que 0", recipe.int("kcal") > 0)
            assertTrue("$id: prepMin no puede ser negativo", recipe.int("prepMin") >= 0)
            assertTrue("$id: coccionMin no puede ser negativo", recipe.int("coccionMin") >= 0)
            assertTrue("$id: reposoMin no puede ser negativo", recipe.int("reposoMin") >= 0)
            assertTrue("$id: el tiempo total debe ser mayor que 0", recipe.totalMinutes() > 0)
            recipe.array("ingredientes").forEach { item ->
                assertTrue(
                    "$id: la cantidad de '${item.jsonObject.text("id")}' debe ser mayor que 0",
                    item.jsonObject.number("cantidad") > 0,
                )
            }
        }
    }

    @Test
    fun `RNF-RIA-05 las recetas tienen dificultad válida, preparativos y pasos`() {
        recipes.forEach { recipe ->
            val id = recipe.text("id")
            assertTrue("$id: dificultad '${recipe.text("dificultad")}' no es válida", recipe.text("dificultad") in difficulties)
            val before = recipe.array("antesDeEmpezar")
            assertTrue("$id: debe tener entre 1 y 2 preparativos", before.size in 1..2)
            before.forEach { assertTrue("$id: preparativo vacío", it.jsonPrimitive.content.isNotBlank()) }
            val steps = recipe.array("pasos")
            assertTrue("$id: debe tener entre 3 y 5 pasos", steps.size in 3..5)
            steps.forEach { step ->
                val json = step.jsonObject
                assertTrue("$id: paso sin título", json.text("titulo").isNotBlank())
                assertTrue("$id: paso sin texto", json.text("texto").isNotBlank())
                assertTrue("$id: minutos del paso no puede ser negativo", json.optionalInt("minutos") >= 0)
            }
        }
    }

    @Test
    fun `CA-RIA-02-09 los ingredientes tienen unidad, sección, precio y etiquetas válidas`() {
        ingredients.forEach { ingredient ->
            val id = ingredient.text("id")
            assertTrue("$id: unidad inválida", ingredient.text("unidad") in unitNames)
            assertTrue("$id: sección inválida", ingredient.text("seccion") in sectionNames)
            assertTrue("$id: precioCop debe ser mayor que 0", ingredient.long("precioCop") > 0)
            assertTrue("$id: precioPor debe ser mayor que 0", ingredient.number("precioPor") > 0)
            assertTrue("$id: vidaUtilDias debe ser mayor que 0", ingredient.optionalInt("vidaUtilDias") > 0)
            ingredient.array("etiquetas").forEach { tag ->
                val name = tag.jsonPrimitive.content
                assertTrue("$id: etiqueta '$name' desconocida", name in tagNames)
            }
        }
    }

    @Test
    fun `RNF-RIA-05 cada receta tiene su foto webp de menos de 150 KB`() {
        recipes.forEach { recipe ->
            val id = recipe.text("id")
            val photo = recipe.text("foto")
            assertEquals(
                "$id: la foto debe llamarse receta_<id con guiones bajos>",
                "receta_" + id.replace('-', '_'),
                photo,
            )
            val file = photoDir.resolve("$photo.webp")
            assertTrue("$id: falta ${file.path}", file.isFile)
            assertTrue("$id: ${file.name} supera los 150 KB", file.length() <= 150 * 1024)
        }
    }

    @Test
    fun `RNF-RIA-05 ninguna receta repite la foto de otra`() {
        // Compara el contenido de los archivos, no solo el nombre: dos recetas no pueden mostrar la misma imagen.
        val porContenido = recipes.map { it.text("foto") }
            .groupBy { photoDir.resolve("$it.webp").readBytes().contentHashCode() }
            .values
            .filter { it.size > 1 }
        assertTrue("Fotos repetidas: $porContenido", porContenido.isEmpty())
    }

    @Test
    fun `RNF-RIA-05 cada foto está documentada con su fuente y licencia`() {
        recipes.forEach { recipe ->
            val photo = recipe.text("foto")
            val row = licenses.lineSequence().firstOrNull { it.contains("$photo.webp") }
            assertTrue("La foto $photo.webp no aparece en docs/licenses/fotos-recetas.md", row != null)
            assertTrue("La foto $photo.webp no tiene URL de fuente", row!!.contains("http"))
        }
    }

    private companion object {
        fun readArray(file: File): List<JsonObject> = Json.parseToJsonElement(file.readText())
            .jsonArray
            .map { it.jsonObject }

        fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content

        fun JsonObject.number(name: String): Double = getValue(name).jsonPrimitive.content.toDouble()

        fun JsonObject.int(name: String): Int = getValue(name).jsonPrimitive.content.toInt()

        fun JsonObject.long(name: String): Long = getValue(name).jsonPrimitive.content.toLong()

        fun JsonObject.optionalInt(name: String): Int =
            get(name)?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        fun JsonObject.array(name: String): JsonArray = getValue(name).jsonArray

        fun JsonObject.totalMinutes(): Int =
            int("prepMin") + int("coccionMin") + int("reposoMin")
    }
}
