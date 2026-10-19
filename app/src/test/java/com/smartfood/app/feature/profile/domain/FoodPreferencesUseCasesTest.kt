package com.smartfood.app.feature.profile.domain

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.profile.domain.usecase.SaveFoodPreferencesUseCase
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: FoodPreferencesUseCasesTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de los casos de uso de preferencias alimentarias (spec 008, HU-PER-03, T011): leen y
 * guardan en el repositorio y propagan el error tipado.
 */
class FoodPreferencesUseCasesTest {

    private val repository = FakeProfileRepository()
    private val getPreferences = GetFoodPreferencesUseCase(repository)
    private val savePreferences = SaveFoodPreferencesUseCase(repository)

    private val completas = FoodPreferences(
        restrictions = setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.NUT_FREE),
        cuisines = setOf(Cuisine.COLOMBIAN),
        spice = SpiceLevel.MEDIUM,
        budget = MealBudget.MODERATE,
    )

    @Test
    fun `CA-PER-03-04 leer devuelve las preferencias guardadas`() = runTest {
        repository.preferences = completas

        assertEquals(Result.success(completas), getPreferences())
    }

    @Test
    fun `CA-PER-03-04 leer propaga el error del repositorio`() = runTest {
        repository.getPreferencesResult = Result.failure(AuthException(AuthError.Network))

        assertEquals(AuthError.Network, getPreferences().authError)
    }

    @Test
    fun `CA-PER-03-04 guardar envia las preferencias al repositorio`() = runTest {
        assertTrue(savePreferences(completas).isSuccess)

        assertEquals(listOf(completas), repository.savedPreferences)
        assertEquals(completas, repository.preferences)
    }

    @Test
    fun `CA-PER-03-04 guardar propaga el error sin tocar lo guardado`() = runTest {
        repository.savePreferencesResult = Result.failure(AuthException(AuthError.Network))

        assertEquals(AuthError.Network, savePreferences(completas).authError)
        assertTrue(repository.savedPreferences.isEmpty())
    }
}
