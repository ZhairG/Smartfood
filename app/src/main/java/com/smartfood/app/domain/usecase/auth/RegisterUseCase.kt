package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    suspend fun execute(email: String, pass: String, confirmPass: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El email no puede estar vacío."))
        }
        if (pass != confirmPass) {
            return Result.failure(IllegalArgumentException("Las contraseñas no coinciden."))
        }
        val validationResult = validatePasswordUseCase.execute(pass)
        if (!validationResult.isValid) {
            return Result.failure(IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad."))
        }
        return authRepository.register(email, pass)
    }
}
