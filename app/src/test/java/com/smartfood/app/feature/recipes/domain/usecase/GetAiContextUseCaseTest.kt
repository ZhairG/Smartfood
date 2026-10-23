package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.Store
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
