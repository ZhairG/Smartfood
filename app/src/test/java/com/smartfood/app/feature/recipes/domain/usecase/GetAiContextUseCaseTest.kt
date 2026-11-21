package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.Store
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: GetAiContextUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [GetAiContextUseCase] (spec 009, T014): el contexto lleva solo los productos que usan las
 * recetas, del que vence antes al que vence después (CA-RIA-02-01), y las restricciones del Perfil como reglas del
 * agente (CA-RIA-02-09).
 */
class GetAiContextUseCaseTest {

    @Test
    fun `CA-RIA-02-01 solo trae los productos que usan las recetas, del que vence antes al que vence despues`() = runTest {
        val context = AiFixtures().getAiContext()

        assertEquals(listOf("Pollo", "Tomate", "Cebolla", "Papa", "Zanahoria", "Ajo", "Arroz"), context.items.map { it.name })
        assertTrue("Un producto que ninguna receta usa no se emparejó", context.pantry.none { it.itemId == "jabon" })
        assertEquals(Store.D1, context.store)
    }

    @Test
    fun `CA-RIA-02-09 las restricciones del Perfil llegan al agente como reglas`() = runTest {
        val context = AiFixtures(setOf(DietaryRestriction.VEGAN, DietaryRestriction.GLUTEN_FREE)).getAiContext()

        assertEquals(setOf(DietaryRestriction.VEGAN, DietaryRestriction.GLUTEN_FREE), context.restrictions)
        assertEquals(setOf(DietRule.VEGAN, DietRule.GLUTEN_FREE), context.diet)
    }

    @Test
    fun `CA-COC-02-02 las cocinas del Perfil llegan al contexto`() = runTest {
        val fixtures = AiFixtures()
        fixtures.profile.preferences = FoodPreferences(cuisines = setOf(Cuisine.ITALIAN, Cuisine.ASIAN))

        assertEquals(setOf(Cuisine.ITALIAN, Cuisine.ASIAN), fixtures.getAiContext().cuisines)
    }

    @Test
    fun `CA-COC-03-04 si las preferencias no se leen las cocinas quedan desconocidas, no vacias`() = runTest {
        val fixtures = AiFixtures()
        fixtures.profile.preferences = FoodPreferences(cuisines = setOf(Cuisine.ITALIAN))
        fixtures.profile.getPreferencesResult = Result.failure(IllegalStateException("sin red"))

        val context = fixtures.getAiContext()

        assertTrue(context.cuisines.isEmpty())
        assertFalse("Sin leer el Perfil no se sabe si hay cocinas", context.preferencesKnown)
    }

    @Test
    fun `CA-RIA-02-12 un perfil VEGAN con la lectura fallida no pasa por sin restricciones`() = runTest {
        val fixtures = AiFixtures(setOf(DietaryRestriction.VEGAN))
        fixtures.profile.getPreferencesResult = Result.failure(IllegalStateException("sin red"))

        val context = fixtures.getAiContext()

        assertFalse("El contexto debe decir que el Perfil es desconocido", context.preferencesKnown)
        assertTrue("El inventario se sigue mostrando", context.items.isNotEmpty())
        assertEquals(Store.D1, context.store)
    }

    @Test
    fun `CA-RIA-02-12 con el perfil leido las preferencias son conocidas, aunque no haya restricciones`() = runTest {
        val context = AiFixtures().getAiContext()

        assertTrue(context.restrictions.isEmpty())
        assertTrue(context.preferencesKnown)
    }
}
