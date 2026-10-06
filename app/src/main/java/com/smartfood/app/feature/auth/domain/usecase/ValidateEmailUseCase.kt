package com.smartfood.app.feature.auth.domain.usecase

import javax.inject.Inject

/**
 * <build>
 * <b>Nombre:</b> ValidateEmailUseCase.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 24/09/2026<br>
 * <b>Descripción:</b> Caso de uso responsable de validar la estructura sintáctica de un correo electrónico.<br>
 * Previene llamadas de red innecesarias ante entradas mal formateadas.<br>
 * </build>
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
