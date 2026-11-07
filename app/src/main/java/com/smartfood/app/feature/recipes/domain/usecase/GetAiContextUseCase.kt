package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.AiContext
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: GetAiContextUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Arma el contexto de "Sugerencia con IA" (spec 009, CA-RIA-02-01 y 02-09, T014): los productos del
 * inventario que usan las recetas, ya emparejados con el catálogo; las restricciones alimentarias del Perfil, también
 * como reglas del agente (`DietRule` tiene los mismos nombres que `DietaryRestriction`), las cocinas favoritas (spec 011)
 * y la tienda de los precios. Si las preferencias no se pueden leer, el contexto sigue sin restricciones ni cocinas,
 * como [GetPreferredStoreUseCase] (CA-COC-03-04). [observe]
 * lo mantiene al día mientras la pantalla sigue en la pila (CA-RIA-02-11).
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

    /** El contexto con el inventario, el Perfil y la tienda de este momento. */
    suspend operator fun invoke(): AiContext = build(observeInventory().first().flatMap { it.items })

    /** El contexto, otra vez cada vez que cambia el inventario (CA-RIA-02-11). */
    fun observe(): Flow<AiContext> = observeInventory().map { sections -> build(sections.flatMap { it.items }) }

    private suspend fun build(inventory: List<InventoryItem>): AiContext {
        val ingredients = catalog.ingredients()
        val pantry = matcher.match(inventory, ingredients)
        val matchedIds = pantry.mapTo(HashSet()) { it.itemId }
        val preferences = getFoodPreferences().getOrNull()
        val restrictions = preferences?.restrictions.orEmpty()
        return AiContext(
            pantry = pantry,
            items = inventory.filter { it.id in matchedIds }.sortedWith(compareBy({ it.expiresOn }, { it.name })),
            ingredients = ingredients,
            restrictions = restrictions,
            diet = restrictions.mapTo(LinkedHashSet()) { DietRule.valueOf(it.name) },
            store = getPreferredStore(),
            cuisines = preferences?.cuisines.orEmpty(),
        )
    }
}
