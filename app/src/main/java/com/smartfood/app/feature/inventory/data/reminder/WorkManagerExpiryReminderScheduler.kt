package com.smartfood.app.feature.inventory.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: WorkManagerExpiryReminderScheduler.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Programa con WorkManager el aviso diario de vencimiento a las 9:00 (hora del teléfono, CA-INV-08-01).
 * El trabajo es periódico, único y de 24 horas; el primer disparo se retrasa hasta la próxima 9:00 y, si ya existe,
 * se reemplaza (`UPDATE`) en lugar de duplicarse. Cerrar sesión lo cancela (CA-INV-08-06).
 *
 * @param context Contexto de la aplicación, para obtener el `WorkManager`.
 * @param clock Reloj del teléfono: calcula cuánto falta para la próxima 9:00. Las pruebas inyectan un reloj fijo.
 */
@Singleton
class WorkManagerExpiryReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : ExpiryReminderScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNextReminder(clock).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ExpiryReminderWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(ExpiryReminderWorker.UNIQUE_WORK_NAME)
    }

    companion object {

        /** Hora del teléfono a la que se publica el aviso (CA-INV-08-01). */
        const val REMINDER_HOUR = 9

        /** Cada cuánto se repite el aviso, en horas: una vez al día (CA-INV-08-01). */
        private const val PERIOD_HOURS = 24L

        /**
         * Tiempo que falta para la próxima 9:00 en la hora del teléfono (CA-INV-08-01). Si ya pasó la hora de hoy
         * (o son exactamente las 9:00), apunta a la de mañana.
         *
         * @param clock Reloj del teléfono.
         * @return Diferencia entre ahora y la próxima 9:00.
         */
        fun delayUntilNextReminder(clock: Clock): Duration {
            val now = ZonedDateTime.now(clock)
            val today = now.withHour(REMINDER_HOUR).withMinute(0).withSecond(0).withNano(0)
            val next = if (today.isAfter(now)) today else today.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
