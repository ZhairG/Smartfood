package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.testutil.FakePasswordRecoveryRepository
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> PasswordRecoveryUseCasesTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de las validaciones de los 3 pasos de la recuperación (HU-04).<br>
 * </build>
 */
class PasswordRecoveryUseCasesTest {

    private val repository = FakePasswordRecoveryRepository()
    private val requestCode = RequestRecoveryCodeUseCase(repository, ValidateEmailUseCase())
    private val verifyCode = VerifyRecoveryCodeUseCase(repository)
    private val resetPassword = ResetPasswordUseCase(repository, ValidatePasswordUseCase())

    @Test
    fun `CA-04-01 correo vacio falla con EmptyFields de EMAIL`() = runTest {
        assertEquals(AuthError.EmptyFields(setOf(AuthField.EMAIL)), requestCode("  ").authError)
        assertEquals(0, repository.requestCodeCalls)
    }

    @Test
    fun `CA-04-02 correo mal formado falla con InvalidEmail`() = runTest {
        assertEquals(AuthError.InvalidEmail, requestCode("demo@").authError)
    }

    @Test
    fun `CA-04-04 correo valido solicita el codigo al repositorio`() = runTest {
        assertTrue(requestCode("demo@smartfood.com").isSuccess)
        assertEquals(1, repository.requestCodeCalls)
    }

    @Test
    fun `CA-04-05 codigo vacio falla con EmptyFields de OTP_CODE`() = runTest {
        assertEquals(AuthError.EmptyFields(setOf(AuthField.OTP_CODE)), verifyCode("demo@smartfood.com", "").authError)
    }

    @Test
    fun `CA-04-06 codigo incompleto o no numerico falla sin consultar el repositorio`() = runTest {
        assertEquals(AuthError.InvalidOtpCode, verifyCode("demo@smartfood.com", "12").authError)
        assertEquals(AuthError.InvalidOtpCode, verifyCode("demo@smartfood.com", "12a4").authError)
        assertEquals(0, repository.verifyCodeCalls)
    }

    @Test
    fun `CA-04-07 codigo de 4 digitos se verifica en el repositorio`() = runTest {
        assertTrue(verifyCode("demo@smartfood.com", "1234").isSuccess)
        assertEquals(1, repository.verifyCodeCalls)
    }

    @Test
    fun `CA-04-10 contrasena debil falla con WeakPassword`() = runTest {
        assertEquals(AuthError.WeakPassword, resetPassword("demo@smartfood.com", "1234", "debil", "debil").authError)
    }

    @Test
    fun `CA-04-10 confirmacion distinta falla con PasswordsDontMatch`() = runTest {
        val result = resetPassword("demo@smartfood.com", "1234", "Secreta#1", "Secreta#2")

        assertEquals(AuthError.PasswordsDontMatch, result.authError)
        assertEquals(0, repository.resetPasswordCalls)
    }

    @Test
    fun `CA-04-11 contrasena valida y confirmada se restablece`() = runTest {
        assertTrue(resetPassword("demo@smartfood.com", "1234", "Secreta#1", "Secreta#1").isSuccess)
        assertEquals(1, repository.resetPasswordCalls)
    }
}
