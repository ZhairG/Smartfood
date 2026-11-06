package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ValidateOwnRecipeUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Cada límite de CA-SAV-03-05 (spec 010, T011) y el orden en que se informa el primer campo inválido.
 */
class ValidateOwnRecipeUseCaseTest {

    private val validate = ValidateOwnRecipeUseCase()

    private val valido = OwnRecipeDraft(
        name = "Arepas",
        category = RecipeCategory.AREPAS,
        cookMinutes = "30",
        preparation = "Mezclar y asar.",
    )

    private fun campoInvalido(draft: OwnRecipeDraft): OwnRecipeField? {
        val error = validate(draft).exceptionOrNull() as? OwnRecipeException ?: return null
        return (error.error as OwnRecipeError.Invalid).field
    }

    private fun ingredientes(cantidad: Int) = List(cantidad) { RecipeIngredient("ing-$it", 1.0) }

    @Test
    fun `CA-SAV-03-05 un borrador completo es valido`() {
        assertTrue(validate(valido).isSuccess)
    }

    @Test
    fun `CA-SAV-03-05 el nombre tiene de 2 a 60 caracteres`() {
        assertEquals(OwnRecipeField.NAME, campoInvalido(valido.copy(name = "A")))
        assertEquals(null, campoInvalido(valido.copy(name = "Ab")))
        assertEquals(null, campoInvalido(valido.copy(name = "a".repeat(60))))
        assertEquals(OwnRecipeField.NAME, campoInvalido(valido.copy(name = "a".repeat(61))))
    }

    @Test
    fun `CA-SAV-03-05 los espacios de los extremos no cuentan`() {
        assertEquals(null, campoInvalido(valido.copy(name = " Arepas ")))
        assertEquals(OwnRecipeField.NAME, campoInvalido(valido.copy(name = "   A   ")))
    }

    @Test
    fun `CA-SAV-03-05 la categoria es obligatoria`() {
        assertEquals(OwnRecipeField.CATEGORY, campoInvalido(valido.copy(category = null)))
    }

    @Test
    fun `CA-SAV-03-05 el tiempo es un entero de 1 a 240 minutos`() {
        assertEquals(OwnRecipeField.COOK_MINUTES, campoInvalido(valido.copy(cookMinutes = "0")))
        assertEquals(null, campoInvalido(valido.copy(cookMinutes = "1")))
        assertEquals(null, campoInvalido(valido.copy(cookMinutes = "240")))
        assertEquals(OwnRecipeField.COOK_MINUTES, campoInvalido(valido.copy(cookMinutes = "241")))
        assertEquals(OwnRecipeField.COOK_MINUTES, campoInvalido(valido.copy(cookMinutes = "")))
    }

    @Test
    fun `CA-SAV-03-05 la preparacion es obligatoria y tiene hasta 2000 caracteres`() {
        assertEquals(OwnRecipeField.PREPARATION, campoInvalido(valido.copy(preparation = "  ")))
        assertEquals(null, campoInvalido(valido.copy(preparation = "a".repeat(2_000))))
        assertEquals(OwnRecipeField.PREPARATION, campoInvalido(valido.copy(preparation = "a".repeat(2_001))))
    }

    @Test
    fun `CA-SAV-03-05 los ingredientes son opcionales, hasta 20, sin repetir y con cantidad`() {
        assertEquals(null, campoInvalido(valido.copy(ingredients = emptyList())))
        assertEquals(null, campoInvalido(valido.copy(ingredients = ingredientes(20))))
        assertEquals(OwnRecipeField.INGREDIENTS, campoInvalido(valido.copy(ingredients = ingredientes(21))))
        val repetidos = listOf(RecipeIngredient("arroz", 1.0), RecipeIngredient("arroz", 2.0))
        assertEquals(OwnRecipeField.INGREDIENTS, campoInvalido(valido.copy(ingredients = repetidos)))
        val sinCantidad = listOf(RecipeIngredient("arroz", 0.0))
        assertEquals(OwnRecipeField.INGREDIENTS, campoInvalido(valido.copy(ingredients = sinCantidad)))
    }

    @Test
    fun `CA-SAV-03-06 informa primero el campo que esta mas arriba`() {
        assertEquals(OwnRecipeField.NAME, campoInvalido(OwnRecipeDraft()))
        assertEquals(OwnRecipeField.CATEGORY, campoInvalido(OwnRecipeDraft(name = "Arepas")))
    }
}
