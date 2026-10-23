package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Test
import org.junit.Assert.assertFalse

/**
 * Nombre: RecipeEvaluatorTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [RecipeEvaluator] (spec 009 §2, plan §3.2, T010): uso del inventario, faltantes,
 * costo, rescate ponderado por urgencia, desperdicio y la monotonía que necesita la poda del agente.
 */
class RecipeEvaluatorTest {

    private val ingredients = listOf(
        ingredient("pollo", priceCop = 14_000),
        ingredient("arroz", priceCop = 6_000),
        ingredient("tomate", priceCop = 1_000, unit = IngredientUnit.UNIT),
    ).associateBy { it.id }
    private val evaluator = RecipeEvaluator(ingredients)
    private val guiso = recipe("guiso", "pollo" to 200.0, "arroz" to 300.0, "tomate" to 3.0)

    @Test
    fun `CA-RIA-02-05 sin inventario todo se compra en empaques completos`() {
        val e = evaluator.evaluate(guiso, servings = 8, day = 1, stock = PantryStock.of(emptyList()))

        // 8 porciones = el doble: 400 g de pollo (1 empaque de 1 kg, 14.000), 600 g de arroz (1 empaque de 1 kg, 6.000)
        // y 6 tomates (6 unidades de 1.000).
        assertEquals(26_000L, e.costCop)
        assertEquals(listOf(1, 1, 6), e.uses.map { it.packs })
        assertEquals(listOf(Store.D1, Store.D1, Store.D1), e.uses.map { it.store })
        assertEquals(3, e.missingCount)
        assertEquals(0.0, e.pantryShare, 1e-9)
        assertEquals(0.0, e.rescue, 1e-9)
    }

    @Test
    fun `CA-RIA-02-06 usa primero el producto que vence antes y solo compra lo que falta`() {
        val stock = PantryStock.of(
            listOf(pantry("p-lejos", "pollo", 500.0, 6), pantry("p-cerca", "pollo", 120.0, 1), pantry("a", "arroz", 1000.0, 30))
        )
        val e = evaluator.evaluate(guiso, servings = 4, day = 1, stock = stock)

        val pollo = e.uses.first { it.ingredientId == "pollo" }
        assertEquals(mapOf<String, Double?>("p-cerca" to 120.0, "p-lejos" to 80.0), pollo.fromPantry)
        assertEquals(0.0, pollo.toBuy, 1e-9)
        assertEquals(listOf("p-cerca"), pollo.rescuedItems)
        // Solo faltan los 3 tomates.
        assertEquals(3_000L, e.costCop)
        assertEquals(1, e.missingCount)
        assertEquals(2.0 / 3, e.pantryShare, 1e-9)
        // Rescate: usa todo el pollo que vence en 1 día → 1 · 1/(1+1).
        assertEquals(0.5, e.rescue, 1e-9)
        assertEquals(420.0, e.remaining.remainingOf("p-lejos")!!, 1e-9)
    }

    @Test
    fun `CA-RIA-02-05 un producto sin cantidad alcanza para la receta y se usa completo`() {
        val stock = PantryStock.of(listOf(pantry("bolsa", "arroz", null, 2)))
        val e = evaluator.evaluate(guiso, servings = 4, day = 1, stock = stock)

        val arroz = e.uses.first { it.ingredientId == "arroz" }
        assertEquals(mapOf<String, Double?>("bolsa" to null), arroz.fromPantry)
        assertEquals(0.0, arroz.toBuy, 1e-9)
        assertTrue("ya no está disponible", !e.remaining.isAvailable("bolsa"))
        assertEquals(1.0 / 3, e.rescue, 1e-9)
    }

    @Test
    fun `RNF-RIA-01 los productos vencidos o que vencen antes del dia de la receta no se usan`() {
        val stock = PantryStock.of(listOf(pantry("vencido", "pollo", 500.0, -1), pantry("manana", "arroz", 500.0, 1)))

        val dia1 = evaluator.evaluate(guiso, servings = 4, day = 1, stock = stock)
        val dia3 = evaluator.evaluate(guiso, servings = 4, day = 3, stock = stock)

        assertTrue("el pollo vencido se compra", dia1.uses.first { it.ingredientId == "pollo" }.isMissing)
        assertTrue("el día 1 usa el arroz", !dia1.uses.first { it.ingredientId == "arroz" }.isMissing)
        assertTrue("el día 3 el arroz ya venció", dia3.uses.first { it.ingredientId == "arroz" }.isMissing)
    }

