package com.smartfood.app.feature.recipes.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.InventoryErrorMapper
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Nombre: FirestoreFavoritesRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Implementación de [FavoritesRepository] sobre Cloud Firestore
 * (`users/{uid}/favorites/{recipeId}`, spec 010, RF-SAV-01, plan §2.1 y §3, T002). Escribe con [Map] para no depender
 * de reflexión (R8) y sin transacciones, como el inventario (spec 006 §13): cada escritura espera al servidor como
 * mucho [WRITE_WAIT_MILLIS] y, si no responde, queda en la cola como `PENDING` (CA-SAV-01-04, RNF-SAV-02).
 *
 * Copia el patrón de `FirestoreInventoryRepository`: lee el uid al empezar a recolectar, escucha los cambios de
 * metadatos para saber si una favorita sigue pendiente y clasifica los errores con [InventoryErrorMapper].
 *
 * @param firebaseAuth Sesión de la que sale el uid.
 * @param firestore Instancia de [FirebaseFirestore] provista por Hilt.
 */
class FirestoreFavoritesRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : FavoritesRepository {

    /** Colección de favoritas del usuario con sesión activa; sin sesión falla con [InventoryError.NotAuthenticated]. */
    private fun favorites(): CollectionReference {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw InventoryException(InventoryError.NotAuthenticated)
        return firestore.collection(USERS_COLLECTION).document(uid).collection(FAVORITES_COLLECTION)
    }

    override fun observeFavorites(): Flow<List<Favorite>> = flow {
        // El uid se lee al empezar a recolectar, no al crear el Flow. Con los cambios de metadatos, la lista se
        // actualiza también cuando una escritura pendiente llega al servidor (CA-SAV-02-09).
        emitAll(favorites().snapshots(MetadataChanges.INCLUDE))
    }
        .map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                document.toFavorite()
            }
        }
        .catch { throw InventoryErrorMapper.toException(it) }

    override suspend fun setFavorite(
        recipeId: String,
        source: FavoriteSource,
        favorite: Boolean,
    ): Result<WriteOutcome> = safeRunCatching {
        val document = favorites().document(recipeId)
        val task: Task<Void> = if (favorite) {
            // `set` también sirve cuando el documento ya existe: volver a tocar el ♡ sin conexión no falla y las
            // reglas permiten `create` y `update` (plan §6).
            document.set(
                mapOf(
                    SOURCE_FIELD to source.toFirestoreValue(),
                    SAVED_AT_FIELD to FieldValue.serverTimestamp()
                )
            )
        } else {
            document.delete()
        }
        // `await()` de un Task<Void> da null aunque termine bien: por eso el bloque devuelve Unit.
        val confirmed = withTimeoutOrNull(WRITE_WAIT_MILLIS) { task.await(); Unit } != null
        if (confirmed) WriteOutcome.SYNCED else WriteOutcome.PENDING
    }.mapInventoryError()

    /**
     * Lee una favorita. La hora se estima cuando todavía no llegó el servidor, así una favorita guardada sin conexión
     * ya cuenta como reciente (spec §7).
     */
    private fun DocumentSnapshot.toFavorite(): Favorite? {
        val source = getString(SOURCE_FIELD)?.toFavoriteSource() ?: return null
        val savedAt = getTimestamp(SAVED_AT_FIELD, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: return null
        return Favorite(
            recipeId = id,
            source = source,
            savedAtMillis = savedAt,
            isPendingSync = metadata.hasPendingWrites(),
        )
    }

    /** Valor del campo `source` que exigen las reglas: `"catalog"` u `"own"` (plan §6). */
    private fun FavoriteSource.toFirestoreValue(): String = when (this) {
        FavoriteSource.CATALOG -> CATALOG_VALUE
        FavoriteSource.OWN -> OWN_VALUE
    }

    /** Lee el campo `source`; devuelve `null` si el valor no es conocido. */
    private fun String.toFavoriteSource(): FavoriteSource? = when (lowercase()) {
        CATALOG_VALUE -> FavoriteSource.CATALOG
        OWN_VALUE -> FavoriteSource.OWN
        else -> null
    }

    private companion object {
        const val WRITE_WAIT_MILLIS = 3_000L
        const val USERS_COLLECTION = "users"
        const val FAVORITES_COLLECTION = "favorites"
        const val SOURCE_FIELD = "source"
        const val SAVED_AT_FIELD = "savedAt"
        const val CATALOG_VALUE = "catalog"
        const val OWN_VALUE = "own"
    }
}
