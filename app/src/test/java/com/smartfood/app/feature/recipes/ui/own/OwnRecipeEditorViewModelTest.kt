package com.smartfood.app.feature.recipes.ui.own

import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.usecase.GetOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SaveOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ValidateOwnRecipeUseCase
import com.smartfood.app.testutil.EmptyRecipeCatalog
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.ownRecipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: OwnRecipeEditorViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Editor de recetas propias (spec 010, T015): guardar, cargar, errores por campo, hojas de categoría e
 * ingrediente, descartar y eliminar (CA-SAV-03-02 a 03-06, 04-01 a 04-04 y 05-01).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnRecipeEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val recipes = FakeOwnRecipesRepository()
    private val favorites = FakeFavoritesRepository()
    private val save = SaveOwnRecipeUseCase(recipes, favorites, ValidateOwnRecipeUseCase())
    private val viewModel by lazy { OwnRecipeEditorViewModel(save, GetOwnRecipeUseCase(recipes), EmptyRecipeCatalog) }

    @get:Rule val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private fun llenarFormulario() {
        viewModel.onEvent(OwnRecipeEditorEvent.NameChanged("Arroz casero"))
        viewModel.onEvent(OwnRecipeEditorEvent.CategoryPicked(RecipeCategory.ARROCES))
        viewModel.onEvent(OwnRecipeEditorEvent.CookMinutesChanged("30"))
        viewModel.onEvent(OwnRecipeEditorEvent.PreparationChanged("Cocinar"))
    }

    private fun agregarIngrediente(id: String, cantidad: String) {
        viewModel.onEvent(OwnRecipeEditorEvent.AddIngredientClicked)
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientPicked(id))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientQuantityChanged(cantidad))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientConfirmed)
    }

    @Test fun `CA-SAV-03-04 guardar emite efecto con receta creada`() = runTest(dispatcher) {
        llenarFormulario()
        viewModel.onEvent(OwnRecipeEditorEvent.SaveClicked)
        runCurrent()
        assertEquals(OwnRecipeEditorEffect.Saved("own-recipe-1", "Arroz casero", false), viewModel.effects.first())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test fun `CA-SAV-04-01 inicializar edicion carga la receta`() = runTest(dispatcher) {
        recipes.recipes.value = listOf(ownRecipe(id = "edit-1"))
        viewModel.initialize("edit-1")
        runCurrent()
        assertEquals("Arroz casero", viewModel.uiState.value.draft.name)
        assertEquals("30", viewModel.uiState.value.draft.cookMinutes)
        assertFalse(viewModel.uiState.value.isNew)
    }

    @Test fun `CA-SAV-04-02 guardar una edicion avisa que fue editada`() = runTest(dispatcher) {
        recipes.recipes.value = listOf(ownRecipe(id = "edit-1"))
        viewModel.initialize("edit-1")
        runCurrent()
        viewModel.onEvent(OwnRecipeEditorEvent.SaveClicked)
        runCurrent()
        assertEquals(OwnRecipeEditorEffect.Saved("edit-1", "Arroz casero", true), viewModel.effects.first())
    }

    @Test fun `CA-SAV-03-06 un campo invalido queda en rojo y no se guarda`() = runTest(dispatcher) {
        llenarFormulario()
        viewModel.onEvent(OwnRecipeEditorEvent.CookMinutesChanged("0"))
        viewModel.onEvent(OwnRecipeEditorEvent.SaveClicked)
        runCurrent()
        assertEquals(OwnRecipeField.COOK_MINUTES, viewModel.uiState.value.errorField)
        assertEquals(OwnRecipeError.Invalid(OwnRecipeField.COOK_MINUTES), viewModel.uiState.value.error)
        assertTrue(recipes.saveCalls.isEmpty())
    }

    @Test fun `CA-SAV-04-03 al corregir el aviso y el rojo desaparecen`() = runTest(dispatcher) {
        llenarFormulario()
        viewModel.onEvent(OwnRecipeEditorEvent.CookMinutesChanged("0"))
        viewModel.onEvent(OwnRecipeEditorEvent.SaveClicked)
        runCurrent()
        viewModel.onEvent(OwnRecipeEditorEvent.CookMinutesChanged("30"))
        assertNull(viewModel.uiState.value.error)
    }

    @Test fun `CA-SAV-03-02 la hoja de categoria se abre y la elegida queda en el borrador`() = runTest(dispatcher) {
        viewModel.onEvent(OwnRecipeEditorEvent.CategoryClicked)
        assertTrue(viewModel.uiState.value.isCategorySheetOpen)
        viewModel.onEvent(OwnRecipeEditorEvent.CategoryPicked(RecipeCategory.PASTAS))
        assertFalse(viewModel.uiState.value.isCategorySheetOpen)
        assertEquals(RecipeCategory.PASTAS, viewModel.uiState.value.draft.category)
    }

    @Test fun `CA-SAV-03-03 agregar un ingrediente acepta coma decimal y cierra la hoja`() = runTest(dispatcher) {
        agregarIngrediente("arroz", "2,5")
        assertEquals(listOf(RecipeIngredient("arroz", 2.5)), viewModel.uiState.value.draft.ingredients)
        assertNull(viewModel.uiState.value.ingredientSheet)
    }

    @Test fun `CA-SAV-03-03 sin una cantidad valida la hoja sigue abierta`() = runTest(dispatcher) {
        viewModel.onEvent(OwnRecipeEditorEvent.AddIngredientClicked)
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientPicked("arroz"))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientQuantityChanged("0"))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientConfirmed)
        assertTrue(viewModel.uiState.value.ingredientSheet?.isQuantityInvalid == true)
        assertTrue(viewModel.uiState.value.draft.ingredients.isEmpty())
    }

    @Test fun `CA-SAV-03-03 tocar una fila cambia la cantidad sin repetir el ingrediente`() = runTest(dispatcher) {
        agregarIngrediente("arroz", "1")
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientClicked("arroz"))
        assertEquals("1", viewModel.uiState.value.ingredientSheet?.quantity)
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientQuantityChanged("3"))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientConfirmed)
        assertEquals(listOf(RecipeIngredient("arroz", 3.0)), viewModel.uiState.value.draft.ingredients)
    }

    @Test fun `CA-SAV-03-03 quitar elimina la fila`() = runTest(dispatcher) {
        agregarIngrediente("arroz", "1")
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientClicked("arroz"))
        viewModel.onEvent(OwnRecipeEditorEvent.IngredientRemoved)
        assertTrue(viewModel.uiState.value.draft.ingredients.isEmpty())
    }

    @Test fun `CA-SAV-04-04 descartar vuelve sin guardar`() = runTest(dispatcher) {
        llenarFormulario()
        viewModel.onEvent(OwnRecipeEditorEvent.DiscardClicked)
        runCurrent()
        assertEquals(OwnRecipeEditorEffect.Close, viewModel.effects.first())
        assertTrue(recipes.saveCalls.isEmpty())
    }

    @Test fun `CA-SAV-05-01 eliminar abre la confirmacion con el nombre`() = runTest(dispatcher) {
        recipes.recipes.value = listOf(ownRecipe(id = "edit-1"))
        viewModel.initialize("edit-1")
        runCurrent()
        viewModel.onEvent(OwnRecipeEditorEvent.DeleteClicked)
        runCurrent()
        assertEquals(OwnRecipeEditorEffect.OpenDelete("edit-1", "Arroz casero"), viewModel.effects.first())
    }
}
