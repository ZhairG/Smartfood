package com.smartfood.app.feature.recipes.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.Supermarket
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.BuildRecipeIngredientsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipeDetailUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecipeDetailViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas del detalle (spec 009, CA-RIA-04-01 y 04-02, T008): agrupación y "En tu inventario", costo de lo
 * que falta y recálculo de cantidades y costos al cambiar las porciones, con límites 1 y 12.
 */
class RecipeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hoy = LocalDate.of(2026, 10, 20)
    private val clock = fixedClock(hoy)

    private val recipe = Recipe(
        id = "pollo-guisado",
        name = "Pollo guisado con arroz",
        photo = "receta_pollo_guisado",
        servings = 4,
        prepMinutes = 15,
        cookMinutes = 25,
        restMinutes = 0,
        difficulty = "Media",
        kcal = 480,
        ingredients = listOf(
            RecipeIngredient("pollo", 600.0),
            RecipeIngredient("arroz", 300.0),
            RecipeIngredient("tomate", 200.0),
        ),
        steps = emptyList(),
    )

    private val ingredients = mapOf(
        "pollo" to ingredient("pollo", "Pollo", "Carnes", 14_500),
        "arroz" to ingredient("arroz", "Arroz", "Cereales", 4_000),
        "tomate" to ingredient("tomate", "Tomate", "Verduras", 250, pack = 100.0),
    )

    private val inventory = listOf(
        item("pollo-1", "Pollo", FoodCategory.PROTEINS, 1_000.0),
        item("arroz-1", "Arroz", FoodCategory.GRAINS, 1_000.0),
    )

    @Test
    fun `CA-RIA-04-01 agrupa por secciones y marca lo que alcanza del inventario`() = runTest {
        val state = viewModel().uiState.first { !it.isLoading }

        assertEquals(listOf("Verduras", "Cereales", "Carnes"), state.groups.map { it.section })
        val tomate = state.groups.first { it.section == "Verduras" }.lines.single()
        assertEquals("Tomate", tomate.name)
        assertEquals(200.0, tomate.quantity, 0.0)
        assertFalse(tomate.inInventory)
        // 200 g de tomate: 2 empaques de 100 g a 250.
        assertEquals(500L, tomate.costCop)

        val pollo = state.groups.first { it.section == "Carnes" }.lines.single()
        assertTrue(pollo.inInventory)
        assertEquals(0L, pollo.costCop)
        assertTrue(state.groups.first { it.section == "Cereales" }.lines.single().inInventory)
    }

    @Test
    fun `CA-RIA-04-02 agregar una porción recalcula cantidades y costos`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(RecipeDetailEvent.IncreaseServings)
        val state = viewModel.uiState.first { it.servings == 5 }

        assertEquals(5, state.servings)
        val tomate = state.groups.first { it.section == "Verduras" }.lines.single()
        assertEquals(250.0, tomate.quantity, 0.0)
        // 250 g: 3 empaques de 100 g.
        assertEquals(750L, tomate.costCop)
        assertEquals(
            750.0,
            state.groups.first { it.section == "Carnes" }.lines.single().quantity,
            0.0
        )
        assertEquals(
            375.0,
            state.groups.first { it.section == "Cereales" }.lines.single().quantity,
            0.0
        )
    }

    @Test
    fun `CA-RIA-04-02 quitar una porción recalcula y no baja de una`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(RecipeDetailEvent.DecreaseServings)
        val cuatro = viewModel.uiState.first { it.servings == 3 }
        assertEquals(
            150.0,
            cuatro.groups.first { it.section == "Verduras" }.lines.single().quantity,
            0.0
        )

        repeat(5) { viewModel.onEvent(RecipeDetailEvent.DecreaseServings) }
        val minima = viewModel.uiState.first { it.servings == 1 }
        assertEquals(1, minima.servings)
        assertEquals(
            50.0,
            minima.groups.first { it.section == "Verduras" }.lines.single().quantity,
            0.0
        )
        assertEquals(
            250L,
            minima.groups.first { it.section == "Verduras" }.lines.single().costCop
        )
    }

    @Test
    fun `CA-RIA-04-02 no pasa de doce porciones`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoading }

        repeat(12) { viewModel.onEvent(RecipeDetailEvent.IncreaseServings) }

        val state = viewModel.uiState.first { it.servings == 12 }
        assertEquals(RecipeDetailViewModel.MaxServings, state.servings)
        assertEquals(
            600.0,
            state.groups.first { it.section == "Verduras" }.lines.single().quantity,
            0.0
        )
    }

    @Test
    fun `CA-RIA-04-07 usa Mi supermercado del Perfil y D1 si no se eligió`() = runTest {
        assertEquals(Store.D1, viewModel().uiState.first { !it.isLoading }.store)

        profile.preferences = FoodPreferences(store = Supermarket.OLIMPICA)
        val state = viewModel().uiState.first { !it.isLoading && it.store == Store.OLIMPICA }

        val tomate = state.groups.first { it.section == "Verduras" }.lines.single()
        // La prueba solo tiene precio de D1: Olímpica no lo tiene y la fila dice que el precio es de D1.
        assertEquals(Store.D1, tomate.store)
        assertEquals(2, tomate.packs)
    }

    @Test
    fun `CA-RIA-04-01 si la receta no existe muestra el error`() = runTest {
        val state = viewModel(recipeId = "no-existe").uiState.first { !it.isLoading }

        assertTrue(state.hasError)
        assertTrue(state.groups.isEmpty())
    }

    private val profile = FakeProfileRepository()

    private fun viewModel(recipeId: String = recipe.id): RecipeDetailViewModel {
        val repository = FakeInventoryRepository().apply { items.value = inventory }
        val observeInventory = ObserveInventoryUseCase(
            repository = repository,
            photoSync = FakePhotoSync(),
            classifyExpiry = ClassifyExpiryUseCase(clock)
        )
        return RecipeDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(RecipeDetailViewModel.RECIPE_ID_KEY to recipeId)),
            getDetail = GetRecipeDetailUseCase(FakeRecipeCatalog()),
            buildIngredients = BuildRecipeIngredientsUseCase(IngredientMatcher(clock)),
            observeInventory = observeInventory,
            getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(profile)),
        )
    }

    private fun ingredient(id: String, name: String, section: String, priceCop: Long, pack: Double = 1_000.0) = Ingredient(
        id = id,
        name = name,
        section = section,
        unit = IngredientUnit.GRAM,
        pack = pack,
        prices = mapOf(Store.D1 to priceCop),
    )

    private fun item(id: String, name: String, category: FoodCategory, grams: Double) = InventoryItem(
        id = id,
        name = name,
        category = category,
        expiresOn = hoy.plusDays(10),
        quantity = Quantity(grams, QuantityUnit.GRAMS),
    )

    /** Catálogo fake de una sola receta. */
    private inner class FakeRecipeCatalog : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = listOf(recipe)
        override suspend fun ingredients(): Map<String, Ingredient> = ingredients
    }
}
