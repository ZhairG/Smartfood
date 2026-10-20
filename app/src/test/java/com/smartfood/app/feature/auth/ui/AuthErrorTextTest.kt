package com.smartfood.app.feature.auth.ui

import com.smartfood.app.R
import com.smartfood.app.feature.auth.domain.model.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: AuthErrorTextTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [messageRes] para los errores del Perfil (spec 008, T004): cada error nuevo tiene
 * su propio texto en `strings_profile.xml`.
 */
class AuthErrorTextTest {

    @Test
    fun `CA-PER-02-05 CA-PER-02-06 CA-PER-02-07 cada error del perfil tiene su mensaje`() {
        listOf(
            AuthError.InvalidName to R.string.error_invalid_name,
            AuthError.InvalidPhone to R.string.error_invalid_phone,
            AuthError.InvalidBirthDate to R.string.error_invalid_birth_date,
            AuthError.InvalidCredentials to R.string.error_invalid_credentials,
        ).forEach { (error, res) -> assertEquals("$error", res, error.messageRes()) }
    }
}
