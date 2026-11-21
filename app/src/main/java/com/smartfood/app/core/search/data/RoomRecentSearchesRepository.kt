package com.smartfood.app.core.search.data

import com.smartfood.app.core.database.CurrentUid
import com.smartfood.app.core.database.RecentSearchDao
import com.smartfood.app.core.database.RecentSearchEntity
import com.smartfood.app.core.search.domain.RecentSearchesRepository
import com.smartfood.app.core.search.domain.SearchScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Nombre: RoomRecentSearchesRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Búsquedas recientes guardadas en Room (spec 014, T004, RF-LOC-03). Cada fila lleva el `uid` con sesión
 * y cada consulta filtra por él (RNF-LOC-02); sin sesión no se lee ni se guarda nada.
 *
 * @param dao Tabla `recent_searches`.
 * @param currentUid Usuario con sesión.
 */
class RoomRecentSearchesRepository @Inject constructor(
    private val dao: RecentSearchDao,
    private val currentUid: CurrentUid,
) : RecentSearchesRepository {

    override fun observe(scope: SearchScope, limit: Int): Flow<List<String>> {
        val uid = currentUid.get() ?: return flowOf(emptyList())
        return dao.observe(uid, scope.name, limit).map { rows -> rows.map { it.text } }
    }

    override suspend fun record(scope: SearchScope, text: String, normalized: String, usedAt: Long, keep: Int) {
        val uid = currentUid.get() ?: return
        dao.upsert(RecentSearchEntity(uid, scope.name, normalized, text, usedAt))
        dao.trim(uid, scope.name, keep)
    }

    override suspend fun clear(scope: SearchScope) {
        val uid = currentUid.get() ?: return
        dao.clear(uid, scope.name)
    }
}

/** Enlaza [RecentSearchesRepository] con Room. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RecentSearchesModule {

    @Binds
    abstract fun bindRecentSearchesRepository(impl: RoomRecentSearchesRepository): RecentSearchesRepository
}
