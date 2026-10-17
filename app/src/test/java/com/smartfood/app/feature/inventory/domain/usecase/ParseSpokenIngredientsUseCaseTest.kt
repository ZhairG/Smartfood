package com.smartfood.app.feature.inventory.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ParseSpokenIngredientsUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ParseSpokenIngredientsUseCase] (RF-INV-06).
 */
class ParseSpokenIngredientsUseCaseTest {

    private val parse = ParseSpokenIngredientsUseCase()

    private fun parse(text: String) = parse(listOf(text))

    @Test
    fun `CA-INV-07-03 comas e y separan los ingredientes`() {
        assertEquals(listOf("carne", "pollo", "leche"), parse("carne, pollo y leche"))
    }

    @Test
    fun `CA-INV-07-03 una coma seguida de y no deja la conjuncion en el ingrediente`() {
        assertEquals(listOf("carne", "leche"), parse("carne, y leche"))
    }

    @Test
    fun `CA-INV-07-03 la conjuncion e tambien separa`() {
        assertEquals(listOf("tomate", "huevos", "arroz"), parse("tomate, huevos e arroz"))
    }

    @Test
    fun `CA-INV-07-03 sin pausa tambien se separa por palabras`() {
        // El reconocedor no siempre pone la coma en la pausa (checklist del #45).
        assertEquals(listOf("leche", "tomate", "pasta"), parse("leche tomate y pasta"))
    }

    @Test
    fun `CA-INV-07-03 el resultado va en minusculas y sin puntuacion`() {
        assertEquals(listOf("carne", "pollo", "leche"), parse("Carne, Pollo y Leche."))
    }

    @Test
    fun `CA-INV-07-03 se quitan los articulos iniciales`() {
        assertEquals(
            listOf("leche", "tomates", "pasta", "cebolla"),
            parse("la leche, unos tomates, el pasta y una cebolla")
        )
    }

    @Test
    fun `CA-INV-07-03 los articulos y conectores dentro del dictado se quitan`() {
        assertEquals(listOf("salsa", "casa", "arroz", "pollo"), parse("salsa de la casa, arroz con pollo"))
    }

    @Test
    fun `CA-INV-07-03 las palabras que contienen y o e no se dividen`() {
        assertEquals(listOf("yogur", "leche", "entera"), parse("yogur y leche entera"))
    }

    @Test
    fun `CA-INV-07-03 los ingredientes repetidos aparecen una sola vez`() {
        assertEquals(listOf("limón", "leche"), parse("limón, leche y limon"))
    }

    @Test
    fun `CA-INV-07-03 se usa la primera alternativa no vacia`() {
        assertEquals(listOf("pasta"), parse(listOf("  ", "pasta", "pastas")))
    }

    @Test
    fun `CA-INV-07-05 un dictado vacio no da ingredientes`() {
        assertTrue(parse(emptyList()).isEmpty())
        assertTrue(parse(listOf("", " ")).isEmpty())
        assertTrue(parse(" , y ").isEmpty())
    }
}
