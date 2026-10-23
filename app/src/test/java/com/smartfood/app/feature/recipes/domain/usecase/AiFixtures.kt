package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.RealCatalog
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.fixedClock
import java.time.LocalDate

/**
 * Nombre: AiFixtures.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Datos de prueba del flujo de la IA (spec 009, T014): el catálogo real ([RealCatalog]), una despensa común
 * con pollo que vence mañana y un producto que ninguna receta usa, y los casos de uso armados con fakes. Lo usan
 * `GetAiContextUseCaseTest`, `GeneratePlanUseCaseTest` y `AiPlanViewModelTest`.
 */
internal class AiFixtures(restrictions: Set<DietaryRestriction> = emptySet()) {

    val today: LocalDate = LocalDate.of(2026, 10, 20)
    val clock = fixedClock(today)

    val catalog = object : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = RealCatalog.recipes
        override suspend fun ingredients(): Map<String, Ingredient> = RealCatalog.ingredients
    }

    val inventory = FakeInventoryRepository().apply {
        items.value = listOf(
            item("arroz", "Arroz", days = 60, Quantity(1.0, QuantityUnit.KILOGRAMS)),
            item("pollo", "Pollo", days = 1, Quantity(500.0, QuantityUnit.GRAMS)),
            item("papa", "Papa", days = 10, Quantity(1.0, QuantityUnit.KILOGRAMS)),
            item("cebolla", "Cebolla", days = 8, Quantity(3.0, QuantityUnit.UNITS)),
            item("tomate", "Tomate", days = 3, Quantity(4.0, QuantityUnit.UNITS)),
            item("ajo", "Ajo", days = 30, null),
            item("zanahoria", "Zanahoria", days = 12, Quantity(500.0, QuantityUnit.GRAMS)),
            item("jabon", "Jabón de loza", days = 400, null),
        )
    }

    val profile = FakeProfileRepository().apply { preferences = FoodPreferences(restrictions = restrictions) }

    private val preferences = GetFoodPreferencesUseCase(profile)

    val getAiContext = GetAiContextUseCase(
        catalog = catalog,
        observeInventory = ObserveInventoryUseCase(inventory, FakePhotoSync(), ClassifyExpiryUseCase(clock)),
        matcher = IngredientMatcher(clock),
        getFoodPreferences = preferences,
        getPreferredStore = GetPreferredStoreUseCase(preferences),
    )

    val generatePlan = GeneratePlanUseCase(catalog)

    private fun item(id: String, name: String, days: Long, quantity: Quantity?) = InventoryItem(
        id = id,
        name = name,
        category = FoodCategory.OTHER,
        expiresOn = today.plusDays(days),
        quantity = quantity,
    )
}
