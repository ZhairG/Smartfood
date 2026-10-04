package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Nombre: ResetPasswordUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso para gestionar las 3 etapas del flujo de recuperación de contraseña:
 * 1) Envío real del correo y generación de código OTP mediante Firebase Auth.
 * 2) Verificación del código OTP de 4 dígitos.
 * 3) Restablecimiento y actualización de contraseña en Firebase Auth.
 *
 * @param authRepository Repositorio de autenticación.
 * @param validateEmailUseCase Caso de uso para validación de formato de correo.
 * @param validatePasswordUseCase Caso de uso para validación de formato de contraseña.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Solicita el envío del correo de recuperación vía Firebase y genera un código OTP.
     *
     * @param email Correo electrónico ingresado.
     * @return [Result] que entrega el código OTP generado o falla de validación/red.
     */
    suspend fun sendResetCode(email: String): Result<String> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("EMPTY_EMAIL"))
        }
        if (!validateEmailUseCase.execute(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("INVALID_EMAIL"))
        }
        return authRepository.sendPasswordReset(trimmedEmail)
    }

    /**
     * Valida la corrección del código OTP de 4 dígitos ingresado comparándolo con el esperado.
     *
     * @param code Código ingresado por el usuario.
     * @param expectedOtp Código OTP asignado para la sesión actual.
     * @return [Result] exitoso si coincide; en caso contrario devuelve error clasificado.
     */
    fun verifyOtpCode(code: String, expectedOtp: String? = null): Result<Unit> {
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            return Result.failure(IllegalArgumentException("EMPTY_OTP_CODE"))
        }
        if (trimmedCode.length < 4 || !trimmedCode.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("INVALID_OTP_CODE"))
        }
        if (expectedOtp != null && expectedOtp.isNotBlank() && trimmedCode != expectedOtp) {
            return Result.failure(IllegalArgumentException("INVALID_OTP_CODE"))
        }
        return Result.success(Unit)
    }

    /**
     * Actualiza la contraseña en Firebase Auth validando requisitos de fortaleza.
     *
     * @param email Correo del usuario.
     * @param code Código OTP utilizado.
     * @param newPass Nueva contraseña.
     * @param confirmPass Confirmación de contraseña.
     * @return [Result] que encapsula el éxito de la actualización en Firebase Auth.
     */
    suspend fun resetPassword(
        email: String,
        code: String,
        newPass: String,
        confirmPass: String
    ): Result<Unit> {
        val validation = validatePasswordUseCase.execute(newPass)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException("VALIDATION_ERROR"))
        }
        if (newPass != confirmPass) {
            return Result.failure(IllegalArgumentException("PASSWORDS_DONT_MATCH"))
        }
        return authRepository.confirmPasswordReset(email, code, newPass)
    }
}
