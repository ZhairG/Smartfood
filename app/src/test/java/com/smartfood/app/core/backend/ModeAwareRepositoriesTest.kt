package com.smartfood.app.core.backend

import com.smartfood.app.feature.auth.data.repository.ModeAwarePasswordRecoveryRepository
import com.smartfood.app.feature.profile.data.repository.ModeAwareEmailChangeRepository
import com.smartfood.app.testutil.FakeEmailChangeRepository
import com.smartfood.app.testutil.FakeBackendModeProvider
import com.smartfood.app.testutil.FakePasswordRecoveryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ModeAwareRepositoriesTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Los repositorios que se registran en Hilt delegan según el interruptor del backend (spec 012,
 * RF-SPK-02, CA-SPK-01-01 y CA-SPK-01-02).
 */
class ModeAwareRepositoriesTest {

    @Test
    fun `CA-SPK-01-01 CA-SPK-01-02 la recuperacion usa el codigo en Blaze y el enlace en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val codigo = FakePasswordRecoveryRepository(usesCode = true)
        val enlace = FakePasswordRecoveryRepository(usesCode = false)
        val repository = ModeAwarePasswordRecoveryRepository(mode, blaze = codigo, free = enlace)

        assertFalse(repository.usesCode)
        repository.requestCode("demo@smartfood.com")
        assertEquals(1, enlace.requestCodeCalls)

        mode.mode = BackendMode.BLAZE
        assertTrue(repository.usesCode)
        repository.requestCode("demo@smartfood.com")
        assertEquals(1, codigo.requestCodeCalls)
    }

    @Test
    fun `CA-SPK-03-01 el cambio de correo usa el codigo en Blaze y el enlace en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val codigo = FakeEmailChangeRepository(usesCode = true)
        val enlace = FakeEmailChangeRepository(usesCode = false)
        val repository = ModeAwareEmailChangeRepository(mode, blaze = codigo, free = enlace)

        assertFalse(repository.usesCode)
        repository.requestCode("nuevo@correo.com")
        assertEquals(listOf("nuevo@correo.com"), enlace.requested)

        mode.mode = BackendMode.BLAZE
        assertTrue(repository.usesCode)
        repository.requestCode("otro@correo.com")
        assertEquals(listOf("otro@correo.com"), codigo.requested)
    }
}
