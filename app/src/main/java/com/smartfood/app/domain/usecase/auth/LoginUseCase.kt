package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Nombre: LoginUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso para la ejecución del inicio de sesión de usuario.
 *
 * @param authRepository Repositorio de autenticación.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Valida precondiciones y solicita el inicio de sesión al repositorio de autenticación.
     *
     * @param email Correo electrónico del usuario.
     * @param pass Contraseña del usuario.
     * @return [Result] que encapsula el éxito de la operación o la excepción en caso de fallo.
     */
    suspend fun execute(email: String, pass: String): Result<Unit> {
        if (email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("El correo y la contraseña no pueden estar vacíos."))
        }
        return authRepository.login(email, pass)
    }
}
