package com.smartfood.app.feature.inventory.domain.usecase

import java.util.Locale
import javax.inject.Inject

/**
 * Nombre: ParseSpokenIngredientsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Convierte un dictado en español en la lista de ingredientes que se busca (RF-INV-06, CA-INV-07-03).
 * Los separadores son las comas que el reconocedor pone en las pausas y las conjunciones "y" y "e". Sin pausa ni
 * conector no se separa ("carne pollo y leche" → "carne pollo", "leche"), porque no se puede hacer con seguridad.
 */
class ParseSpokenIngredientsUseCase @Inject constructor() {

    /**
     * @param alternatives Transcripciones del reconocedor, de la más probable a la menos probable.
     * @return Una palabra por ingrediente, en minúsculas, sin artículos ni conectores y sin repetidos; vacía si no se
     *   entendió nada. El reconocedor no siempre pone la coma en las pausas ("leche tomate y pasta"), así que cada
     *   palabra es un término: la búsqueda por "contiene" con O sigue encontrando los productos de varias palabras
     *   (CA-INV-07-03, #45).
     */
    operator fun invoke(alternatives: List<String>): List<String> {
        val text = alternatives.firstOrNull { it.isNotBlank() } ?: return emptyList()
        return text.lowercase(SPANISH)
            .split(SEPARATORS)
            .filter { it.isNotEmpty() && it !in STOP_WORDS }
            .distinctBy(SearchInventoryUseCase::normalize)
    }

    private companion object {
        val SPANISH: Locale = Locale.forLanguageTag("es-CO")
        /** Espacios, comas, punto y coma y la puntuación del dictado. */
        val SEPARATORS = Regex("""[\s,;.!?¿¡]+""")
        /** Artículos y conectores que no son ingredientes ("y", "e", "de", "con"…). */
        val STOP_WORDS = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "y", "e", "de", "del", "con", "sin", "a", "al", "para", "por",
        )
    }
}
