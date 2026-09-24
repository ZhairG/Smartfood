package com.smartfood.app.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: ValidateEmailUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para verificar la validación de formato de correo electrónico en [ValidateEmailUseCase].
 */
class ValidateEmailUseCaseTest {

    private lateinit var classUnderTest: ValidateEmailUseCase

    @Before
    fun prepararEntornoTest() {
        classUnderTest = ValidateEmailUseCase()
    }

    @Test
    fun `correos electronicos validos pasan la validacion`() {
        assertTrue(classUnderTest.execute("pepitoperez@gmail.com"))
        assertTrue(classUnderTest.execute("nombre.apellido+tag@correo.edu.co"))
        assertTrue(classUnderTest.execute("  pepitoperez@gmail.com  "))
    }

    @Test
    fun `correos electronicos vacios o mal formados fallan la validacion`() {
        assertFalse(classUnderTest.execute(""))
        assertFalse(classUnderTest.execute("pepitoperez"))
        assertFalse(classUnderTest.execute("pepitoperez@"))
        assertFalse(classUnderTest.execute("@gmail.com"))
        assertFalse(classUnderTest.execute("pepito@gmail"))
        assertFalse(classUnderTest.execute("pepito perez@gmail.com"))
        assertFalse(classUnderTest.execute("pepito@@gmail.com"))
    }
}
