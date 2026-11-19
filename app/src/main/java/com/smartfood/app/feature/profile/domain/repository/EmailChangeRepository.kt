package com.smartfood.app.feature.profile.domain.repository

import kotlin.time.Duration

/**
 * Nombre: EmailChangeRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Cambio de correo con código (spec 008, T009b, RF-PER-03). El servidor envía un código de 4 dígitos al
 * correo nuevo y, al confirmarlo, cambia la cuenta y el perfil sin cerrar la sesión.
 */
interface EmailChangeRepository {

    /**
     * `true` si el cambio se confirma con el código de 4 dígitos; `false` si [requestCode] envía un enlace de Firebase
     * y el correo cambia al abrirlo (spec 012, modo gratuito, CA-SPK-03-01).
     */
    val usesCode: Boolean get() = true

    /**
     * Envía el código a [newEmail]. Requiere un inicio de sesión reciente (reautenticar justo antes).
     *
     * @return Espera antes de poder pedir otro código.
     */
    suspend fun requestCode(newEmail: String): Result<Duration>

    /**
     * Comprueba el código y cambia el correo de la cuenta y del perfil.
     *
     * @return Correo nuevo de la cuenta.
     */
    suspend fun confirm(code: String): Result<String>
}
