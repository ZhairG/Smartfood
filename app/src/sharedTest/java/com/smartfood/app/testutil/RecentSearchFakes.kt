package com.smartfood.app.testutil

import com.smartfood.app.core.search.domain.RecentSearchesRepository
import com.smartfood.app.core.search.domain.SearchScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Nombre: RecentSearchFakes.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: [RecentSearchesRepository] en memoria (spec 014, T004) con las mismas reglas que Room: clave por barra y
 * texto normalizado, orden por fecha descendente y recorte a las [keep] más recientes.
 */
class FakeRecentSearchesRepository : RecentSearchesRepository {

    /** Una búsqueda guardada. */
    data class Row(val scope: SearchScope, val normalized: String, val text: String, val usedAt: Long)

    val rows = MutableStateFlow<List<Row>>(emptyList())

    override fun observe(scope: SearchScope, limit: Int): Flow<List<String>> = rows.map { all ->
        all.filter { it.scope == scope }.sortedByDescending { it.usedAt }.take(limit).map { it.text }
    }

    override suspend fun record(scope: SearchScope, text: String, normalized: String, usedAt: Long, keep: Int) {
        val others = rows.value.filterNot { it.scope == scope && it.normalized == normalized }
        val updated = others + Row(scope, normalized, text, usedAt)
        val kept = updated.filter { it.scope == scope }.sortedByDescending { it.usedAt }.take(keep)
        rows.value = updated.filter { it.scope != scope } + kept
    }

    override suspend fun clear(scope: SearchScope) {
        rows.value = rows.value.filterNot { it.scope == scope }
    }

    /** Textos guardados de [scope], del más reciente al más antiguo. */
    fun texts(scope: SearchScope): List<String> =
        rows.value.filter { it.scope == scope }.sortedByDescending { it.usedAt }.map { it.text }
}
