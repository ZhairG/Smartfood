package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.testutil.fixedClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ClassifyExpiryUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ClassifyExpiryUseCase] en los bordes del umbral de 3 días (RF-INV-02).
 */
class ClassifyExpiryUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val classify = ClassifyExpiryUseCase(fixedClock(today))

    @Test
    fun `CA-INV-01-06 un producto que vencio ayer esta vencido`() {
        assertEquals(ExpiryStatus.EXPIRED, classify(LocalDate.of(2026, 9, 9)))
    }

    @Test
    fun `CA-INV-01-06 un producto que vence hoy esta proximo a vencer`() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, classify(LocalDate.of(2026, 9, 10)))
    }

    @Test
    fun `CA-INV-01-06 un producto que vence en 3 dias esta proximo a vencer`() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, classify(LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `CA-INV-01-06 un producto que vence en 4 dias esta fresco`() {
        assertEquals(ExpiryStatus.FRESH, classify(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `CA-INV-01-06 la clasificacion cruza meses y anios por dias de calendario`() {
        val newYearsEve = ClassifyExpiryUseCase(fixedClock(LocalDate.of(2026, 12, 30)))

        assertEquals(ExpiryStatus.EXPIRING_SOON, newYearsEve(LocalDate.of(2027, 1, 2)))
        assertEquals(ExpiryStatus.FRESH, newYearsEve(LocalDate.of(2027, 1, 3)))
    }
}
