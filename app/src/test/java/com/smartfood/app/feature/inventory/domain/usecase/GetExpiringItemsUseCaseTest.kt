package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: GetExpiringItemsUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [GetExpiringItemsUseCase] (HU-INV-08).
 */
class GetExpiringItemsUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val getExpiring = GetExpiringItemsUseCase(ClassifyExpiryUseCase(fixedClock(today)))

    @Test
    fun `CA-INV-08-01 solo entran los productos que vencen hoy o en los proximos 3 dias`() {
        val items = listOf(
            inventoryItem(id = "vencido", expiresOn = today.minusDays(1)),
            inventoryItem(id = "hoy", expiresOn = today),
            inventoryItem(id = "en-3", expiresOn = today.plusDays(3)),
            inventoryItem(id = "en-4", expiresOn = today.plusDays(4)),
        )

        assertEquals(listOf("hoy", "en-3"), getExpiring(items).map { it.id })
    }

    @Test
    fun `CA-INV-08-02 el producto mas proximo a vencer va primero`() {
        val items = listOf(
            inventoryItem(id = "pan", name = "Pan", expiresOn = today.plusDays(2)),
            inventoryItem(id = "yogur", name = "Yogur", expiresOn = today.plusDays(1)),
            inventoryItem(id = "leche", name = "Leche Entera", expiresOn = today.plusDays(1)),
        )

        assertEquals(listOf("leche", "yogur", "pan"), getExpiring(items).map { it.id })
    }

    @Test
    fun `CA-INV-08-03 sin productos por vencer la lista queda vacia`() {
        val items = listOf(
            inventoryItem(id = "vencido", expiresOn = today.minusDays(5)),
            inventoryItem(id = "fresco", expiresOn = today.plusDays(30)),
        )

        assertTrue(getExpiring(items).isEmpty())
        assertTrue(getExpiring(emptyList()).isEmpty())
    }
}
