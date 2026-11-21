package com.smartfood.app.core.database

import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Nombre: LocalUserData.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Borra los datos de la base local al cerrar sesión (spec 014, RF-LOC-04, CA-LOC-01-05 y CA-LOC-02-08).
 * `LogoutUseCase` no es `suspend`, así que el borrado corre en el alcance de la app, fuera del hilo principal. Si
 * fallara, los DAO igual filtran por `uid` (RNF-LOC-02).
 */
interface LocalUserData {

    /** Borra el historial de cocina y las búsquedas recientes guardados en el teléfono. */
    fun clearAll()
}

/** Alcance de corrutinas que vive lo mismo que la app (`SupervisorJob` + `Dispatchers.IO`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * [LocalUserData] sobre Room.
 *
 * @param database Base local.
 * @param scope Alcance de la app, para terminar el borrado aunque la pantalla se cierre.
 */
class RoomLocalUserData @Inject constructor(
    private val database: SmartFoodDatabase,
    @param:ApplicationScope private val scope: CoroutineScope,
) : LocalUserData {

    override fun clearAll() {
        scope.launch {
            database.cookingDao().deleteAll()
            database.recentSearchDao().deleteAll()
        }
    }
}
