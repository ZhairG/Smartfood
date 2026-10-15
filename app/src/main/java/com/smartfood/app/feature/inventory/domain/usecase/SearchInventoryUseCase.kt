package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Nombre: SearchInventoryUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Filtra el inventario por texto y categoría (RF-INV-05). El texto se divide por comas en términos y
 * un producto coincide si su nombre contiene cualquiera de ellos, sin importar mayúsculas ni tildes (ADR-INV-05).
 */
class SearchInventoryUseCase @Inject constructor() {

    /**
     * @param query Texto de la barra de búsqueda; vacío no filtra por nombre.
     * @param category Categoría elegida, o `null` para "Todos" (CA-INV-06-03).
     * @return Los productos que cumplen el texto y la categoría a la vez (CA-INV-06-04), ordenados por fecha de
     * vencimiento y nombre.
     */
    operator fun invoke(items: List<InventoryItem>, query: String, category: FoodCategory?): List<InventoryItem> {
        val terms = query.split(',').map(::normalize).filter { it.isNotEmpty() }.map(::variants)
        return items
            .filter { category == null || it.category == category }
            .filter { item ->
                val name = normalize(item.name)
                terms.isEmpty() || terms.any { variants -> variants.any { name.contains(it) } }
            }
            .sortedWith(compareBy<InventoryItem> { it.expiresOn }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    /**
     * Formas con las que un término puede aparecer en un nombre: el término y su singular simple, sin la `es` o la
     * `s` final (CA-INV-07-04). No se recorta si quedaría una raíz demasiado corta.
     */
    private fun variants(term: String): List<String> = buildList {
        add(term)
        if (term.endsWith("es") && term.length - 2 >= MIN_STEM_LENGTH) add(term.dropLast(2))
        if (term.endsWith("s") && term.length - 1 >= MIN_STEM_LENGTH) add(term.dropLast(1))
    }

    companion object {
        private const val MIN_STEM_LENGTH = 3
        private val DIACRITICS = Regex("""\p{Mn}+""")
        private val SPACES = Regex("""\s+""")

        /** Minúsculas, sin tildes y con los espacios recortados y unificados ("Lácteo " → "lacteo"). */
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase(Locale.ROOT)
                .trim()
                .replace(SPACES, " ")
    }
}
