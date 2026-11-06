package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.testutil.EmptyRecipeCatalog
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.ownRecipe
import com.smartfood.app.testutil.savedFavorite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: DeleteOwnRecipeUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Eliminar y deshacer (spec 010, T017, CA-SAV-05-03 y 05-04) y el contador del Perfil (T020, CA-SAV-06-01 y
 * 06-02).
 */
class DeleteOwnRecipeUseCaseTest {

    private val recipes = FakeOwnRecipesRepository()
    private val favorites = FakeFavoritesRepository()
    private val delete = DeleteOwnRecipeUseCase(recipes, favorites)
    private val restore = RestoreOwnRecipeUseCase(recipes, favorites)
    private val receta = ownRecipe(id = "r1", createdAtMillis = 500L)

    @Test
    fun `CA-SAV-05-03 elimina la receta y su corazon y guarda una copia`() = runTest {
        recipes.recipes.value = listOf(receta)
        favorites.favorites.value = listOf(savedFavorite("r1", FavoriteSource.OWN, savedAtMillis = 700L))

        val copia = delete("r1").getOrThrow()

        assertTrue(recipes.recipes.value.isEmpty())
        assertTrue(favorites.favorites.value.isEmpty())
        assertTrue(copia.wasFavorite)
        assertEquals(700L, copia.favoriteSavedAtMillis)
        assertEquals(receta, copia.recipe)
    }

    @Test
    fun `CA-SAV-05-03 sin corazon no toca las favoritas`() = runTest {
        recipes.recipes.value = listOf(receta)

        val copia = delete("r1").getOrThrow()

        assertFalse(copia.wasFavorite)
        assertTrue(favorites.setCalls.isEmpty())
    }

    @Test
    fun `CA-SAV-05-04 deshacer devuelve la receta con su id, su fecha y su corazon`() = runTest {
        recipes.recipes.value = listOf(receta)
        favorites.favorites.value = listOf(savedFavorite("r1", FavoriteSource.OWN))
        val copia = delete("r1").getOrThrow()

        restore(copia).getOrThrow()

        assertEquals(listOf(receta), recipes.recipes.value)
        assertEquals(false, recipes.saveCalls.last().isNew)
        assertEquals(listOf("r1"), favorites.favorites.value.map { it.recipeId })
    }

    @Test
    fun `CA-SAV-06-01 cuenta sin repetir la creada que es favorita ni las huerfanas`() = runTest {
        recipes.recipes.value = listOf(receta, ownRecipe(id = "r2"))
        favorites.favorites.value = listOf(
            savedFavorite("r1", FavoriteSource.OWN),
            savedFavorite("borrada", FavoriteSource.OWN),
            savedFavorite("no-existe", FavoriteSource.CATALOG),
        )

        assertEquals(2, CountSavedRecipesUseCase(favorites, recipes, EmptyRecipeCatalog)().first())
    }

    @Test
    fun `CA-SAV-06-02 el contador cambia al quitar una favorita del catalogo`() = runTest {
        val catalogo = object : RecipeCatalog by EmptyRecipeCatalog {
            override suspend fun recipes(): List<Recipe> = listOf(recetaCatalogo)
        }
        favorites.favorites.value = listOf(savedFavorite("cat-1", FavoriteSource.CATALOG))
        val contar = CountSavedRecipesUseCase(favorites, recipes, catalogo)
        assertEquals(1, contar().first())

        favorites.favorites.value = emptyList()

        assertEquals(0, contar().first())
    }

    private val recetaCatalogo = Recipe(
        id = "cat-1",
        name = "Arepas",
        servings = 2,
        prepMinutes = 5,
        cookMinutes = 10,
        difficulty = "Fácil",
        kcal = 300,
        ingredients = emptyList(),
        steps = emptyList(),
    )
}
