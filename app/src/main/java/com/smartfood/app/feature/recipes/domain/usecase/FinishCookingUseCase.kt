package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.PhotoEdit
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.PantryStock
import com.smartfood.app.feature.recipes.domain.agent.RecipeEvaluator
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Nombre: FinishCookingUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Finaliza la preparación de una receta (spec 009, CA-RIA-04-05 y 04-06; Zhair #116):
 * 1. Lee la receta con [GetRecipeDetailUseCase] y el inventario con [ObserveInventoryUseCase].
 * 2. Descuenta los ingredientes en su unidad original usando [RecipeEvaluator].
 * 3. Llama a [DeleteItemUseCase] si la cantidad queda <= 0 o no tenía cantidad, o [UpdateItemUseCase] si queda sobrante.
 * 4. Calcula `rescuedValueCop` para productos usados con vencimiento en 0..3 días.
 * 5. Guarda el [CookingRecord] en Firestore/local.
 */
class FinishCookingUseCase @Inject constructor(
    private val getRecipeDetail: GetRecipeDetailUseCase,
    private val observeInventory: ObserveInventoryUseCase,
    private val getPreferredStore: GetPreferredStoreUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val cookingHistoryRepository: CookingHistoryRepository,
    private val ingredientMatcher: IngredientMatcher,
    private val clock: Clock,
) {
    suspend operator fun invoke(recipeId: String, servings: Int): Result<WriteOutcome> {
        val detail = getRecipeDetail(recipeId).getOrElse { return Result.failure(it) }
        val recipe = detail.recipe
        val store = getPreferredStore()
        val sections = observeInventory().first()
        val inventoryItems = sections.flatMap { it.items }
        val inventoryMap = inventoryItems.associateBy { it.id }

        val matchedStock = PantryStock.of(ingredientMatcher.match(inventoryItems, detail.ingredients))
        val evaluation = RecipeEvaluator(detail.ingredients).evaluate(
            recipe = recipe,
            servings = servings,
            day = 1,
            stock = matchedStock,
            store = store
        )

        val today = LocalDate.now(clock)
        var totalRescuedCop = 0.0
        var worstOutcome = WriteOutcome.SYNCED

        // 1. Recorrer los usos e inventario
        for (use in evaluation.uses) {
            val ingredient = detail.ingredients[use.ingredientId] ?: continue
            val packPrice = ingredient.priceAt(store).priceCop
            val packSize = ingredient.pack

            for ((itemId, takenBaseUnit) in use.fromPantry) {
                val item = inventoryMap[itemId] ?: continue
                val daysToExpire = ChronoUnit.DAYS.between(today, item.expiresOn).toInt()

                // Calcular el valor rescatado si vence en 0..3 días (CA-RIA-04-06)
                if (daysToExpire in 0..3) {
                    val amountUsedBase = takenBaseUnit ?: item.quantity?.amount ?: packSize
                    val itemRescuedValue = (amountUsedBase * packPrice) / packSize
                    totalRescuedCop += itemRescuedValue
                }

                // Descontar inventario
                if (takenBaseUnit == null || item.quantity == null) {
                    // Sin cantidad o usado completo
                    val result = deleteItemUseCase(item)
                    result.onSuccess { if (it == WriteOutcome.PENDING) worstOutcome = WriteOutcome.PENDING }
                        .onFailure { return Result.failure(it) }
                } else {
                    val originalUnit = item.quantity.unit
                    val usedInOriginalUnit = convertBaseUnitToOriginal(takenBaseUnit, originalUnit)
                    val newAmount = item.quantity.amount - usedInOriginalUnit

                    if (newAmount <= 0.001) {
                        val result = deleteItemUseCase(item)
                        result.onSuccess { if (it == WriteOutcome.PENDING) worstOutcome = WriteOutcome.PENDING }
                            .onFailure { return Result.failure(it) }
                    } else {
                        val newQuantityText = formatDouble(newAmount)
                        val draft = ItemDraft(
                            name = item.name,
                            expiresOn = item.expiresOn,
                            category = item.category,
                            quantityText = newQuantityText,
                            unit = originalUnit
                        )
                        val result = updateItemUseCase(item, draft, PhotoEdit.Keep)
                        result.onSuccess { if (it.outcome == WriteOutcome.PENDING) worstOutcome = WriteOutcome.PENDING }
                            .onFailure { return Result.failure(it) }
                    }
                }
            }
        }

        // 2. Guardar registro de cocción
        val recordId = cookingHistoryRepository.newRecordId()
        val record = CookingRecord(
            id = recordId,
            recipeId = recipeId,
            servings = servings,
            cookedAt = System.currentTimeMillis(),
            rescuedValueCop = totalRescuedCop.toInt().coerceAtLeast(0)
        )

        cookingHistoryRepository.saveRecord(record)
            .getOrElse { return Result.failure(it) }

        return Result.success(worstOutcome)
    }

    private fun convertBaseUnitToOriginal(amountBase: Double, originalUnit: QuantityUnit): Double {
        return when (originalUnit) {
            QuantityUnit.KILOGRAMS -> amountBase / 1000.0
            QuantityUnit.POUNDS -> amountBase / 453.59237
            QuantityUnit.LITERS -> amountBase / 1000.0
            else -> amountBase
        }
    }

    private fun formatDouble(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString().replace('.', ',')
        }
    }
}
