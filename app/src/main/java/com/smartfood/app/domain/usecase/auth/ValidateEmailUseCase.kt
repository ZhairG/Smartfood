package com.smartfood.app.domain.usecase.auth

import javax.inject.Inject

/**
 * Caso de uso responsable de validar el formato de un correo electrónico antes de
 * enviarlo al servidor (prevención de errores: evita una llamada de red innecesaria).
 *
 * Es una validación de formato básica (usuario@dominio.tld); la existencia real de la cuenta
 * la determina el backend.
 */
class ValidateEmailUseCase @Inject constructor() {

    /** @return `true` si [email] (ignorando espacios en los extremos) tiene un formato válido. */
    fun execute(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    }
}
