package com.smartfood.app.testutil

import com.smartfood.app.core.database.CookingDao
import com.smartfood.app.core.database.CookingEntity
import com.smartfood.app.core.database.CurrentUid
import com.smartfood.app.core.database.LocalUserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Nombre: LocalDataFakes.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fakes de la base local (spec 014) para las pruebas unitarias e instrumentadas: un [CookingDao] en
 * memoria con el mismo orden y filtro por `uid` que Room, el usuario con sesión y el borrado al cerrar sesión.
 */

/** [CookingDao] en memoria: ordena por fecha descendente y filtra por `uid`, como la consulta de Room. */
class FakeCookingDao : CookingDao() {

    val rows = MutableStateFlow<List<CookingEntity>>(emptyList())

    override fun observe(uid: String): Flow<List<CookingEntity>> =
        rows.map { all -> all.filter { it.uid == uid }.sortedByDescending { it.cookedAt } }

    override suspend fun upsert(entity: CookingEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun upsertAll(entities: List<CookingEntity>) = entities.forEach { upsert(it) }

    override suspend fun deleteByUid(uid: String) {
        rows.value = rows.value.filterNot { it.uid == uid }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

/** Usuario con sesión que la prueba puede cambiar; `null` es sin sesión. */
class FakeCurrentUid(var uid: String? = "ana") : CurrentUid {
    override fun get(): String? = uid
}

/** Cuenta las veces que se pidió borrar la base local al cerrar sesión (CA-LOC-01-05, CA-LOC-02-08). */
class FakeLocalUserData : LocalUserData {
    var clears = 0
        private set

    override fun clearAll() {
        clears++
    }
}
