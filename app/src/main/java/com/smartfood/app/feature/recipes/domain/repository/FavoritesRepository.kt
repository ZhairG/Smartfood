package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: FavoritesRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Contrato de las recetas favoritas (spec 010, RF-SAV-01, plan §3, T002). La implementación guarda en
 * `users/{uid}/favorites/{recipeId}`; las pantallas y los casos de uso solo conocen este contrato.
 */
interface FavoritesRepository {

    /** Favoritas del usuario con sesión, con la hora en que se guardaron y si esperan sincronizarse. */
    fun observeFavorites(): Flow<List<Favorite>>

    /**
     * Ids de las favoritas quitadas cuyo borrado todavía espera al servidor. Un borrado saca el documento de
     * [observeFavorites], así que sin este flujo Guardadas no sabría que hay un cambio pendiente (CA-SAV-02-09).
     */
    fun observePendingRemovals(): Flow<Set<String>>

    /**
     * Marca o quita una receta como favorita.
     *
     * @param recipeId Id del catálogo o de una receta propia.
     * @param source Origen de la receta.
     * @param favorite `true` para guardarla, `false` para quitarla.
     * @return `SYNCED` si el servidor confirmó la escritura y `PENDING` si quedó en la cola sin conexión
     *   (CA-SAV-01-04, RNF-SAV-02).
     */
    suspend fun setFavorite(
        recipeId: String,
        source: FavoriteSource,
        favorite: Boolean,
    ): Result<WriteOutcome>
}
