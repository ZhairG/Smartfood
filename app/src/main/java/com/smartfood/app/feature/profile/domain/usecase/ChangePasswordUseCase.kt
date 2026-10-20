package com.smartfood.app.feature.profile.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import javax.inject.Inject

/**
 * Nombre: ChangePasswordUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Caso de uso para actualizar la contraseña de la cuenta confirmando la actual (HU-PER-05, CA-PER-05-04).
 * Reautentica primero con la contraseña actual, valida la nueva contraseña y actualiza la credencial en Firebase Auth.
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
) {
    /**
     * Ejecuta el flujo de cambio de contraseña.
     *
     * @param currentPassword Contraseña actual.
     * @param newPassword Nueva contraseña.
     * @param confirmPassword Confirmación de la nueva contraseña.
     * @return `Result.success(Unit)` si la contraseña fue actualizada con éxito, o `Result.failure(AuthException)` con el error.
     */
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<Unit> {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return authFailure(AuthError.EmptyFields(setOf(AuthField.PASSWORD)))
        }
        if (newPassword != confirmPassword) {
            return authFailure(AuthError.PasswordsDontMatch)
        }
        val validation = validatePasswordUseCase.execute(newPassword)
        if (!validation.isValid) {
            return authFailure(AuthError.WeakPassword)
        }
        val reauthResult = authRepository.reauthenticate(currentPassword)
        if (reauthResult.isFailure) {
            return reauthResult
        }
        return authRepository.updatePassword(newPassword)
    }
}
