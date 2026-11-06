package com.smartfood.app.feature.recipes.ui.own

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.usecase.DeleteOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.DeletedRecipeStore
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.ownRecipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: DeleteOwnRecipeViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Confirmación de "Eliminar receta" (spec 010, T018, CA-SAV-05-02 y 05-03).
 */
class DeleteOwnRecipeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recipes = FakeOwnRecipesRepository()
    private val store = DeletedRecipeStore()

    private fun viewModel() = DeleteOwnRecipeViewModel(
        savedStateHandle = SavedStateHandle(mapOf(DeleteOwnRecipeViewModel.RECIPE_ID_KEY to "r1")),
        delete = DeleteOwnRecipeUseCase(recipes, FakeFavoritesRepository()),
        store = store,
    )

    @Test
    fun `CA-SAV-05-03 confirmar elimina y deja la copia para el aviso`() = runTest {
        recipes.recipes.value = listOf(ownRecipe(id = "r1"))
        val viewModel = viewModel()

        viewModel.onConfirm()
        viewModel.deleted.first()

        assertTrue(recipes.recipes.value.isEmpty())
        assertEquals("r1", store.pending.value?.recipe?.id)
    }

    @Test
    fun `CA-SAV-05-02 antes de confirmar no se elimina nada`() {
        recipes.recipes.value = listOf(ownRecipe(id = "r1"))

        viewModel()

        assertEquals(1, recipes.recipes.value.size)
    }

    @Test
    fun `CA-SAV-05-03 si falla muestra el error y permite reintentar`() = runTest {
        val viewModel = viewModel()

        viewModel.onConfirm()

        val state = viewModel.uiState.first { !it.isDeleting }
        assertEquals(OwnRecipeError.NotFound, state.error)
        assertFalse(state.isDeleting)
    }
}
