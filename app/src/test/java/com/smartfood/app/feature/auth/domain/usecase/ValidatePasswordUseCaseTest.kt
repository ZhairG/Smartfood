package com.smartfood.app.feature.auth.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> ValidatePasswordUseCaseTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 24/09/2026<br>
 * <b>Descripción:</b> Pruebas unitarias para verificar los criterios de fortaleza de contraseña en<br>
 * [ValidatePasswordUseCase].<br>
 * </build>
 */
class ValidatePasswordUseCaseTest {

    private lateinit var classUnderTest: ValidatePasswordUseCase

    @Before
    fun prepararEntornoTest() {
        classUnderTest = ValidatePasswordUseCase()
    }

    @Test
    fun `RF-AUTH-04 contrasena demasiado corta, falla la validacion`() {
        val result = classUnderTest.execute("Pass#")
        assertFalse(result.isValid)
        assertFalse(result.hasMinimumLength)
    }

    @Test
    fun `RF-AUTH-04 contrasena sin mayuscula, falla la validacion`() {
        val result = classUnderTest.execute("password#123")
        assertFalse(result.isValid)
        assertFalse(result.hasUppercase)
        assertTrue(result.hasMinimumLength)
    }

    @Test
    fun `RF-AUTH-04 contrasena sin caracter especial, falla la validacion`() {
        val result = classUnderTest.execute("Password123")
        assertFalse(result.isValid)
        assertFalse(result.hasSpecialCharacter)
    }

    @Test
    fun `RF-AUTH-04 contrasena cumple todos los criterios, pasa la validacion`() {
        val result = classUnderTest.execute("SecurePass#123")
        assertTrue(result.isValid)
        assertTrue(result.hasMinimumLength)
        assertTrue(result.hasUppercase)
        assertTrue(result.hasSpecialCharacter)
    }
}
