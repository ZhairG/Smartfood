package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Nombre: GeneratePlanUseCaseTest.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Pruebas del caso de uso T014. */
class GeneratePlanUseCaseTest {
    private class EmptyCatalog : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = emptyList()
        override suspend fun ingredients(): Map<String, Ingredient> = emptyMap()
    }

    @Test
    fun `CA-RIA-02-07 inventario y catalogo vacios devuelven sin plan`() = runTest {
        val repository = FakeInventoryRepository()
        repository.items.value = emptyList()
        val observe = ObserveInventoryUseCase(repository, FakePhotoSync(), ClassifyExpiryUseCase(fixedClock(LocalDate.of(2026, 10, 20))))
        val useCase = GeneratePlanUseCase(observe, EmptyCatalog(), IngredientMatcher(fixedClock(LocalDate.of(2026, 10, 20))), GetFoodPreferencesUseCase(FakeProfileRepository()), GetPreferredStoreUseCase(GetFoodPreferencesUseCase(FakeProfileRepository())))

        assertTrue(useCase(PlanContext(2_000, 4)) is PlanResult.NoPlan)
    }
}
