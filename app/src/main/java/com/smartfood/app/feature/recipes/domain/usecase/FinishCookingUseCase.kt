package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.PantryStock
import com.smartfood.app.feature.recipes.domain.agent.RecipeEvaluator
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.InventoryChange
import com.smartfood.app.feature.recipes.domain.repository.CookingRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Nombre: FinishCookingUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026 (revisado en noviembre de 2026, plan §6)
 * Descripción: Finaliza la preparación de una receta (spec 009, CA-RIA-04-05, 04-06, 04-08 y 04-09; Zhair #116):
 * 1. Si la operación [operationId] ya se aplicó, devuelve éxito sin escribir nada (CA-RIA-04-09).
 * 2. Lee la receta con [GetRecipeDetailUseCase] y el inventario con [ObserveInventoryUseCase].
 * 3. Calcula lo que se usa de cada producto con [RecipeEvaluator], en su unidad original: se borra si queda en 0 o no
 *    tenía cantidad, y si no, solo cambia su cantidad (el precio y la foto se conservan, CA-RIA-04-08).
 * 4. Calcula `rescuedValueCop` con los productos usados que vencen en 0 a 3 días (CA-RIA-04-06).
 * 5. Escribe los cambios y el [CookingRecord] en un solo lote con [CookingRepository.finishCooking]: o todo o nada.
 * 6. Encola el borrado de las fotos de los productos borrados, como `DeleteItemUseCase`.
 *
 * Devuelve [WriteOutcome.PENDING] si el lote quedó en la cola sin conexión (spec 006 §13), para mostrar
 * "Guardado en tu teléfono".
 */
class FinishCookingUseCase @Inject constructor(
    private val getRecipeDetail: GetRecipeDetailUseCase,
    private val observeInventory: ObserveInventoryUseCase,
    private val getPreferredStore: GetPreferredStoreUseCase,
    private val cookingRepository: CookingRepository,
    private val photoSync: PhotoSync,
    private val ingredientMatcher: IngredientMatcher,
    private val clock: Clock,
) {

    /** Id nuevo para un cierre; el ViewModel lo crea una vez y lo repite en cada intento (CA-RIA-04-09). */
    fun newOperationId(): String = cookingRepository.newOperationId()

    suspend operator fun invoke(recipeId: String, servings: Int, operationId: String): Result<WriteOutcome> {
        // Un reintento de un cierre que ya se aplicó no vuelve a descontar (CA-RIA-04-09).
        if (cookingRepository.isFinished(operationId)) return Result.success(WriteOutcome.SYNCED)

        val detail = getRecipeDetail(recipeId).getOrElse { return Result.failure(it) }
        val store = getPreferredStore()
        val inventoryItems = observeInventory().first().flatMap { it.items }
        val inventoryMap = inventoryItems.associateBy { it.id }

        val matchedStock = PantryStock.of(ingredientMatcher.match(inventoryItems, detail.ingredients))
        val evaluation = RecipeEvaluator(detail.ingredients).evaluate(
            recipe = detail.recipe,
            servings = servings,
            day = 1,
            stock = matchedStock,
            store = store
        )

        val today = LocalDate.now(clock)
        var totalRescuedCop = 0.0
        // Lo que se usa de cada producto, sumado por si dos ingredientes lo comparten; `null` es "todo el producto".
        val used = LinkedHashMap<String, Double?>()

        for (use in evaluation.uses) {
            val ingredient = detail.ingredients[use.ingredientId] ?: continue
            val packPrice = ingredient.priceAt(store).priceCop
            val packSize = ingredient.pack

            for ((itemId, takenBaseUnit) in use.fromPantry) {
                val item = inventoryMap[itemId] ?: continue
                val daysToExpire = ChronoUnit.DAYS.between(today, item.expiresOn).toInt()

                // Valor rescatado si vence en 0..3 días (CA-RIA-04-06).
                if (daysToExpire in 0..3) {
                    val amountUsedBase = takenBaseUnit ?: item.quantity?.amount ?: packSize
                    totalRescuedCop += (amountUsedBase * packPrice) / packSize
                }

                val quantity = item.quantity
                val usedInOriginalUnit = if (takenBaseUnit == null || quantity == null) {
                    null
                } else {
                    convertBaseUnitToOriginal(takenBaseUnit, quantity.unit)
                }
                used[itemId] = if (itemId in used) {
                    val previous = used[itemId]
                    if (previous == null || usedInOriginalUnit == null) null else previous + usedInOriginalUnit
                } else {
                    usedInOriginalUnit
                }
            }
        }

        val changes = used.map { (itemId, amount) -> change(inventoryMap.getValue(itemId), amount) }
        val record = CookingRecord(
            id = operationId,
            recipeId = recipeId,
            servings = servings,
            cookedAt = clock.millis(),
            rescuedValueCop = totalRescuedCop.toInt().coerceAtLeast(0)
        )

        return cookingRepository.finishCooking(record, changes).onSuccess {
            changes.filterIsInstance<InventoryChange.Delete>().forEach { (item) ->
                photoSync.cancelUpload(item.id)
                item.photo?.let { photoSync.enqueueDelete(it.path) }
            }
        }
    }

    /** Se borra si no tenía cantidad, se usó todo o queda en 0; si no, solo cambia la cantidad (CA-RIA-04-08). */
    private fun change(item: InventoryItem, usedAmount: Double?): InventoryChange {
        val quantity = item.quantity
        if (usedAmount == null || quantity == null) return InventoryChange.Delete(item)
        val remaining = roundAmount(quantity.amount - usedAmount)
        return if (remaining <= 0.001) {
            InventoryChange.Delete(item)
        } else {
            InventoryChange.UpdateQuantity(item, Quantity(remaining, quantity.unit))
        }
    }

    private fun convertBaseUnitToOriginal(amountBase: Double, originalUnit: QuantityUnit): Double {
        return when (originalUnit) {
            QuantityUnit.KILOGRAMS -> amountBase / 1000.0
            QuantityUnit.POUNDS -> amountBase / 453.59237
            QuantityUnit.LITERS -> amountBase / 1000.0
            else -> amountBase
        }
    }

    /** A dos decimales, como se mostraba antes en el inventario. */
    private fun roundAmount(value: Double): Double = Math.round(value * 100.0) / 100.0
}
