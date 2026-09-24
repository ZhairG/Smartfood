package com.smartfood.app.domain.usecase.auth

import javax.inject.Inject

/**
 * Nombre: ValidatePasswordUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso que evalúa las reglas de seguridad mínimas para las contraseñas.
 */
class ValidatePasswordUseCase @Inject constructor() {

    /**
     * Evalúa una cadena de texto de contraseña frente a las reglas de fortaleza requeridas.
     *
     * @param password Contraseña a evaluar.
     * @return Objeto [PasswordValidationResult] con el desglose de cada regla.
     */
    fun execute(password: String): PasswordValidationResult {
        return PasswordValidationResult(
            hasMinimumLength = password.length >= 8,
            hasUppercase = password.any { it.isUpperCase() },
            hasSpecialCharacter = password.any { !it.isLetterOrDigit() }
        )
    }
}

/**
 * Resultado detallado de la validación de contraseña.
 *
 * @property hasMinimumLength Cumple con la longitud de al menos 8 caracteres.
 * @property hasUppercase Contiene al menos una letra mayúscula.
 * @property hasSpecialCharacter Contiene al menos un carácter especial o símbolo.
 * @property isValid Indica si la contraseña cumple con la totalidad de los criterios requeridos.
 */
data class PasswordValidationResult(
    val hasMinimumLength: Boolean,
    val hasUppercase: Boolean,
    val hasSpecialCharacter: Boolean
) {
    val isValid: Boolean
        get() = hasMinimumLength && hasUppercase && hasSpecialCharacter
}
