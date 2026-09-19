package com.smartfood.app.domain.usecase.auth

import javax.inject.Inject

/**
 * Caso de uso responsable de validar si una contraseña cumple
 * con los criterios mínimos de seguridad de la aplicación.
 */
class ValidatePasswordUseCase @Inject constructor() {

    fun execute(password: String): PasswordValidationResult {
        return PasswordValidationResult(
            hasMinimumLength = password.length >= 8,
            hasUppercase = password.any { it.isUpperCase() },
            hasSpecialCharacter = password.any { !it.isLetterOrDigit() }
        )
    }
}

data class PasswordValidationResult(
    val hasMinimumLength: Boolean,
    val hasUppercase: Boolean,
    val hasSpecialCharacter: Boolean
) {
    val isValid: Boolean
        get() = hasMinimumLength && hasUppercase && hasSpecialCharacter
}
