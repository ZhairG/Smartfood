package com.smartfood.app.feature.auth.data

import com.smartfood.app.feature.auth.data.demo.DemoPasswordRecoveryRepository
import com.smartfood.app.feature.auth.data.demo.DemoRecoveryData
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: DemoPasswordRecoveryRepositoryTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de la recuperación **simulada** con los datos de prueba de la spec (HU-04).
 */
class DemoPasswordRecoveryRepositoryTest {

    private val repository = DemoPasswordRecoveryRepository()
    private val demoEmail = DemoRecoveryData.REGISTERED_EMAILS.first()

    @Test
    fun `los datos de prueba coinciden con los documentados en la spec`() {
        assertEquals(setOf("demo@smartfood.com", "prueba@smartfood.com"), DemoRecoveryData.REGISTERED_EMAILS)
        assertEquals("1234", DemoRecoveryData.VALID_CODE)
    }

    @Test
    fun `CA-04-03 correo sin cuenta falla con AccountNotFound`() = runTest {
        assertEquals(AuthError.AccountNotFound, repository.requestCode("otro@correo.com").authError)
    }

    @Test
    fun `CA-04-04 correo de prueba solicita el codigo sin importar mayusculas`() = runTest {
        assertTrue(repository.requestCode("DEMO@smartfood.com").isSuccess)
    }

    @Test
    fun `CA-04-06 codigo distinto al de prueba falla con InvalidOtpCode`() = runTest {
        assertEquals(AuthError.InvalidOtpCode, repository.verifyCode(demoEmail, "9999").authError)
    }

    @Test
    fun `CA-04-07 codigo de prueba se verifica`() = runTest {
        assertTrue(repository.verifyCode(demoEmail, DemoRecoveryData.VALID_CODE).isSuccess)
    }

    @Test
    fun `CA-04-11 restablecer exige de nuevo el codigo valido`() = runTest {
        assertTrue(repository.resetPassword(demoEmail, DemoRecoveryData.VALID_CODE, "Secreta#1").isSuccess)
        assertEquals(AuthError.InvalidOtpCode, repository.resetPassword(demoEmail, "0000", "Secreta#1").authError)
    }
}
