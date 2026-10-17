package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: HasPendingChangesUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [HasPendingChangesUseCase] (spec 006 §13, CA-INV-10-07, T046): hay cambios
 * pendientes si quedan escrituras sin confirmar o fotos sin subir.
 */
class HasPendingChangesUseCaseTest {

    private val repository = FakeInventoryRepository()
    private val photoSync = FakePhotoSync()
    private val hasPendingChanges = HasPendingChangesUseCase(repository, photoSync)

    @Test
    fun `CA-INV-10-07 sin escrituras ni fotos pendientes no hay cambios sin subir`() = runTest {
        assertFalse(hasPendingChanges())
    }

    @Test
    fun `CA-INV-10-07 una escritura sin confirmar es un cambio pendiente`() = runTest {
        repository.pendingWrites = true

        assertTrue(hasPendingChanges())
    }

    @Test
    fun `CA-INV-10-07 una foto sin subir es un cambio pendiente`() = runTest {
        photoSync.pending.value = mapOf("leche" to "/files/pending/leche/a.jpg")

        assertTrue(hasPendingChanges())
    }
}
