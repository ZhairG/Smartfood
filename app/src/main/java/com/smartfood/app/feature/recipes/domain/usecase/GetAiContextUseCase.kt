package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.AiContext
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Nombre: GetAiContextUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Arma el contexto de "Sugerencia con IA" (spec 009, CA-RIA-02-01 y 02-09, T014): los productos del
 * inventario que usan las recetas, ya emparejados con el catálogo; las restricciones alimentarias del Perfil, también
 * como reglas del agente (`DietRule` tiene los mismos nombres que `DietaryRestriction`), y la tienda de los precios. Si
 * las preferencias no se pueden leer, el contexto sigue sin restricciones, como [GetPreferredStoreUseCase].
 *
 * @param catalog Recetas e ingredientes.
 * @param observeInventory Inventario de la persona con sesión.
 * @param matcher Emparejamiento inventario ↔ ingredientes (RF-RIA-02).
 * @param getFoodPreferences Preferencias alimentarias del Perfil (spec 008).
 * @param getPreferredStore "Mi supermercado".
 */
class GetAiContextUseCase @Inject constructor(
    private val catalog: RecipeCatalog,
    private val observeInventory: ObserveInventoryUseCase,
    private val matcher: IngredientMatcher,
    private val getFoodPreferences: GetFoodPreferencesUseCase,
    private val getPreferredStore: GetPreferredStoreUseCase,
) {

    suspend operator fun invoke(): AiContext {
        val ingredients = catalog.ingredients()
        val inventory = observeInventory().first().flatMap { it.items }
        val pantry = matcher.match(inventory, ingredients)
        val matchedIds = pantry.mapTo(HashSet()) { it.itemId }
        val restrictions = getFoodPreferences().getOrNull()?.restrictions.orEmpty()
        return AiContext(
            pantry = pantry,
            items = inventory.filter { it.id in matchedIds }.sortedWith(compareBy({ it.expiresOn }, { it.name })),
            ingredients = ingredients,
            restrictions = restrictions,
            diet = restrictions.mapTo(LinkedHashSet()) { DietRule.valueOf(it.name) },
            store = getPreferredStore(),
        )
    }
}
