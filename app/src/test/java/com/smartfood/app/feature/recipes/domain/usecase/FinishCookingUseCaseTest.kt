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
 * Descripción: Pruebas unitarias de [FinishCookingUseCase] (spec 009, CA-RIA-04-05, CA-RIA-04-06, T021): descuenta en la
 * unidad original (kg incluido), borra lo que queda en 0 o no tenía cantidad, cuenta como rescatado solo lo que vence
 * en 0 a 3 días y devuelve `PENDING` si algo quedó en la cola sin conexión.
 */
class FinishCookingUseCaseTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val clock = fixedClock(today)

    private val arepas = recipe("arepas_hogao", RecipeIngredient("arepa", 2.0))
    private val papas = recipe("papas_cocidas", RecipeIngredient("papa", 300.0))

    private val ingredients = mapOf(
        "arepa" to Ingredient(
            id = "arepa", name = "Arepa de maíz", section = "Cereales", unit = IngredientUnit.UNIT, pack = 5.0,
            prices = mapOf(Store.D1 to 3000L)
        ),
        "papa" to Ingredient(
            id = "papa", name = "Papa", section = "Verduras", unit = IngredientUnit.GRAM, pack = 1000.0,
            prices = mapOf(Store.D1 to 4000L)
        ),
    )

    private inner class FakeCatalog : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = listOf(arepas, papas)
        override suspend fun ingredients(): Map<String, Ingredient> = this@FinishCookingUseCaseTest.ingredients
    }

    private class FakeCookingHistoryRepository : CookingHistoryRepository {
        val saved = mutableListOf<CookingRecord>()
        var outcome = WriteOutcome.SYNCED
        override fun observeHistory(): Flow<List<CookingRecord>> = flowOf(saved)
        override fun newRecordId(): String = "record1"
        override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> {
            saved.add(record)
            return Result.success(outcome)
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

        val photoSync = NoOpPhotoSync()
        finishCookingUseCase = FinishCookingUseCase(
            getRecipeDetail = GetRecipeDetailUseCase(FakeCatalog()),
            observeInventory = ObserveInventoryUseCase(inventoryRepository, photoSync, ClassifyExpiryUseCase(clock)),
            getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(FakeProfileRepository())),
            updateItemUseCase = UpdateItemUseCase(inventoryRepository, photoSync, ValidateItemUseCase(clock)),
            deleteItemUseCase = DeleteItemUseCase(inventoryRepository, photoSync),
            cookingHistoryRepository = historyRepository,
            ingredientMatcher = IngredientMatcher(clock),
            clock = clock
        )
    }

    @Test
    fun `CA-RIA-04-05 descuenta inventario y guarda registro con valor rescatado`() = runTest {
        inventoryRepository.createItem(item("item1", "Arepa de maíz", Quantity(5.0, QuantityUnit.UNITS), days = 1))

        val result = finishCookingUseCase("arepas_hogao", 2)

        assertEquals(WriteOutcome.SYNCED, result.getOrNull())
        assertEquals(3.0, quantityOf("item1"), 0.001)
        val record = historyRepository.saved.single()
        assertEquals("arepas_hogao", record.recipeId)
        assertEquals(2, record.servings)
        assertEquals(clock.millis(), record.cookedAt)
        // 2 arepas de un paquete de 5 a $3.000 en D1.
        assertEquals(1200, record.rescuedValueCop)
    }

    @Test
    fun `CA-RIA-04-05 borra el producto que queda en 0`() = runTest {
        inventoryRepository.createItem(item("item1", "Arepa de maíz", Quantity(2.0, QuantityUnit.UNITS), days = 1))

        finishCookingUseCase("arepas_hogao", 2)

        assertEquals(listOf("item1"), inventoryRepository.deletedIds)
    }

    @Test
    fun `CA-RIA-04-05 borra el producto que no tenia cantidad`() = runTest {
        inventoryRepository.createItem(item("item1", "Arepa de maíz", quantity = null, days = 1))

        finishCookingUseCase("arepas_hogao", 2)

        assertEquals(listOf("item1"), inventoryRepository.deletedIds)
    }

    @Test
    fun `CA-RIA-04-05 descuenta en la unidad original del producto (kg)`() = runTest {
        inventoryRepository.createItem(item("papa1", "Papa", Quantity(1.0, QuantityUnit.KILOGRAMS), days = 10))

        finishCookingUseCase("papas_cocidas", 2)

        val papa = inventoryRepository.items.value.single { it.id == "papa1" }
        assertEquals(QuantityUnit.KILOGRAMS, papa.quantity?.unit)
        assertEquals(0.7, papa.quantity!!.amount, 0.001)
    }

    @Test
    fun `CA-RIA-04-06 lo que vence en mas de 3 dias no cuenta como rescatado`() = runTest {
        inventoryRepository.createItem(item("papa1", "Papa", Quantity(1.0, QuantityUnit.KILOGRAMS), days = 10))

        finishCookingUseCase("papas_cocidas", 2)

        assertEquals(0, historyRepository.saved.single().rescuedValueCop)
    }

    @Test
    fun `CA-RIA-04-05 sin conexion devuelve PENDING para mostrar Guardado en tu telefono`() = runTest {
        inventoryRepository.createItem(item("item1", "Arepa de maíz", Quantity(5.0, QuantityUnit.UNITS), days = 1))
        historyRepository.outcome = WriteOutcome.PENDING

        val result = finishCookingUseCase("arepas_hogao", 2)

        assertTrue(result.isSuccess)
        assertEquals(WriteOutcome.PENDING, result.getOrNull())
    }

    private fun recipe(id: String, ingredient: RecipeIngredient) = Recipe(
        id = id,
        name = id,
        photo = null,
        servings = 2,
        prepMinutes = 10,
        cookMinutes = 15,
        difficulty = "Fácil",
        kcal = 350,
        ingredients = listOf(ingredient),
        beforeStarting = emptyList(),
        steps = listOf(RecipeStep("Cocinar", "Cocina", minutes = null))
    )

    private fun item(id: String, name: String, quantity: Quantity?, days: Long) = InventoryItem(
        id = id,
        name = name,
        category = FoodCategory.GRAINS,
        expiresOn = today.plusDays(days),
        quantity = quantity
    )

    private fun quantityOf(id: String): Double =
        inventoryRepository.items.value.single { it.id == id }.quantity!!.amount
}
