package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ObserveInventoryUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ObserveInventoryUseCase]: secciones, orden y cambios en vivo (HU-INV-01).
 */
class ObserveInventoryUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val repository = FakeInventoryRepository()
    private val photoSync = FakePhotoSync()
    private val observe = ObserveInventoryUseCase(repository, photoSync, ClassifyExpiryUseCase(fixedClock(today)))

    @Test
    fun `CA-INV-10-04 un producto con foto pendiente lleva la foto del telefono y queda pendiente`() = runTest {
        repository.items.value = listOf(
            inventoryItem(id = "leche", name = "Leche", expiresOn = today.plusDays(10)),
            inventoryItem(id = "pasta", name = "Pasta", expiresOn = today.plusDays(10)),
        )
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")

        val items = observe().first().single().items

        val leche = items.first { it.id == "leche" }
        assertEquals("/files/pending/leche/a.jpg", leche.localPhotoPath)
        assertTrue(leche.isPendingSync)
        val pasta = items.first { it.id == "pasta" }
        assertNull(pasta.localPhotoPath)
        assertFalse(pasta.isPendingSync)
    }

    @Test
    fun `CA-INV-10-05 cuando la foto se sube el producto deja de estar pendiente`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "leche", expiresOn = today.plusDays(10)))
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")
        assertTrue(observe().first().single().items.single().isPendingSync)

        photoSync.pending.value = emptyMap()

        assertFalse(observe().first().single().items.single().isPendingSync)
    }

    @Test
    fun `CA-INV-01-01 las secciones van en orden vencido, proximo a vencer y fresco`() = runTest {
        repository.items.value = listOf(
            inventoryItem(id = "pasta", name = "Pasta", expiresOn = today.plusDays(60)),
            inventoryItem(id = "leche", name = "Leche", expiresOn = today.plusDays(2)),
            inventoryItem(id = "yogur", name = "Yogur", expiresOn = today.minusDays(1)),
        )

        val sections = observe().first()

        assertEquals(
            listOf(ExpiryStatus.EXPIRED, ExpiryStatus.EXPIRING_SOON, ExpiryStatus.FRESH),
            sections.map { it.status }
        )
        assertEquals(listOf("yogur", "leche", "pasta"), sections.flatMap { s -> s.items.map { it.id } })
    }

    @Test
    fun `CA-INV-01-01 las secciones vacias no se muestran`() = runTest {
        repository.items.value = listOf(inventoryItem(expiresOn = today.plusDays(10)))

        val sections = observe().first()

        assertEquals(listOf(ExpiryStatus.FRESH), sections.map { it.status })
    }

    @Test
    fun `CA-INV-01-01 sin productos no hay secciones`() = runTest {
        assertTrue(observe().first().isEmpty())
    }

    @Test
    fun `CA-INV-01-04 dentro de una seccion se ordena por fecha y a igual fecha por nombre`() = runTest {
        repository.items.value = listOf(
            inventoryItem(id = "3", name = "Tomate", expiresOn = today.plusDays(20)),
            inventoryItem(id = "2", name = "pollo", expiresOn = today.plusDays(10)),
            inventoryItem(id = "1", name = "Carne", expiresOn = today.plusDays(10)),
        )

        val fresh = observe().first().single()

        assertEquals(listOf("Carne", "pollo", "Tomate"), fresh.items.map { it.name })
    }

    @Test
    fun `CA-INV-01-07 un cambio en el repositorio emite las secciones de nuevo`() = runTest {
        repository.items.value = listOf(inventoryItem(id = "a", expiresOn = today.plusDays(10)))
        assertEquals(1, observe().first().single().items.size)

        repository.createItem(inventoryItem(id = "b", name = "Arroz", expiresOn = today.plusDays(30)))

        assertEquals(2, observe().first().single().items.size)
    }
}
