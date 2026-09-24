package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Nombre: RegisterUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso para la creación y registro de nuevas cuentas de usuario.
 *
 * @param authRepository Repositorio de autenticación.
 * @param validatePasswordUseCase Caso de uso para la verificación de fortaleza de contraseña.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Valida la coherencia y fortaleza de los datos e inicia la creación de la cuenta.
     *
     * @param email Correo electrónico a registrar.
     * @param pass Contraseña elegida.
     * @param confirmPass Confirmación de contraseña.
     * @return [Result] con el resultado de la operación.
     */
    suspend fun execute(email: String, pass: String, confirmPass: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El correo electrónico no puede estar vacío."))
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