    @Test
    fun `CA-RIA-02-04 el desperdicio cuenta la fraccion sin usar de lo que vence en 3 dias o menos`() {
        val stock = PantryStock.of(
            listOf(pantry("a", "arroz", 400.0, 2), pantry("t", "tomate", null, 3), pantry("lejos", "pollo", 100.0, 9))
        )
        val after = evaluator.evaluate(recipe("solo-arroz", "arroz" to 100.0), 4, 1, stock).remaining

        assertEquals(2.0, evaluator.waste(stock), 1e-9)
        assertEquals(0.75 + 1.0, evaluator.waste(after), 1e-9)
        assertEquals(listOf("a", "t"), evaluator.wastedItems(after))
    }

    @Test
    fun `CA-RIA-02-05 lo que sobra de un empaque sirve los días siguientes sin volver a pagarlo`() {
        val dia1 = evaluator.evaluate(guiso, servings = 4, day = 1, stock = PantryStock.of(emptyList()))
        // 200 g de pollo y 300 g de arroz: sobran 800 g y 700 g.
        assertEquals(800.0, dia1.remaining.purchasedOf("pollo"), 1e-9)

        val dia2 = evaluator.evaluate(guiso, servings = 4, day = 2, stock = dia1.remaining)

        val pollo = dia2.uses.first { it.ingredientId == "pollo" }
        assertEquals(200.0, pollo.fromPurchase, 1e-9)
        assertFalse(pollo.isMissing)
        // Solo se vuelven a comprar los 3 tomates.
        assertEquals(3_000L, dia2.costCop)
        assertEquals(1, dia2.missingCount)
        assertEquals("lo comprado no cuenta como inventario", 0.0, dia2.pantryShare, 1e-9)
    }

    @Test
    fun `CA-RIA-02-05 los básicos de despensa no se cobran ni cuentan como faltantes`() {
        val conSal = ingredients + ("sal" to ingredient("sal", 2_900).copy(pantryBasic = true))
        val receta = recipe("arroz-con-sal", "arroz" to 300.0, "sal" to 5.0)

        val e = RecipeEvaluator(conSal).evaluate(receta, 4, 1, PantryStock.of(emptyList()))

        val sal = e.uses.first { it.ingredientId == "sal" }
        assertFalse(sal.isMissing)
        assertEquals(0L, sal.buyCostCop)
        assertEquals(6_000L, e.costCop)
        assertEquals(1, e.missingCount)
    }

    @Test
    fun `CA-RIA-04-07 usa el precio de la tienda elegida o el más barato de otra tienda`() {
        val tiendas = mapOf(
            "pollo" to ingredient("pollo").copy(prices = mapOf(Store.EXITO to 22_000L, Store.D1 to 14_500L)),
            "arroz" to ingredient("arroz").copy(prices = mapOf(Store.OLIMPICA to 4_200L, Store.CARULLA to 4_500L)),
        )
        val receta = recipe("simple", "pollo" to 200.0, "arroz" to 300.0)

        val e = RecipeEvaluator(tiendas).evaluate(receta, 4, 1, PantryStock.of(emptyList()), Store.EXITO)

        assertEquals(listOf(Store.EXITO, Store.OLIMPICA), e.uses.map { it.store })
        assertEquals(22_000L + 4_200L, e.costCop)
    }

    @Test
    fun `CA-RIA-02-04 con menos inventario el rescate no sube y el costo nunca es negativo`() {
        // Propiedades que hacen válidas la poda y el forward checking del agente: el sobrante de un empaque puede abaratar
        // otra receta, pero el rescate solo baja.
        repeat(20) { seed ->
            val s = randomScenario(seed, recipeCount = 6)
            val ev = RecipeEvaluator(s.ingredients)
            val full = PantryStock.of(s.pantry)
            val reduced = ev.evaluate(s.recipes[0], 4, 1, full).remaining
            s.recipes.drop(1).forEach { r ->
                val a = ev.evaluate(r, 4, 1, full)
                val b = ev.evaluate(r, 4, 1, reduced)
                assertTrue("costo $seed/${r.id}", b.costCop >= 0)
                assertTrue("rescate $seed/${r.id}", b.rescue <= a.rescue + 1e-9)
            }
        }
    }
}
