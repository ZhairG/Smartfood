package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.recipes.domain.model.FoodTag
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: IngredientMatcherTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de tabla del emparejamiento inventario ↔ catálogo (spec 009, plan §3.1 y §4.2, T005, RF-RIA-02):
 * nombre y sinónimos, el texto más largo, la conversión de unidades (kg, lb, l), las unidades incompatibles, el
 * descarte de productos sin coincidencia y los días para vencer.
 */
class IngredientMatcherTest {

    private val hoy = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(hoy.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val matcher = IngredientMatcher(clock)

    @Test
    fun `RF-RIA-02 Leche Entera coincide con leche y 1 L pasa a 1000 ml`() {
        val ingredients = mapOf("leche" to ingredient("leche", "Leche", unit = IngredientUnit.MILLILITER))

        val match = matcher.match(listOf(item("i1", "Leche Entera", 1.0, QuantityUnit.LITERS)), ingredients)

        assertEquals(1, match.size)
        assertEquals("leche", match.first().ingredientId)
        assertEquals(1000.0, match.first().quantity!!, 0.0)
    }

    @Test
    fun `RF-RIA-02 coincide por sinónimo sin importar mayúsculas ni tildes`() {
        val ingredients = mapOf(
            "pollo" to ingredient("pollo", "Pollo", synonyms = listOf("pechuga")),
        )

        val match = matcher.match(listOf(item("i1", "PECHUGA DE POLLO", 500.0, QuantityUnit.GRAMS)), ingredients)

        assertEquals("pollo", match.single().ingredientId)
        assertEquals(500.0, match.single().quantity!!, 0.0)
    }

    @Test
    fun `RF-RIA-02 convierte gramos, libras, kilogramos y unidades`() {
        val ingredients = mapOf(
            "arroz" to ingredient("arroz", "Arroz"),
            "carne" to ingredient("carne", "Carne de res", synonyms = listOf("carne")),
            "sal" to ingredient("sal", "Sal"),
            "huevo" to ingredient("huevo", "Huevo", unit = IngredientUnit.UNIT),
        )

        val match = matcher.match(
            listOf(
                item("a1", "Arroz", 500.0, QuantityUnit.GRAMS),
                item("c1", "Carne", 1.0, QuantityUnit.POUNDS),
                item("s1", "Sal", 1.0, QuantityUnit.KILOGRAMS),
                item("h1", "Huevos", 6.0, QuantityUnit.UNITS),
            ),
            ingredients,
        ).associateBy { it.itemId }

        assertEquals(500.0, match.getValue("a1").quantity!!, 0.0)
        assertEquals(453.59237, match.getValue("c1").quantity!!, 0.00001)
        assertEquals(1000.0, match.getValue("s1").quantity!!, 0.0)
        assertEquals(6.0, match.getValue("h1").quantity!!, 0.0)
    }

    @Test
    fun `RF-RIA-02 con unidades incompatibles la cantidad queda nula`() {
        val ingredients = mapOf(
            "leche" to ingredient("leche", "Leche", unit = IngredientUnit.MILLILITER),
            "sal" to ingredient("sal", "Sal"),
        )

        val match = matcher.match(
            listOf(
                item("i1", "Leche", 1.0, QuantityUnit.UNITS),
                item("i2", "Sal", 1.0, QuantityUnit.LITERS),
            ),
            ingredients,
        )

        assertEquals(2, match.size)
        match.forEach { assertNull("${it.ingredientId} debería quedar sin cantidad", it.quantity) }
    }

    @Test
    fun `RF-RIA-02 sin cantidad en el inventario la cantidad queda nula`() {
        val ingredients = mapOf("arroz" to ingredient("arroz", "Arroz"))

        val match = matcher.match(listOf(item("i1", "Arroz", null, null)), ingredients)

        assertEquals("arroz", match.single().ingredientId)
        assertNull(match.single().quantity)
    }

    @Test
    fun `RF-RIA-02 gana el nombre más largo contenido en el producto`() {
        val ingredients = mapOf(
            "leche" to ingredient("leche", "Leche"),
            "coco" to ingredient("coco", "Coco"),
            "leche-de-coco" to ingredient("leche-de-coco", "Leche de coco"),
        )

        val match = matcher.match(
            listOf(
                item("i1", "Leche de coco", 400.0, QuantityUnit.MILLILITERS),
                item("i2", "Leche de vaca", 1.0, QuantityUnit.LITERS),
            ),
            ingredients,
        ).associateBy { it.itemId }

        assertEquals("leche-de-coco", match.getValue("i1").ingredientId)
        assertEquals("leche", match.getValue("i2").ingredientId)
    }

    @Test
    fun `RF-RIA-02 empate de texto se resuelve por id de ingrediente`() {
        val ingredients = mapOf(
            "a-sal" to ingredient("a-sal", "Sal", unit = IngredientUnit.UNIT),
            "b-sal" to ingredient("b-sal", "Sal"),
        )

        val match = matcher.match(listOf(item("i1", "Sal", 500.0, QuantityUnit.GRAMS)), ingredients)

        assertEquals("a-sal", match.single().ingredientId)
    }

    @Test
    fun `RF-RIA-02 los productos sin coincidencia se omiten`() {
        val ingredients = mapOf("arroz" to ingredient("arroz", "Arroz"))

        val match = matcher.match(
            listOf(
                item("i1", "Jabón de loza", 1.0, QuantityUnit.UNITS),
                item("i2", "Arroz", 500.0, QuantityUnit.GRAMS),
            ),
            ingredients,
        )

        assertEquals(listOf("i2"), match.map { it.itemId })
    }

    @Test
    fun `RF-RIA-02 calcula los días para vencer`() {
        val ingredients = mapOf("arroz" to ingredient("arroz", "Arroz"))

        val match = matcher.match(
            listOf(
                item("i1", "Arroz", null, null, expiresOn = hoy.plusDays(3)),
                item("i2", "Arroz", null, null, expiresOn = hoy),
                item("i3", "Arroz", null, null, expiresOn = hoy.minusDays(2)),
            ),
            ingredients,
        )

        assertEquals(3, match[0].daysToExpire)
        assertEquals(0, match[1].daysToExpire)
        assertEquals(-2, match[2].daysToExpire)
    }

    @Test
    fun `RF-RIA-02 un catálogo vacío no produce coincidencias`() {
        assertTrue(matcher.match(listOf(item("i1", "Arroz", null, null)), emptyMap()).isEmpty())
    }

    private fun ingredient(
        id: String,
        name: String,
        synonyms: List<String> = emptyList(),
        unit: IngredientUnit = IngredientUnit.GRAM,
    ) = Ingredient(
        id = id,
        name = name,
        synonyms = synonyms,
        section = "Carnes",
        unit = unit,
        priceCop = 10_000,
        priceQuantity = 1_000.0,
        tags = setOf(FoodTag.MEAT),
    )

    private fun item(
        id: String,
        name: String,
        amount: Double?,
        unit: QuantityUnit?,
        expiresOn: LocalDate = hoy.plusDays(5),
    ) = InventoryItem(
        id = id,
        name = name,
        category = FoodCategory.OTHER,
        expiresOn = expiresOn,
        quantity = if (amount != null && unit != null) Quantity(amount, unit) else null,
    )
}
