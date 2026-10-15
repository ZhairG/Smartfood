package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Nombre: ClassifyExpiryUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Clasifica un producto según los días que faltan para su vencimiento (RF-INV-02, CA-INV-01-06).
 *
 * @param clock Reloj del que se toma "hoy", en la zona horaria del teléfono.
 */
class ClassifyExpiryUseCase @Inject constructor(
    private val clock: Clock
) {
    /**
     * @return [ExpiryStatus.EXPIRED] si la fecha ya pasó, [ExpiryStatus.EXPIRING_SOON] si vence hoy o en los
     * próximos [EXPIRING_SOON_DAYS] días, y [ExpiryStatus.FRESH] en otro caso.
     */
    operator fun invoke(expiresOn: LocalDate): ExpiryStatus {
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(clock), expiresOn)
        return when {
            daysLeft < 0 -> ExpiryStatus.EXPIRED
            daysLeft <= EXPIRING_SOON_DAYS -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.FRESH
        }
    }

    companion object {
        /** Días restantes hasta los que un producto está "Próximo a vencer" (spec §3). */
        const val EXPIRING_SOON_DAYS = 3L
    }
}
