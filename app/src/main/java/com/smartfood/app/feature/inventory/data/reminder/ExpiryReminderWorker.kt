package com.smartfood.app.feature.inventory.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.usecase.GetExpiringItemsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Nombre: ExpiryReminderWorker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Trabajo diario que publica el aviso de los productos por vencer (HU-INV-08, plan §6). Sin sesión no
 * avisa y termina bien (CA-INV-08-06); con sesión toma el inventario, se queda con los productos que vencen hoy o en
 * los próximos 3 días y publica la notificación (CA-INV-08-01 a 08-03). Un fallo de red se reintenta con WorkManager.
 *
 * @param appContext Contexto de la aplicación.
 * @param params Parámetros del trabajo.
 * @param authRepository Cuenta y sesión: indica si hay usuario con sesión activa.
 * @param inventoryRepository Inventario del usuario.
 * @param getExpiringItems Productos que vencen hoy o en los próximos 3 días.
 * @param notifier Canal y publicación de la notificación.
 */
@HiltWorker
class ExpiryReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val inventoryRepository: InventoryRepository,
    private val getExpiringItems: GetExpiringItemsUseCase,
    private val notifier: ExpiryNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (authRepository.currentUserId() == null) {
            notifier.cancel()
            return Result.success()
        }
        return safeRunCatching { inventoryRepository.observeItems().first() }.fold(
            onSuccess = { items ->
                notifier.notifyExpiring(getExpiringItems(items))
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
