package com.smartfood.app.feature.profile.data

import com.smartfood.app.feature.profile.data.repository.PreferencesDocument
import com.smartfood.app.feature.profile.data.repository.preferencesFromDocument
import com.smartfood.app.feature.profile.data.repository.toDocumentFields
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel
import com.smartfood.app.feature.profile.domain.model.Supermarket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Nombre: PreferencesDocumentTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de la conversión entre [FoodPreferences] y el mapa `preferences` de `users/{uid}`
 * (spec 008, CA-PER-03-04, T010): lectura tolerante y escritura sin `null`.
 */
class PreferencesDocumentTest {

    @Test
    fun `CA-PER-03-04 las preferencias guardadas se leen del documento`() {
        val preferencias = preferencesFromDocument(
            mapOf(
                PreferencesDocument.PREFERENCES to mapOf(
                    PreferencesDocument.RESTRICTIONS to listOf("VEGETARIAN", "GLUTEN_FREE"),
                    PreferencesDocument.CUISINES to listOf("COLOMBIAN", "MEDITERRANEAN"),
                    PreferencesDocument.SPICE to "HOT",
                    PreferencesDocument.BUDGET to "ECONOMIC",
                )
            )
        )

        assertEquals(
            FoodPreferences(
                restrictions = setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.GLUTEN_FREE),
                cuisines = setOf(Cuisine.COLOMBIAN, Cuisine.MEDITERRANEAN),
                spice = SpiceLevel.HOT,
                budget = MealBudget.ECONOMIC,
            ),
            preferencias
        )
    }

    @Test
    fun `CA-PER-03-04 sin mapa o con valores desconocidos se devuelven preferencias vacias`() {
        assertEquals(FoodPreferences(), preferencesFromDocument(emptyMap()))
        assertEquals(
            FoodPreferences(),
            preferencesFromDocument(mapOf(PreferencesDocument.PREFERENCES to "ninguna"))
        )

        val raro = preferencesFromDocument(
            mapOf(
                PreferencesDocument.PREFERENCES to mapOf(
                    PreferencesDocument.RESTRICTIONS to listOf("CARNIVORE", 7),
                    PreferencesDocument.CUISINES to "ITALIAN",
                    PreferencesDocument.SPICE to "EXTREME",
                    PreferencesDocument.BUDGET to false,
                )
            )
        )
        assertEquals(FoodPreferences(), raro)
    }

    @Test
    fun `CA-PER-03-04 al guardar se escriben las listas y las claves elegidas`() {
        val campos = FoodPreferences(
            restrictions = setOf(DietaryRestriction.NUT_FREE, DietaryRestriction.VEGAN),
            cuisines = setOf(Cuisine.ITALIAN),
            spice = SpiceLevel.MILD,
            budget = MealBudget.UNLIMITED,
        ).toDocumentFields()

        assertEquals(setOf("restrictions", "cuisines", "spice", "budget"), campos.keys)
        // Orden de los enums, para que el documento no cambie según el orden en que se tocaron los chips.
        assertEquals(listOf("VEGAN", "NUT_FREE"), campos[PreferencesDocument.RESTRICTIONS])
        assertEquals(listOf("ITALIAN"), campos[PreferencesDocument.CUISINES])
        assertEquals("MILD", campos[PreferencesDocument.SPICE])
        assertEquals("UNLIMITED", campos[PreferencesDocument.BUDGET])
    }

    @Test
    fun `CA-PER-03-05 el supermercado se guarda y se lee, y uno desconocido se ignora`() {
        val campos = FoodPreferences(store = Supermarket.OLIMPICA).toDocumentFields()
        assertEquals("OLIMPICA", campos[PreferencesDocument.STORE])

        val leidas = preferencesFromDocument(mapOf(PreferencesDocument.PREFERENCES to campos))
        assertEquals(Supermarket.OLIMPICA, leidas.store)

        val desconocida = preferencesFromDocument(mapOf(PreferencesDocument.PREFERENCES to mapOf("store" to "ARA")))
        assertEquals(null, desconocida.store)
        assertEquals("sin elegir, las recetas usan D1", Supermarket.D1, desconocida.storeOrDefault)
    }

    @Test
    fun `CA-PER-03-04 sin picante ni presupuesto no se escriben esas claves`() {
        val campos = FoodPreferences().toDocumentFields()

        assertEquals(setOf("restrictions", "cuisines"), campos.keys)
        assertFalse(PreferencesDocument.SPICE in campos)
        assertFalse(PreferencesDocument.BUDGET in campos)
        assertFalse(PreferencesDocument.STORE in campos)
        assertEquals(emptyList<String>(), campos[PreferencesDocument.RESTRICTIONS])
        assertEquals(emptyList<String>(), campos[PreferencesDocument.CUISINES])
    }
}
