package com.smartfood.app.feature.profile.ui.preferences

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.profile.domain.usecase.SaveFoodPreferencesUseCase
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: FoodPreferencesViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [FoodPreferencesViewModel] (spec 008, HU-PER-03, T013): carga, selección múltiple y
 * única, "Guardar Preferencias" habilitado solo con cambios, confirmación, guardado, "Guardado" automático, error y
 * salida sin guardar.
 */
class FoodPreferencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()

    private val guardadas = FoodPreferences(
        restrictions = setOf(DietaryRestriction.VEGETARIAN),
        cuisines = setOf(Cuisine.COLOMBIAN),
        spice = SpiceLevel.MILD,
        budget = MealBudget.ECONOMIC,
    )

    private fun crearViewModel() = FoodPreferencesViewModel(
        getPreferences = GetFoodPreferencesUseCase(repository),
        savePreferences = SaveFoodPreferencesUseCase(repository),
    )

    private val FoodPreferencesViewModel.state get() = uiState.value

    @Test
    fun `CA-PER-03-04 carga las preferencias guardadas en el borrador`() {
        repository.preferences = guardadas

        val state = crearViewModel().state

        assertFalse(state.isLoading)
        assertEquals(guardadas, state.saved)
        assertEquals(guardadas, state.draft)
        assertFalse("sin cambios, Guardar está deshabilitado", state.isSaveEnabled)
    }

    @Test
    fun `CA-PER-03-04 si la lectura falla se muestra el error y los chips quedan vacios`() {
        repository.getPreferencesResult = authFailure(AuthError.Network)

        val state = crearViewModel().state

        assertFalse(state.isLoading)
        assertEquals(AuthError.Network, state.error)
        assertEquals(FoodPreferences(), state.draft)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun `CA-PER-03-02 restricciones y cocinas permiten varios chips y se pueden quitar`() {
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.RestrictionToggled(DietaryRestriction.VEGAN))
        viewModel.onEvent(FoodPreferencesEvent.RestrictionToggled(DietaryRestriction.GLUTEN_FREE))
        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ITALIAN))
        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ASIAN))

        assertEquals(
            setOf(DietaryRestriction.VEGAN, DietaryRestriction.GLUTEN_FREE),
            viewModel.state.draft.restrictions
        )
        assertEquals(setOf(Cuisine.ITALIAN, Cuisine.ASIAN), viewModel.state.draft.cuisines)
        assertTrue(viewModel.state.isSaveEnabled)

        viewModel.onEvent(FoodPreferencesEvent.RestrictionToggled(DietaryRestriction.VEGAN))
        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ASIAN))

        assertEquals(setOf(DietaryRestriction.GLUTEN_FREE), viewModel.state.draft.restrictions)
        assertEquals(setOf(Cuisine.ITALIAN), viewModel.state.draft.cuisines)
    }

    @Test
    fun `CA-PER-03-02 picante y presupuesto permiten uno y tocar el elegido lo quita`() {
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.SpiceSelected(SpiceLevel.MILD))
        assertEquals(SpiceLevel.MILD, viewModel.state.draft.spice)

        viewModel.onEvent(FoodPreferencesEvent.SpiceSelected(SpiceLevel.HOT))
        assertEquals("el nuevo reemplaza al anterior", SpiceLevel.HOT, viewModel.state.draft.spice)

        viewModel.onEvent(FoodPreferencesEvent.SpiceSelected(SpiceLevel.HOT))
        assertNull("tocar el elegido lo quita", viewModel.state.draft.spice)

        viewModel.onEvent(FoodPreferencesEvent.BudgetSelected(MealBudget.MODERATE))
        viewModel.onEvent(FoodPreferencesEvent.BudgetSelected(MealBudget.UNLIMITED))
        assertEquals(MealBudget.UNLIMITED, viewModel.state.draft.budget)

        viewModel.onEvent(FoodPreferencesEvent.BudgetSelected(MealBudget.UNLIMITED))
        assertNull(viewModel.state.draft.budget)
    }

    @Test
    fun `CA-PER-03-03 Guardar pide confirmar, Cancelar no guarda y volver al dato guardado lo deshabilita`() {
        repository.preferences = guardadas
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ITALIAN))
        viewModel.onEvent(FoodPreferencesEvent.SaveClicked)
        assertEquals(FoodPreferencesDialog.ConfirmSave, viewModel.state.dialog)

        viewModel.onEvent(FoodPreferencesEvent.DialogDismissed)
        assertNull(viewModel.state.dialog)
        assertTrue(repository.savedPreferences.isEmpty())

        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ITALIAN))
        assertFalse("quitar el cambio lo deshabilita", viewModel.state.isSaveEnabled)
    }

    @Test
    fun `CA-PER-03-03 confirmar guarda, muestra Guardado y lo cierra a los 2 segundos`() = runTest {
        repository.preferences = guardadas
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.RestrictionToggled(DietaryRestriction.NUT_FREE))
        viewModel.onEvent(FoodPreferencesEvent.SpiceSelected(SpiceLevel.HOT))
        val esperado = viewModel.state.draft

        viewModel.onEvent(FoodPreferencesEvent.SaveConfirmed)
        runCurrent()

        assertEquals(listOf(esperado), repository.savedPreferences)
        assertEquals(esperado, viewModel.state.saved)
        assertEquals("el borrador queda igual a lo guardado", esperado, viewModel.state.draft)
        assertFalse(viewModel.state.isSaving)
        assertFalse(viewModel.state.hasChanges)
        assertEquals(FoodPreferencesDialog.Saved, viewModel.state.dialog)

        advanceTimeBy(FoodPreferencesViewModel.RESULT_DIALOG_MILLIS + 1)
        assertNull(viewModel.state.dialog)
    }

    @Test
    fun `CA-PER-03-03 si guardar falla muestra el error y conserva el borrador para reintentar`() = runTest {
        repository.preferences = guardadas
        repository.savePreferencesResult = authFailure(AuthError.Network)
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.MEXICAN))
        viewModel.onEvent(FoodPreferencesEvent.SaveConfirmed)
        runCurrent()

        assertEquals(AuthError.Network, viewModel.state.error)
        assertNull(viewModel.state.dialog)
        assertFalse(viewModel.state.isSaving)
        assertTrue("el borrador sigue ahí", viewModel.state.hasChanges)
        assertTrue(repository.savedPreferences.isEmpty())

        viewModel.onEvent(FoodPreferencesEvent.BudgetSelected(MealBudget.MODERATE))
        assertNull("tocar un chip quita el error", viewModel.state.error)
    }

    @Test
    fun `CA-PER-03-04 volver avisa al Perfil sin guardar el borrador`() = runTest {
        repository.preferences = guardadas
        val viewModel = crearViewModel()

        viewModel.onEvent(FoodPreferencesEvent.CuisineToggled(Cuisine.ITALIAN))
        viewModel.onEvent(FoodPreferencesEvent.BackClicked)

        assertEquals(FoodPreferencesEffect.NavigateBack, viewModel.effects.first())
        assertTrue(repository.savedPreferences.isEmpty())
    }
}
