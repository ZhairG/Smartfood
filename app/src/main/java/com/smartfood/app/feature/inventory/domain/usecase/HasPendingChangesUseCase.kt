package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import javax.inject.Inject

/**
 * Nombre: HasPendingChangesUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Dice si hay cambios del inventario que todavía no se suben: escrituras sin confirmar o fotos
 * esperando. El Perfil lo consulta antes de cerrar sesión (spec 006 §13, CA-INV-10-07, T046).
 *
 * @param repository Repositorio de productos.
 * @param photoSync Fotos que esperan subirse.
 */
class HasPendingChangesUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoSync: PhotoSync
) {
    /** @return `true` si hay escrituras o fotos sin subir. */
    suspend operator fun invoke(): Boolean = photoSync.hasPending() || repository.hasPendingWrites()
}
