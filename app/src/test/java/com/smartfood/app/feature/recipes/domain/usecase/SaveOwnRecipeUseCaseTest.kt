package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.ownRecipe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: SaveOwnRecipeUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Guardar una receta propia (spec 010, T012): la nueva queda favorita, la editada conserva su id y su fecha
 * de creación, y sin conexión queda pendiente (CA-SAV-03-04, 03-08, 03-09 y 04-02).
 */
class SaveOwnRecipeUseCaseTest {

    private val recipes = FakeOwnRecipesRepository()
    private val favorites = FakeFavoritesRepository()
    private val save = SaveOwnRecipeUseCase(recipes, favorites, ValidateOwnRecipeUseCase())

    private val draft = OwnRecipeDraft(
        name = "  Arepas rellenas  ",
        category = RecipeCategory.AREPAS,
        cookMinutes = "30",
        preparation = "Mezclar y asar.",
    )

    @Test
    fun `CA-SAV-03-09 una receta nueva tambien queda favorita`() = runTest {
        val saved = save(null, draft).getOrThrow()

        assertEquals("Arepas rellenas", saved.recipe.name)
        assertEquals(
            listOf(FakeFavoritesRepository.FavoriteCall(saved.recipe.id, FavoriteSource.OWN, true)),
            favorites.setCalls
        )
    }

    @Test
    fun `CA-SAV-04-02 editar conserva el id y la fecha de creacion y no toca el corazon`() = runTest {
        recipes.recipes.value = listOf(ownRecipe(id = "r1", createdAtMillis = 500L))

        val saved = save("r1", draft).getOrThrow()

        assertEquals("r1", saved.recipe.id)
        assertEquals(500L, saved.recipe.createdAtMillis)
        assertEquals(false, recipes.saveCalls.single().isNew)
        assertTrue(favorites.setCalls.isEmpty())
    }

    @Test
    fun `CA-SAV-03-08 sin conexion la receta queda pendiente`() = runTest {
        recipes.outcome = WriteOutcome.PENDING

        val saved = save(null, draft).getOrThrow()

        assertEquals(WriteOutcome.PENDING, saved.outcome)
        assertTrue(saved.recipe.isPendingSync)
    }

    @Test
    fun `CA-SAV-03-06 un borrador invalido no se guarda`() = runTest {
        val result = save(null, draft.copy(cookMinutes = "0"))

        assertTrue(result.exceptionOrNull() is OwnRecipeException)
        assertTrue(recipes.saveCalls.isEmpty())
    }
}
