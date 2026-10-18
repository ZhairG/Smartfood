package com.smartfood.app.feature.inventory.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: ParseSpokenIngredientsUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ParseSpokenIngredientsUseCase] (RF-INV-06), como tabla de dictados y los
 * ingredientes que deben salir (#105).
 */
class ParseSpokenIngredientsUseCaseTest {

    private val parse = ParseSpokenIngredientsUseCase()

    @Test
    fun `CA-INV-07-03 CA-INV-07-05 cada dictado se separa en sus ingredientes`() {
        listOf(
            // Comas, "y" y "e" separan, también después de una coma.
            listOf("carne, pollo y leche") to listOf("carne", "pollo", "leche"),
            listOf("carne, y leche") to listOf("carne", "leche"),
            listOf("tomate, huevos e arroz") to listOf("tomate", "huevos", "arroz"),
            // El reconocedor no siempre pone la coma en la pausa (checklist del #45): se separa por palabras.
            listOf("leche tomate y pasta") to listOf("leche", "tomate", "pasta"),
            // En minúsculas, sin puntuación, sin artículos ni conectores y sin repetidos.
            listOf("Carne, Pollo y Leche.") to listOf("carne", "pollo", "leche"),
            listOf("la leche, unos tomates, el pasta y una cebolla") to listOf("leche", "tomates", "pasta", "cebolla"),
            listOf("salsa de la casa, arroz con pollo") to listOf("salsa", "casa", "arroz", "pollo"),
            listOf("limón, leche y limon") to listOf("limón", "leche"),
            // Las palabras que contienen "y" o "e" no se dividen.
            listOf("yogur y leche entera") to listOf("yogur", "leche", "entera"),
            // Se usa la primera alternativa no vacía.
            listOf("  ", "pasta", "pastas") to listOf("pasta"),
            // CA-INV-07-05: un dictado vacío no da ingredientes.
            emptyList<String>() to emptyList(),
            listOf("", " ") to emptyList(),
            listOf(" , y ") to emptyList()
        ).forEach { (dictado, ingredientes) ->
            assertEquals("Dictado $dictado", ingredientes, parse(dictado))
        }
    }
}
