package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import javax.inject.Inject

/**
 * Nombre: PasswordRecoveryUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso de los 3 pasos de la recuperación de contraseña (HU-04).
 * Las validaciones de formato viven aquí; la verificación del código la hace el [PasswordRecoveryRepository].
 */

/**
 * Paso 1: valida el correo y solicita el código (CA-04-01 a CA-04-04).
 *
 * @param recoveryRepository Repositorio de recuperación.
 * @param validateEmailUseCase Validador del formato de correo.
 */
class RequestRecoveryCodeUseCase @Inject constructor(
    private val recoveryRepository: PasswordRecoveryRepository,
    private val validateEmailUseCase: ValidateEmailUseCase
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) return authFailure(AuthError.EmptyFields(setOf(AuthField.EMAIL)))
        if (!validateEmailUseCase.execute(trimmedEmail)) return authFailure(AuthError.InvalidEmail)
        return recoveryRepository.requestCode(trimmedEmail)
    }
}

/**
 * Paso 2: valida que el código tenga 4 dígitos y lo verifica (CA-04-05 a CA-04-07).
 *
 * @param recoveryRepository Repositorio de recuperación.
 */
class VerifyRecoveryCodeUseCase @Inject constructor(
    private val recoveryRepository: PasswordRecoveryRepository
) {
    suspend operator fun invoke(email: String, code: String): Result<Unit> {
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) return authFailure(AuthError.EmptyFields(setOf(AuthField.OTP_CODE)))
        if (trimmedCode.length != CODE_LENGTH || !trimmedCode.all(Char::isDigit)) {
            return authFailure(AuthError.InvalidOtpCode)
        }
        return recoveryRepository.verifyCode(email.trim(), trimmedCode)
    }

    companion object {
        /** Cantidad de dígitos del código de verificación. */
        const val CODE_LENGTH = 4
    }
}

/**
 * Paso 3: valida la nueva contraseña y la restablece (CA-04-10 y CA-04-11).
 *
 * @param recoveryRepository Repositorio de recuperación.
 * @param validatePasswordUseCase Validador de la política de contraseña.
 */
class ResetPasswordUseCase @Inject constructor(
    private val recoveryRepository: PasswordRecoveryRepository,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        if (!validatePasswordUseCase.execute(newPassword).isValid) return authFailure(AuthError.WeakPassword)
        if (newPassword != confirmPassword) return authFailure(AuthError.PasswordsDontMatch)
        return recoveryRepository.resetPassword(email.trim(), code.trim(), newPassword)
    }
}
