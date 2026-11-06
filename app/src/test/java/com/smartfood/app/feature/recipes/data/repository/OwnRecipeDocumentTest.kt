package com.smartfood.app.feature.recipes.data.repository

import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.testutil.ownRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Nombre: OwnRecipeDocumentTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Prueba la conversión Map ↔ receta propia (spec 010, T009, RF-SAV-02).
 */
class OwnRecipeDocumentTest {
    @Test
    fun `T009 el documento conserva campos y normaliza el nombre`() {
        val recipe = ownRecipe(name = "  Arroz con Ánimo  ", category = RecipeCategory.ARROCES)
        val fields = recipe.toDocumentFields()

        assertEquals("  Arroz con Ánimo  ", fields[OwnRecipeDocument.NAME])
        assertEquals("arroz con animo", fields[OwnRecipeDocument.NAME_NORMALIZED])
        assertEquals("ARROCES", fields[OwnRecipeDocument.CATEGORY])
        assertEquals(30, fields[OwnRecipeDocument.COOK_MINUTES])
    }

    @Test
    fun `T009 el documento se convierte a receta propia`() {
        val recipe = ownRecipe(ingredients = listOf(RecipeIngredient("arroz", 200.0)))
        val fields = recipe.toDocumentFields() + mapOf(
            OwnRecipeDocument.CREATED_AT to com.google.firebase.Timestamp(java.util.Date(recipe.createdAtMillis)),
            OwnRecipeDocument.UPDATED_AT to com.google.firebase.Timestamp(java.util.Date(recipe.updatedAtMillis)),
        )

        val decoded = ownRecipeFromDocument(recipe.id, fields)

        assertNotNull(decoded)
        assertEquals(recipe, decoded)
    }
}
