package com.smartfood.app.feature.recipes.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.data.repository.InventoryDocument
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.InventoryChange
import com.smartfood.app.feature.recipes.domain.repository.CookingRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Nombre: FirestoreCookingRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Implementación de [CookingRepository] sobre Cloud Firestore (spec 009, plan §6, CA-RIA-04-08 y 04-09).
 * El descuento de `users/{uid}/inventory` y el registro de `users/{uid}/cookings/{operationId}` van en un solo
 * `WriteBatch`, sin transacciones, para que el cierre funcione sin conexión (spec 006 §13): o se aplica todo o nada.
 * Al actualizar un producto solo se escriben la cantidad, la unidad y `updatedAt`, así que su precio y su foto no se
 * tocan. Las reglas niegan editar `cookings`, de modo que un lote que repite la operación se rechaza entero. Usa [Map]
 * para escribir sin reflexión (R8).
 */
class FirestoreCookingRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : CookingRepository {

    private fun user() = firebaseAuth.currentUser?.uid?.let { uid -> firestore.collection(USERS).document(uid) }
        ?: throw InventoryException(InventoryError.NotAuthenticated)

    private fun cookings(): CollectionReference = user().collection(COOKINGS)

    private fun inventory(): CollectionReference = user().collection(INVENTORY)

    override fun newOperationId(): String = cookings().document().id

    override suspend fun isFinished(operationId: String): Boolean {
        val document = safeRunCatching { cookings().document(operationId) }.getOrNull() ?: return false
        // Primero el teléfono: un cierre sin conexión ya está en la caché aunque no haya subido.
        val cached = safeRunCatching { document.get(Source.CACHE).await().exists() }.getOrDefault(false)
        if (cached) return true
        // Si no está en el teléfono, el servidor tiene la última palabra; sin respuesta se asume que no se aplicó
        // (las reglas rechazan el lote si en realidad ya existía).
        return safeRunCatching {
            withTimeoutOrNull(READ_WAIT_MILLIS) { document.get(Source.SERVER).await().exists() } ?: false
        }.getOrDefault(false)
    }

    override suspend fun finishCooking(record: CookingRecord, changes: List<InventoryChange>): Result<WriteOutcome> =
        safeRunCatching {
            val inventory = inventory()
            val batch = firestore.batch()
            changes.forEach { change ->
                val document = inventory.document(change.item.id)
                when (change) {
                    is InventoryChange.UpdateQuantity -> batch.update(
                        document,
                        mapOf(
                            InventoryDocument.QUANTITY to change.quantity.amount,
                            InventoryDocument.UNIT to change.quantity.unit.name,
                            InventoryDocument.UPDATED_AT to FieldValue.serverTimestamp(),
                        )
                    )
                    is InventoryChange.Delete -> batch.delete(document)
                }
            }
            batch.set(
                cookings().document(record.id),
                mapOf(
                    "recipeId" to record.recipeId,
                    "servings" to record.servings,
                    "rescuedValueCop" to record.rescuedValueCop,
                    "cookedAt" to FieldValue.serverTimestamp(),
                )
            )
            val task = batch.commit()
            // `await()` de un Task<Void> da null aunque termine bien: por eso el bloque devuelve Unit.
            val confirmed = withTimeoutOrNull(WRITE_WAIT_MILLIS) { task.await(); Unit } != null
            if (confirmed) WriteOutcome.SYNCED else WriteOutcome.PENDING
        }.mapInventoryError()

    private companion object {
        const val WRITE_WAIT_MILLIS = 3_000L
        const val READ_WAIT_MILLIS = 3_000L
        const val USERS = "users"
        const val COOKINGS = "cookings"
        const val INVENTORY = "inventory"
    }
}
