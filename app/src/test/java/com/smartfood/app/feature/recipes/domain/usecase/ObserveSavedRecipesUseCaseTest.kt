package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.savedFavorite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ObserveSavedRecipesUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de filtros, búsqueda, orden y sincronización de Guardadas (spec 010, CA-SAV-02-03 a 02-05 y
 * CA-SAV-02-09, T004). T004 aún combina solo las favoritas del catálogo; las recetas propias se agregan en T010.
 */
class ObserveSavedRecipesUseCaseTest {

    private val favorites = FakeFavoritesRepository()
    private val catalog = FakeCatalog(
        listOf(
            recipe("arepas", "Arepas de chócolo", minutes = 25),
            recipe("pasta", "Pasta verde", minutes = 30),
            recipe("sopa", "Sopa de tomate", minutes = 20),
        )
    )
    private val observe = ObserveSavedRecipesUseCase(favorites, catalog)

    @Test
    fun `CA-SAV-02-03 recientes ordena favoritas por la última vez que se guardaron`() = runTest {
        favorites.favorites.value = listOf(
            savedFavorite("arepas", savedAtMillis = 100L),
            savedFavorite("pasta", savedAtMillis = 300L),
            savedFavorite("sopa", savedAtMillis = 200L),
        )

        val data = savedData(SavedFilter.RECENT, "", SavedSort.RECENT)

        assertEquals(listOf("pasta", "sopa", "arepas"), data.cards.map { it.id })
        assertTrue(data.cards.all { it.isFavorite && !it.isOwn })
    }

    @Test
    fun `CA-SAV-02-04 busca sin tildes ni mayúsculas dentro del chip elegido`() = runTest {
        favorites.favorites.value = listOf(
            savedFavorite("arepas", savedAtMillis = 100L),
            savedFavorite("pasta", savedAtMillis = 200L),
        )

        val data = savedData(SavedFilter.FAVORITES, "CHOCOLO", SavedSort.RECENT)

        assertEquals(listOf("arepas"), data.cards.map { it.id })
    }

    @Test
    fun `CA-SAV-02-05 nombre A-Z ordena por nombre normalizado`() = runTest {
        favorites.favorites.value = listOf(
            savedFavorite("sopa", savedAtMillis = 300L),
            savedFavorite("pasta", savedAtMillis = 200L),
            savedFavorite("arepas", savedAtMillis = 100L),
        )

        val data = savedData(SavedFilter.FAVORITES, "", SavedSort.NAME)

        assertEquals(listOf("arepas", "pasta", "sopa"), data.cards.map { it.id })
    }

    @Test
    fun `CA-SAV-02-03 descarta favoritas huérfanas y no las cuenta para el vacío general`() = runTest {
        favorites.favorites.value = listOf(savedFavorite("receta-eliminada"))

        val data = savedData(SavedFilter.FAVORITES, "", SavedSort.RECENT)

        assertTrue(data.cards.isEmpty())
        assertTrue(data.isEmptyOverall)
    }

    @Test
    fun `CA-SAV-02-09 expone que una favorita válida espera sincronizarse`() = runTest {
        favorites.favorites.value = listOf(savedFavorite("pasta", isPendingSync = true))

        val data = savedData(SavedFilter.FAVORITES, "", SavedSort.RECENT)

        assertTrue(data.hasPendingSync)
        assertTrue(data.cards.single().isPendingSync)
        assertFalse(data.isEmptyOverall)
    }

    @Test
    fun `CA-SAV-02-03 creadas por mí queda vacío hasta que T010 agregue recetas propias`() = runTest {
        favorites.favorites.value = listOf(savedFavorite("pasta"))

        val data = savedData(SavedFilter.OWN, "", SavedSort.RECENT)

        assertTrue(data.cards.isEmpty())
        assertFalse(data.isEmptyOverall)
    }

    private suspend fun savedData(
        filter: SavedFilter,
        query: String,
        sort: SavedSort,
    ) = observe(flowOf(filter), flowOf(query), flowOf(sort)).first()

    private class FakeCatalog(
        private val values: List<Recipe>,
    ) : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = values
        override suspend fun ingredients() = emptyMap<String, com.smartfood.app.feature.recipes.domain.model.Ingredient>()
    }

    private fun recipe(id: String, name: String, minutes: Int) = Recipe(
        id = id,
        name = name,
        servings = 2,
        prepMinutes = minutes / 2,
        cookMinutes = minutes - minutes / 2,
        difficulty = "Fácil",
        kcal = 400,
        ingredients = emptyList(),
        steps = emptyList(),
    )
}
