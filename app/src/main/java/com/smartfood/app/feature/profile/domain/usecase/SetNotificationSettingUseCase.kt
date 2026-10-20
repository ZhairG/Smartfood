package com.smartfood.app.feature.profile.domain.usecase

import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import javax.inject.Inject

/**
 * Nombre: SetNotificationSettingUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Guarda los ajustes de notificaciones y programa o cancela el trabajo diario (spec 008, T015,
 * CA-PER-04-03). El aviso diario sirve para "Productos por vencer" y para "Inventario vacío", así que se mantiene
 * programado mientras cualquiera de los dos esté encendido y solo se cancela cuando ambos quedan apagados. "Recetas
 * sugeridas" solo se guarda (CA-PER-04-05).
 *
 * @param repository Ajustes guardados en este teléfono.
 * @param scheduler Programa o cancela el aviso diario del inventario.
 */
class SetNotificationSettingUseCase @Inject constructor(
    private val repository: NotificationSettingsRepository,
    private val scheduler: ExpiryReminderScheduler
) {

    /** Guarda [settings] y ajusta el trabajo diario según los dos avisos que lo usan (CA-PER-04-03). */
    suspend operator fun invoke(settings: NotificationSettings) {
        repository.set(settings)
        if (settings.expiringProducts || settings.emptyInventory) {
            scheduler.schedule()
        } else {
            scheduler.cancel()
        }
    }
}
