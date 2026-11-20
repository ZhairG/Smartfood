package com.smartfood.app.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: Daos.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Consultas de la base local (spec 014, plan §2). Todas reciben el `uid` del usuario con sesión
 * (RNF-LOC-02) y son `suspend` o `Flow`, así que Room las corre fuera del hilo principal (RNF-LOC-03).
 */

/**
 * Historial de cocina guardado en el teléfono (HU-LOC-01). Es una clase abstracta para que [replaceAll] tenga cuerpo
 * dentro de una transacción de Room.
 */
@Dao
abstract class CookingDao {

    /** Registros de [uid], del más reciente al más antiguo (CA-LOC-01-01). */
    @Query("SELECT * FROM cookings WHERE uid = :uid ORDER BY cooked_at DESC")
    abstract fun observe(uid: String): Flow<List<CookingEntity>>

    /** Guarda o actualiza un registro (CA-LOC-01-03). */
    @Upsert
    abstract suspend fun upsert(entity: CookingEntity)

    @Upsert
    protected abstract suspend fun upsertAll(entities: List<CookingEntity>)

    @Query("DELETE FROM cookings WHERE uid = :uid")
    protected abstract suspend fun deleteByUid(uid: String)

    /** Reemplaza la copia de [uid] por la que llegó de Firestore, en una sola transacción (CA-LOC-01-02). */
    @Transaction
    open suspend fun replaceAll(uid: String, entities: List<CookingEntity>) {
        deleteByUid(uid)
        upsertAll(entities)
    }

    /** Borra la tabla al cerrar sesión (CA-LOC-01-05). */
    @Query("DELETE FROM cookings")
    abstract suspend fun deleteAll()
}

/** Búsquedas recientes por barra (HU-LOC-02). */
@Dao
interface RecentSearchDao {

    /** Las [limit] búsquedas más recientes de [uid] en [scope] (CA-LOC-02-01). */
    @Query("SELECT * FROM recent_searches WHERE uid = :uid AND scope = :scope ORDER BY used_at DESC LIMIT :limit")
    fun observe(uid: String, scope: String, limit: Int): Flow<List<RecentSearchEntity>>

    /** Guarda una búsqueda; si ya estaba (mismo texto normalizado), actualiza su fecha (CA-LOC-02-04). */
    @Upsert
    suspend fun upsert(entity: RecentSearchEntity)

    /** Deja solo las [keep] más recientes de [uid] en [scope] (CA-LOC-02-04). */
    @Query(
        """
        DELETE FROM recent_searches WHERE uid = :uid AND scope = :scope AND normalized NOT IN (
            SELECT normalized FROM recent_searches WHERE uid = :uid AND scope = :scope ORDER BY used_at DESC LIMIT :keep
        )
        """
    )
    suspend fun trim(uid: String, scope: String, keep: Int)

    /** «Borrar»: quita las búsquedas de una sola barra (CA-LOC-02-06). */
    @Query("DELETE FROM recent_searches WHERE uid = :uid AND scope = :scope")
    suspend fun clear(uid: String, scope: String)

    /** Borra la tabla al cerrar sesión (CA-LOC-02-08). */
    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()
}
