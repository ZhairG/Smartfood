package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.testutil.FakeFavoritesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ToggleFavoriteUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas del cambio de ♡ de una receta (spec 010, CA-SAV-01-03 y CA-SAV-01-04, T004).
 */
class ToggleFavoriteUseCaseTest {

    private val repository = FakeFavoritesRepository()
    private val toggle = ToggleFavoriteUseCase(repository)

    @Test
    fun `CA-SAV-01-03 marcar favorita delega el id y origen y actualiza la lista`() = runTest {
        val result = toggle("pasta-verde", FavoriteSource.CATALOG, favorite = true)

        assertEquals(WriteOutcome.SYNCED, result.getOrNull())
        assertEquals(
            listOf(FakeFavoritesRepository.FavoriteCall("pasta-verde", FavoriteSource.CATALOG, true)),
            repository.setCalls,
        )
        assertEquals(listOf("pasta-verde"), repository.favorites.value.map { it.recipeId })
    }

    @Test
    fun `CA-SAV-01-04 una escritura pendiente conserva el cambio local`() = runTest {
        repository.outcome = WriteOutcome.PENDING
        repository.pendingWrites = true

        val result = toggle("arepas", FavoriteSource.CATALOG, favorite = true)

        assertEquals(WriteOutcome.PENDING, result.getOrNull())
        assertTrue(repository.favorites.value.single().isPendingSync)
    }

    @Test
    fun `CA-SAV-01-03 quitar favorita elimina la receta de la lista local`() = runTest {
        toggle("arepas", FavoriteSource.CATALOG, favorite = true)

        val result = toggle("arepas", FavoriteSource.CATALOG, favorite = false)

        assertEquals(WriteOutcome.SYNCED, result.getOrNull())
        assertTrue(repository.favorites.value.isEmpty())
    }
}
