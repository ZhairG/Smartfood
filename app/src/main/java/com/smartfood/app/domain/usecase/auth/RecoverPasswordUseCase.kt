package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Nombre: RecoverPasswordUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso para solicitar la recuperación de contraseña de una cuenta.
 *
 * @param authRepository Repositorio de autenticación.
 */
class RecoverPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Solicita al repositorio el envío del correo de restablecimiento de contraseña.
     *
     * @param email Correo electrónico de la cuenta a recuperar.
     * @return [Result] con el estado de la solicitud.
     */
    suspend fun execute(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El correo electrónico no puede estar vacío."))
        }
        return authRepository.sendPasswordReset(email)
    }
}
