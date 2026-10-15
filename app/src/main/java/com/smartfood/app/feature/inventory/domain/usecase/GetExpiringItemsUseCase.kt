package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import javax.inject.Inject

/**
 * Nombre: GetExpiringItemsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Elige los productos del aviso diario de vencimiento (HU-INV-08).
 *
 * @param classifyExpiry Clasificación de cada producto.
 */
class GetExpiringItemsUseCase @Inject constructor(
    private val classifyExpiry: ClassifyExpiryUseCase
) {
    /**
     * @return Solo los productos que vencen hoy o en los próximos 3 días (CA-INV-08-01), con el más próximo primero,
     * que es el que nombra la notificación (CA-INV-08-02). Vacía si no hay ninguno: ese día no se avisa
     * (CA-INV-08-03).
     */
    operator fun invoke(items: List<InventoryItem>): List<InventoryItem> =
        items.filter { classifyExpiry(it.expiresOn) == ExpiryStatus.EXPIRING_SOON }
            .sortedWith(compareBy<InventoryItem> { it.expiresOn }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}
