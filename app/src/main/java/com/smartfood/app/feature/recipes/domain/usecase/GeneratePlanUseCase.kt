package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.MealPlanner
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Nombre: GeneratePlanUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Construye el contexto completo y ejecuta el agente de recetas fuera del hilo principal (T014).
 */
class GeneratePlanUseCase @Inject constructor(
    private val observeInventory: ObserveInventoryUseCase,
    private val catalog: RecipeCatalog,
    private val matcher: IngredientMatcher,
    private val getFoodPreferences: GetFoodPreferencesUseCase,
    private val getPreferredStore: GetPreferredStoreUseCase,
) {
    suspend operator fun invoke(context: PlanContext): PlanResult = withContext(Dispatchers.Default) {
        val ingredients = catalog.ingredients()
        val recipes = catalog.recipes()
        val items = observeInventory().first().flatMap { it.items }
        val preferences = getFoodPreferences().getOrNull()
        val diet = preferences?.restrictions.orEmpty().mapNotNull { it.toDietRule() }.toSet()
        val resolved = context.copy(diet = diet, store = getPreferredStore())
        MealPlanner(ingredients).plan(recipes, matcher.match(items, ingredients), resolved)
    }

    private fun DietaryRestriction.toDietRule(): DietRule? = when (this) {
        DietaryRestriction.VEGETARIAN -> DietRule.VEGETARIAN
        DietaryRestriction.VEGAN -> DietRule.VEGAN
        DietaryRestriction.GLUTEN_FREE -> DietRule.GLUTEN_FREE
        DietaryRestriction.LACTOSE_FREE -> DietRule.LACTOSE_FREE
        DietaryRestriction.NUT_FREE -> DietRule.NUT_FREE
    }
}
