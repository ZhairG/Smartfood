package com.smartfood.app.feature.auth.domain.usecase

import javax.inject.Inject

/**
 * Nombre: ValidateEmailUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Caso de uso responsable de validar la estructura sintáctica de un correo electrónico.
 * Previene llamadas de red innecesarias ante entradas mal formateadas.
 */
class ValidateEmailUseCase @Inject constructor() {

    /**
     * Valida si el formato de una dirección de correo electrónico es sintácticamente correcto.
     *
     * @param email Cadena de texto del correo electrónico a evaluar.
     * @return `true` si el correo cumple con el patrón regex estándar; `false` en caso contrario.
     */
    fun execute(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    }
}
