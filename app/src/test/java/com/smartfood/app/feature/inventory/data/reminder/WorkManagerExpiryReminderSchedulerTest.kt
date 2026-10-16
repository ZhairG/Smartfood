package com.smartfood.app.feature.inventory.data.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Nombre: WorkManagerExpiryReminderSchedulerTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias del cálculo de la próxima 9:00 del aviso de vencimiento (CA-INV-08-01, HU-INV-08).
 * El resto de [WorkManagerExpiryReminderScheduler] depende de WorkManager y se verifica en la prueba instrumentada.
 */
class WorkManagerExpiryReminderSchedulerTest {

    private val zone: ZoneId = ZoneId.of("America/Bogota")

    private fun clockAt(hour: Int, minute: Int): Clock =
        Clock.fixed(LocalDateTime.of(2026, 10, 16, hour, minute).atZone(zone).toInstant(), zone)

    @Test
    fun `CA-INV-08-01 antes de las 9 el aviso llega a las 9 del mismo dia`() {
        val delay = WorkManagerExpiryReminderScheduler.delayUntilNextReminder(clockAt(8, 0))

        assertEquals(Duration.ofHours(1), delay)
    }

    @Test
    fun `CA-INV-08-01 a las 9 en punto el aviso pasa al dia siguiente`() {
        val delay = WorkManagerExpiryReminderScheduler.delayUntilNextReminder(clockAt(9, 0))

        assertEquals(Duration.ofHours(24), delay)
    }

    @Test
    fun `CA-INV-08-01 despues de las 9 el aviso llega manana a las 9`() {
        val delay = WorkManagerExpiryReminderScheduler.delayUntilNextReminder(clockAt(23, 30))

        assertEquals(Duration.ofHours(9).plusMinutes(30), delay)
    }
}
