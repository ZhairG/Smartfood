package com.smartfood.app.feature.inventory.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.InventoryErrorMapper
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirestoreInventoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [InventoryRepository] sobre Cloud Firestore (spec 006, RF-INV-01, plan §3).
 * Cada producto vive en `users/{uid}/inventory/{itemId}` y se protege con `firestore.rules` (RNF-INV-01).
 * Guardar y borrar usan transacciones: sin conexión fallan con un error de red en lugar de quedar esperando
 * (RNF-INV-05), y al guardar se sabe si el documento existe para escribir `createdAt` solo al crearlo.
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
        // El uid se lee al empezar a recolectar, no al crear el Flow.
        emitAll(inventory().snapshots())
    }
        .map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                document.data?.let { inventoryItemFromDocument(document.id, it) }
            }
        }
        .catch { throw InventoryErrorMapper.toException(it) }

    override suspend fun getItem(id: String): Result<InventoryItem> = safeRunCatching {
        val snapshot = inventory().document(id).get().await()
        snapshot.data?.let { inventoryItemFromDocument(snapshot.id, it) }
            ?: throw InventoryException(InventoryError.NotFound)
    }.mapInventoryError()

    // El id es aleatorio y no depende de la ruta: no necesita sesión, así que nunca lanza. Sin sesión, el fallo
    // llega como Result en saveItem.
    override fun newItemId(): String = firestore.collection(INVENTORY_COLLECTION).document().id

    override suspend fun saveItem(item: InventoryItem): Result<Unit> = safeRunCatching {
        val document = inventory().document(item.id)
        firestore.runTransaction { transaction ->
            val current = transaction.get(document)
            // Las reglas exigen createdAt = hora del servidor al crear y sin cambios al editar.
            val createdAt: Any = current.get(InventoryDocument.CREATED_AT)
                ?.takeIf { current.exists() }
                ?: FieldValue.serverTimestamp()
            // Un set completo: si se quitó la cantidad o la foto, sus campos desaparecen.
            transaction.set(
                document,
                item.toDocumentFields() + mapOf(
                    InventoryDocument.CREATED_AT to createdAt,
                    InventoryDocument.UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
        Unit
    }.mapInventoryError()

    override suspend fun deleteItem(id: String): Result<Unit> = safeRunCatching {
        val document = inventory().document(id)
        firestore.runTransaction { transaction ->
            transaction.delete(document)
            null
        }.await()
        Unit
    }.mapInventoryError()

    private companion object {
        const val USERS_COLLECTION = "users"
        const val INVENTORY_COLLECTION = "inventory"
    }
}
