package com.smartfood.app.feature.inventory.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.InventoryErrorMapper
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Nombre: FirestoreInventoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [InventoryRepository] sobre Cloud Firestore (spec 006, RF-INV-01, plan §3).
 * Cada producto vive en `users/{uid}/inventory/{itemId}` y se protege con `firestore.rules` (RNF-INV-01).
 * Las escrituras van a la cola de Firestore, que funciona sin conexión (spec 006 §13, T047): cada una espera al
 * servidor como mucho [WRITE_WAIT_MILLIS]; si no responde, queda guardada en el teléfono como `PENDING` y se sube sola
 * cuando vuelve la conexión. Un error que llega antes, como el rechazo de las reglas, sí falla (CA-INV-02-07).
 *
 * @param firestore Instancia de [FirebaseFirestore] provista por Hilt.
 * @param auth Instancia de [FirebaseAuth] de la que sale el uid del usuario con sesión activa.
 */
class FirestoreInventoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : InventoryRepository {

    /** Colección del usuario con sesión activa; sin sesión falla con [InventoryError.NotAuthenticated]. */
    private fun inventory(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw InventoryException(InventoryError.NotAuthenticated)
        return firestore.collection(USERS_COLLECTION).document(uid).collection(INVENTORY_COLLECTION)
    }

    override fun observeItems(): Flow<List<InventoryItem>> = flow {
        // El uid se lee al empezar a recolectar, no al crear el Flow. Con los cambios de metadatos, la lista se
        // actualiza también cuando una escritura pendiente llega al servidor (CA-INV-10-05).
        emitAll(inventory().snapshots(MetadataChanges.INCLUDE))
    }
        .map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                document.data?.let { inventoryItemFromDocument(document.id, it) }
                    ?.copy(isPendingSync = document.metadata.hasPendingWrites())
            }
        }
        .catch { throw InventoryErrorMapper.toException(it) }

    override suspend fun getItem(id: String): Result<InventoryItem> = safeRunCatching {
        val snapshot = inventory().document(id).get().await()
        snapshot.data?.let { inventoryItemFromDocument(snapshot.id, it) }
            ?: throw InventoryException(InventoryError.NotFound)
    }.mapInventoryError()

    // El id es aleatorio y no depende de la ruta: no necesita sesión, así que nunca lanza. Sin sesión, el fallo
    // llega como Result en createItem.
    override fun newItemId(): String = firestore.collection(INVENTORY_COLLECTION).document().id

    override suspend fun createItem(item: InventoryItem): Result<WriteOutcome> = write {
        // Las reglas exigen createdAt = hora del servidor al crear; se resuelve cuando la escritura llega (§3).
        inventory().document(item.id).set(
            item.toDocumentFields() + mapOf(
                InventoryDocument.CREATED_AT to FieldValue.serverTimestamp(),
                InventoryDocument.UPDATED_AT to FieldValue.serverTimestamp()
            )
        )
    }

    override suspend fun updateItem(item: InventoryItem, photoChanged: Boolean): Result<WriteOutcome> = write {
        // `update` no toca createdAt, como piden las reglas, y borra la cantidad o el precio que se quitaron.
        inventory().document(item.id).update(
            item.toUpdateFields(photoChanged, FieldValue.delete()) +
                (InventoryDocument.UPDATED_AT to FieldValue.serverTimestamp())
        )
    }

    override suspend fun deleteItem(id: String): Result<WriteOutcome> = write {
        inventory().document(id).delete()
    }

    override suspend fun setPhoto(id: String, photo: ItemPhoto?): Result<Unit> = safeRunCatching {
        // Lo llama PhotoUploadWorker con conexión: espera la confirmación. Las reglas rechazan editar un producto que
        // ya no existe (con PERMISSION_DENIED, no NOT_FOUND), así que se comprueba antes en el servidor para que el
        // worker sepa que debe borrar la foto recién subida (§13).
        val document = inventory().document(id)
        if (!document.get(Source.SERVER).await().exists()) throw InventoryException(InventoryError.NotFound)
        document.update(
            photo.toPhotoFields(FieldValue.delete()) + (InventoryDocument.UPDATED_AT to FieldValue.serverTimestamp())
        ).await()
        Unit
    }.mapInventoryError()

    override suspend fun hasPendingWrites(): Boolean =
        // Termina cuando el servidor confirmó todas las escrituras; si no termina enseguida, hay pendientes. Cubre
        // también los borrados, que ya no aparecen en la caché.
        withTimeoutOrNull(PENDING_CHECK_MILLIS) { firestore.waitForPendingWrites().await(); Unit } == null

    /**
     * Envía la escritura a la cola de Firestore y espera al servidor como mucho [WRITE_WAIT_MILLIS]. Si no responde
     * a tiempo, la escritura sigue en la cola y el resultado es `PENDING` (CA-INV-10-02).
     */
    private suspend fun write(block: () -> Task<Void>): Result<WriteOutcome> = safeRunCatching {
        val task = block()
        // `await()` de un Task<Void> da null aunque termine bien: por eso el bloque devuelve Unit.
        val confirmed = withTimeoutOrNull(WRITE_WAIT_MILLIS) { task.await(); Unit } != null
        if (confirmed) WriteOutcome.SYNCED else WriteOutcome.PENDING
    }.mapInventoryError()

    private companion object {
        const val WRITE_WAIT_MILLIS = 3_000L
        const val PENDING_CHECK_MILLIS = 500L
        const val USERS_COLLECTION = "users"
        const val INVENTORY_COLLECTION = "inventory"
    }
}
