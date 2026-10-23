package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.PantryStock
import com.smartfood.app.feature.recipes.domain.agent.RecipeEvaluator
import com.smartfood.app.feature.recipes.domain.model.RecipeDetail
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientGroup
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientLine
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredients
import com.smartfood.app.feature.recipes.domain.model.Store
import javax.inject.Inject

/**
 * Nombre: BuildRecipeIngredientsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Escala los ingredientes a las porciones elegidas y los agrupa por sección con el costo de lo que falta
 * (spec 009, CA-RIA-04-01 y 04-02, T008). Usa el mismo emparejamiento y evaluador del agente, así el detalle y el plan
 * calculan igual. No repite reglas en el ViewModel.
 *
 * @param matcher Emparejamiento inventario ↔ ingredientes (RF-RIA-02).
 */
class BuildRecipeIngredientsUseCase @Inject constructor(
    private val matcher: IngredientMatcher,
) {

    /**
     * @param detail Receta y catálogo.
     * @param inventory Productos del inventario con sesión.
     * @param servings Porciones elegidas.
     * @param store Supermercado de los precios (CA-RIA-04-07).
     */
    operator fun invoke(
        detail: RecipeDetail,
        inventory: List<InventoryItem>,
        servings: Int,
        store: Store = Store.D1,
    ): RecipeIngredients {
        val recipe = detail.recipe
        val scale = servings.toDouble() / recipe.servings
        val evaluator = RecipeEvaluator(detail.ingredients)
        val stock = PantryStock.of(matcher.match(inventory, detail.ingredients))
        val evaluation = evaluator.evaluate(recipe, servings, day = 1, stock = stock, store = store)
        val uses = evaluation.uses.associateBy { it.ingredientId }

        val lines = recipe.ingredients.map { used ->
            val ingredient = detail.ingredients.getValue(used.ingredientId)
            val use = uses.getValue(used.ingredientId)
            RecipeIngredientLine(
                ingredientId = ingredient.id,
                name = ingredient.name,
                section = ingredient.section,
                quantity = used.quantity * scale,
                unit = ingredient.unit,
                // "En tu inventario" solo si alcanza para toda la cantidad (CA-RIA-04-01).
                inInventory = use.toBuy <= 0.0 && use.fromPantry.isNotEmpty(),
                costCop = use.buyCostCop,
                pantryBasic = ingredient.pantryBasic,
                packs = use.packs,
                packLabel = ingredient.packLabel,
                store = use.store,
            )
        }

        val groups = lines
            .groupBy { it.section }
            .entries
            .sortedBy { (section, _) -> sectionRank(section) }
            .map { (section, items) -> RecipeIngredientGroup(section = section, lines = items) }

        return RecipeIngredients(groups = groups, missingCostCop = evaluation.costCop)
    }

    /** Orden del Figma `557:2856`; las secciones nuevas van al final en el orden en que aparecen. */
    private fun sectionRank(section: String): Int =
        SECTION_ORDER.indexOf(section).let { if (it < 0) SECTION_ORDER.size else it }

    private companion object {
        val SECTION_ORDER = listOf("Verduras", "Frutas", "Cereales", "Carnes", "Lácteos", "Otros")
    }
}
