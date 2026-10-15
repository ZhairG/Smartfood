package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.InventorySection
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: ObserveInventoryUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Observa el inventario agrupado por estado de vencimiento (HU-INV-01).
 *
 * @param repository Repositorio de productos.
 * @param classifyExpiry Clasificación de cada producto.
 */
class ObserveInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val classifyExpiry: ClassifyExpiryUseCase
) {
    /**
     * @return Las secciones en el orden Vencido, Próximo a vencer y Fresco, sin secciones vacías (CA-INV-01-01).
     * Dentro de cada sección, los productos van por fecha de vencimiento y, a igual fecha, por nombre
     * (CA-INV-01-04).
     */
    operator fun invoke(): Flow<List<InventorySection>> = repository.observeItems().map { items ->
        val byStatus = items.groupBy { classifyExpiry(it.expiresOn) }
        ExpiryStatus.entries.mapNotNull { status ->
            byStatus[status]?.let { InventorySection(status, it.sortedWith(BY_EXPIRY_THEN_NAME)) }
        }
    }

    private companion object {
        val BY_EXPIRY_THEN_NAME: Comparator<InventoryItem> =
            compareBy<InventoryItem> { it.expiresOn }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
