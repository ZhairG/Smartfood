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
     * @return Los ingredientes en minúsculas, sin artículos iniciales ni repetidos; vacía si no se entendió nada.
     */
    operator fun invoke(alternatives: List<String>): List<String> {
        val text = alternatives.firstOrNull { it.isNotBlank() } ?: return emptyList()
        return text.lowercase(SPANISH)
            .split(SEPARATORS)
            .map { it.replace(PUNCTUATION, " ").trim().replace(LEADING_ARTICLE, "").replace(SPACES, " ") }
            .filter { it.isNotEmpty() }
            .distinctBy(SearchInventoryUseCase::normalize)
    }

    private companion object {
        val SPANISH: Locale = Locale.forLanguageTag("es-CO")
        /** Comas, punto y coma, y las palabras sueltas "y" o "e" (no la letra dentro de "yogur" o "leche"). */
        val SEPARATORS = Regex("""\s*(?:[,;]|(?<!\p{L})[ye](?!\p{L}))\s*""")
        val PUNCTUATION = Regex("""[.!?¿¡]""")
        val LEADING_ARTICLE = Regex("""^(el|la|los|las|un|una|unos|unas)\s+""")
        val SPACES = Regex("""\s+""")
    }
}
