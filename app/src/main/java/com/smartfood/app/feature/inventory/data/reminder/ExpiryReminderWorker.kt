package com.smartfood.app.feature.inventory.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.usecase.GetExpiringItemsUseCase
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Nombre: ExpiryReminderWorker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Trabajo diario que publica el aviso de los productos por vencer (HU-INV-08, plan §6). Sin sesión no
 * avisa y termina bien (CA-INV-08-06); con sesión toma el inventario y los ajustes de notificaciones (spec 008,
 * HU-PER-04): si "Productos por vencer" está apagado no publica ese aviso (CA-PER-04-03), y si el inventario está vacío
 * publica "Inventario vacío" cuando ese aviso está encendido (CA-PER-04-04). Un fallo de red o del DataStore se
 * reintenta con WorkManager.
 *
 * @param appContext Contexto de la aplicación.
 * @param params Parámetros del trabajo.
 * @param authRepository Cuenta y sesión: indica si hay usuario con sesión activa.
 * @param inventoryRepository Inventario del usuario.
 * @param getExpiringItems Productos que vencen hoy o en los próximos 3 días.
 * @param notificationSettings Ajustes de notificaciones guardados en este teléfono.
 * @param notifier Canal y publicación de la notificación.
 */
@HiltWorker
class ExpiryReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val inventoryRepository: InventoryRepository,
    private val getExpiringItems: GetExpiringItemsUseCase,
    private val notificationSettings: NotificationSettingsRepository,
    private val notifier: ExpiryNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (authRepository.currentUserId() == null) {
            notifier.cancel()
            return Result.success()
        }
        return safeRunCatching {
            inventoryRepository.observeItems().first() to notificationSettings.observe().first()
        }.fold(
            onSuccess = { (items, settings) ->
                when {
                    items.isEmpty() && settings.emptyInventory -> notifier.notifyEmptyInventory()
                    items.isNotEmpty() && settings.expiringProducts ->
                        notifier.notifyExpiring(getExpiringItems(items))
                    else -> notifier.cancel()
                }
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        /** Nombre único del trabajo periódico (plan §6): lo usa `WorkManagerExpiryReminderScheduler` (T038). */
        const val UNIQUE_WORK_NAME = "expiry-reminder"
    }
}
