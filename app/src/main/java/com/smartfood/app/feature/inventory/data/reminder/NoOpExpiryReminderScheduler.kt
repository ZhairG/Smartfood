package com.smartfood.app.feature.inventory.data.reminder

import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import javax.inject.Inject

/**
 * Nombre: NoOpExpiryReminderScheduler.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación temporal de [ExpiryReminderScheduler] que no hace nada. Existe para que Hilt pueda
 * construir `LogoutUseCase` (T013) mientras no hay avisos reales. Se borra en T038, cuando
 * `WorkManagerExpiryReminderScheduler` la reemplace en `InventoryDataModule`.
 */
class NoOpExpiryReminderScheduler @Inject constructor() : ExpiryReminderScheduler {
    override fun schedule() = Unit
    override fun cancel() = Unit
}
