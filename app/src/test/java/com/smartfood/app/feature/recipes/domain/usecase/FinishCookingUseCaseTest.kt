package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: FinishCookingUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [FinishCookingUseCase] (spec 009, CA-RIA-04-05, CA-RIA-04-06, T021).
 */
class FinishCookingUseCaseTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val clock = fixedClock(today)

    private val recipe = Recipe(
        id = "arepas_hogao",
        name = "Arepas con hogao",
        photo = null,
        servings = 2,
        prepMinutes = 10,
        cookMinutes = 15,
        difficulty = "Fácil",
        kcal = 350,
        ingredients = listOf(RecipeIngredient("arepa", 2.0)),
        beforeStarting = emptyList(),
        steps = listOf(RecipeStep("Calentar", "Calienta", minutes = null))
    )

    private val ingredient = Ingredient(
        id = "arepa",
        name = "Arepa de maíz",
        section = "Cereales",
        unit = IngredientUnit.UNIT,
        pack = 5.0,
        prices = mapOf(Store.D1 to 3000L)
    )

    private inner class FakeCatalog : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = listOf(recipe)
        override suspend fun ingredients(): Map<String, Ingredient> = mapOf("arepa" to ingredient)
    }

    private class FakeCookingHistoryRepository : CookingHistoryRepository {
        val saved = mutableListOf<CookingRecord>()
        override fun observeHistory(): Flow<List<CookingRecord>> = flowOf(saved)
        override fun newRecordId(): String = "record1"
        override suspend fun saveRecord(record: CookingRecord): Result<Unit> {
            saved.add(record)
            return Result.success(Unit)
        }
    }

    private class NoOpPhotoSync : PhotoSync {
        override fun cancelUpload(itemId: String) {}
        override fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?) {}
        override fun enqueueDelete(path: String) {}
        override fun pendingPhotos(): Flow<Map<String, String>> = flowOf(emptyMap())
        override fun hasPending(): Boolean = false
        override fun clearAll() {}
    }

    private lateinit var inventoryRepository: FakeInventoryRepository
    private lateinit var historyRepository: FakeCookingHistoryRepository
    private lateinit var finishCookingUseCase: FinishCookingUseCase

    @Before
    fun setUp() {
        inventoryRepository = FakeInventoryRepository()
        historyRepository = FakeCookingHistoryRepository()

        val getRecipeDetail = GetRecipeDetailUseCase(FakeCatalog())
        val photoSync = NoOpPhotoSync()
        val classifyExpiry = ClassifyExpiryUseCase(clock)
        val observeInventory = ObserveInventoryUseCase(inventoryRepository, photoSync, classifyExpiry)
        val getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(FakeProfileRepository()))
        val validateItem = ValidateItemUseCase(clock)
        val updateItem = UpdateItemUseCase(inventoryRepository, photoSync, validateItem)
        val deleteItem = DeleteItemUseCase(inventoryRepository, photoSync)
        val matcher = IngredientMatcher(clock)

        finishCookingUseCase = FinishCookingUseCase(
            getRecipeDetail = getRecipeDetail,
            observeInventory = observeInventory,
            getPreferredStore = getPreferredStore,
            updateItemUseCase = updateItem,
            deleteItemUseCase = deleteItem,
            cookingHistoryRepository = historyRepository,
            ingredientMatcher = matcher,
            clock = clock
        )
    }

    @Test
    fun `CA-RIA-04-05 descuenta inventario y guarda registro con valor rescatado`() = runTest {
        val item = InventoryItem(
            id = "item1",
            name = "Arepa de maíz",
            category = FoodCategory.GRAINS,
            expiresOn = today.plusDays(1),
            quantity = Quantity(5.0, QuantityUnit.UNITS)
        )
        inventoryRepository.createItem(item)

        val result = finishCookingUseCase("arepas_hogao", 2)

        assertTrue(result.isSuccess)
        assertEquals(WriteOutcome.SYNCED, result.getOrNull())
        assertEquals(1, historyRepository.saved.size)

        val record = historyRepository.saved.first()
        assertEquals("arepas_hogao", record.recipeId)
        assertEquals(2, record.servings)
        assertEquals(1200, record.rescuedValueCop)
    }
}
