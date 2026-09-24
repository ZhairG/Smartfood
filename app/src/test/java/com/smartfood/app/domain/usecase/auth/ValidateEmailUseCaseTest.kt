package com.smartfood.app.domain.usecase.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias del formato de correo electrónico, independientes de la UI.
 */
class ValidateEmailUseCaseTest {

    private lateinit var classUnderTest: ValidateEmailUseCase

    @Before
    fun setUp() {
        classUnderTest = ValidateEmailUseCase()
    }

    @Test
    fun `valid emails pass validation`() {
        assertTrue(classUnderTest.execute("pepitoperez@gmail.com"))
        assertTrue(classUnderTest.execute("nombre.apellido+tag@correo.edu.co"))
        assertTrue(classUnderTest.execute("  pepitoperez@gmail.com  ")) // espacios en los extremos se ignoran
    }

    @Test
    fun `empty or malformed emails fail validation`() {
        assertFalse(classUnderTest.execute(""))
        assertFalse(classUnderTest.execute("pepitoperez"))
        assertFalse(classUnderTest.execute("pepitoperez@"))
        assertFalse(classUnderTest.execute("@gmail.com"))
        assertFalse(classUnderTest.execute("pepito@gmail"))
        assertFalse(classUnderTest.execute("pepito perez@gmail.com"))
        assertFalse(classUnderTest.execute("pepito@@gmail.com"))
    }
}
