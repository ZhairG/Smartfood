package com.smartfood.app.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: ValidatePasswordUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para asegurar que los criterios de la contraseña 
 * funcionen independientemente de la Interfaz de Usuario o del Framework.
 */
class ValidatePasswordUseCaseTest {

    private lateinit var classUnderTest: ValidatePasswordUseCase

    @Before
    fun setUp() {
        classUnderTest = ValidatePasswordUseCase()
    }

    @Test
    fun `password too short, fails validation`() {
        val result = classUnderTest.execute("Pass#")
        assertFalse(result.isValid)
        assertFalse(result.hasMinimumLength)
    }

    @Test
    fun `password without uppercase, fails validation`() {
        val result = classUnderTest.execute("password#123")
        assertFalse(result.isValid)
        assertFalse(result.hasUppercase)
        assertTrue(result.hasMinimumLength)
    }

    @Test
    fun `password without special char, fails validation`() {
        val result = classUnderTest.execute("Password123")
        assertFalse(result.isValid)
        assertFalse(result.hasSpecialCharacter)
    }

    @Test
    fun `password meets all criteria, passes validation`() {
        val result = classUnderTest.execute("SecurePass#123")
        assertTrue(result.isValid)
        assertTrue(result.hasMinimumLength)
        assertTrue(result.hasUppercase)
        assertTrue(result.hasSpecialCharacter)
    }
}
