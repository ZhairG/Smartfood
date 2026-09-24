package com.smartfood.app.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: ValidatePasswordUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para verificar los criterios de fortaleza de contraseña en [ValidatePasswordUseCase].
 */
class ValidatePasswordUseCaseTest {

    private lateinit var classUnderTest: ValidatePasswordUseCase

    @Before
    fun prepararEntornoTest() {
        classUnderTest = ValidatePasswordUseCase()
    }

    @Test
    fun `contrasena demasiado corta, falla la validacion`() {
        val result = classUnderTest.execute("Pass#")
        assertFalse(result.isValid)
        assertFalse(result.hasMinimumLength)
    }

    @Test
    fun `contrasena sin mayuscula, falla la validacion`() {
        val result = classUnderTest.execute("password#123")
        assertFalse(result.isValid)
        assertFalse(result.hasUppercase)
        assertTrue(result.hasMinimumLength)
    }

    @Test
    fun `contrasena sin caracter especial, falla la validacion`() {
        val result = classUnderTest.execute("Password123")
        assertFalse(result.isValid)
        assertFalse(result.hasSpecialCharacter)
    }

    @Test
    fun `contrasena cumple todos los criterios, pasa la validacion`() {
        val result = classUnderTest.execute("SecurePass#123")
        assertTrue(result.isValid)
        assertTrue(result.hasMinimumLength)
        assertTrue(result.hasUppercase)
        assertTrue(result.hasSpecialCharacter)
    }
}
