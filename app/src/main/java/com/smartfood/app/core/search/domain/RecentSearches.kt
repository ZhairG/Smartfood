package com.smartfood.app.core.search.domain

import java.text.Normalizer
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: RecentSearches.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Dominio de las búsquedas recientes (spec 014, HU-LOC-02, RF-LOC-03): la barra donde se busca, el
 * contrato del repositorio y los casos de uso. Las reglas de qué se guarda viven solo en [RecordRecentSearchUseCase];
 * los ViewModels no las repiten. Es Kotlin puro: Room solo aparece en la capa `data`.
 */

/** Barra de búsqueda donde se hizo la búsqueda; cada una tiene sus propias recientes (CA-LOC-02-07). */
enum class SearchScope { RECIPES, INVENTORY }

/** Búsquedas recientes del usuario con sesión (RNF-LOC-02). */
interface RecentSearchesRepository {

    /** Las [limit] búsquedas más recientes de [scope], la última primero. */
    fun observe(scope: SearchScope, limit: Int): Flow<List<String>>

    /**
     * Guarda [text] en [scope]. Si ya existía [normalized], la actualiza en lugar de duplicarla, y deja solo las
     * [keep] más recientes.
     */
    suspend fun record(scope: SearchScope, text: String, normalized: String, usedAt: Long, keep: Int)

    /** «Borrar»: quita las búsquedas de [scope] (CA-LOC-02-06). */
    suspend fun clear(scope: SearchScope)
}

/**
 * Guarda una búsqueda que la persona usó: tocó un resultado, pulsó «Buscar» o dictó por voz (CA-LOC-02-02, 02-03).
 * Se recorta, necesita al menos [MIN_LETTERS] letras, no se duplica aunque cambien mayúsculas o tildes (la repetida sube
 * al primer lugar) y se conservan las [MAX_KEPT] últimas de cada barra (CA-LOC-02-04).
 *
 * @param repository Búsquedas recientes.
 * @param clock Reloj para la fecha de uso.
 */
class RecordRecentSearchUseCase @Inject constructor(
    private val repository: RecentSearchesRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(scope: SearchScope, text: String) {
        val trimmed = text.trim().replace(SPACES, " ")
        if (trimmed.count { it.isLetter() } < MIN_LETTERS) return
        repository.record(scope, trimmed, normalize(trimmed), clock.millis(), MAX_KEPT)
    }

    companion object {
        const val MIN_LETTERS = 2
        const val MAX_KEPT = 10
        private val DIACRITICS = Regex("""\p{Mn}+""")
        private val SPACES = Regex("""\s+""")

        /** Minúsculas y sin tildes, la misma regla que `SearchInventoryUseCase.normalize` ("Limón" → "limon"). */
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase(Locale.ROOT)
    }
}

/** Las búsquedas recientes que se muestran bajo la barra: las [MAX_SHOWN] más recientes (CA-LOC-02-01). */
class ObserveRecentSearchesUseCase @Inject constructor(
    private val repository: RecentSearchesRepository,
) {
    operator fun invoke(scope: SearchScope): Flow<List<String>> = repository.observe(scope, MAX_SHOWN)

    companion object {
        const val MAX_SHOWN = 5
    }
}

/** «Borrar»: quita las búsquedas recientes de una sola barra (CA-LOC-02-06). */
class ClearRecentSearchesUseCase @Inject constructor(
    private val repository: RecentSearchesRepository,
) {
    suspend operator fun invoke(scope: SearchScope) = repository.clear(scope)
}
