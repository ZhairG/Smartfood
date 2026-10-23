package com.smartfood.app.feature.recipes.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Nombre: FirestoreCookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [CookingHistoryRepository] sobre Cloud Firestore (`users/{uid}/cookings`).
 * Usa [Map] para escribir en el servidor sin reflexión (R8). Como el inventario (spec 006 §13), espera al servidor como
 * mucho [WRITE_WAIT_MILLIS]: sin conexión el registro queda en la cola y el resultado es `PENDING`.
 */
class FirestoreCookingHistoryRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : CookingHistoryRepository {

    private fun userCookingsCollection() = firebaseAuth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("cookings")
    }

    override fun observeHistory(): Flow<List<CookingRecord>> {
        val col = userCookingsCollection() ?: return flowOf(emptyList())
        return col.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val recipeId = doc.getString("recipeId") ?: return@mapNotNull null
                val servings = doc.getLong("servings")?.toInt() ?: 2
                val rescuedValueCop = doc.getLong("rescuedValueCop")?.toInt() ?: 0
                // Un registro aún sin subir no tiene la hora del servidor: se usa la estimada para que cuente este mes.
                val cookedAt = doc.getTimestamp("cookedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?.toDate()?.time ?: return@mapNotNull null

                CookingRecord(
                    id = doc.id,
                    recipeId = recipeId,
                    servings = servings,
                    cookedAt = cookedAt,
                    rescuedValueCop = rescuedValueCop
                )
            }
        }
    }

    override fun newRecordId(): String {
        val col = userCookingsCollection() ?: throw InventoryException(InventoryError.NotAuthenticated)
        return col.document().id
    }

    override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> = safeRunCatching {
        val col = userCookingsCollection() ?: throw InventoryException(InventoryError.NotAuthenticated)
        val data = hashMapOf(
            "recipeId" to record.recipeId,
            "servings" to record.servings,
            "rescuedValueCop" to record.rescuedValueCop,
            "cookedAt" to FieldValue.serverTimestamp()
        )
        val task = col.document(record.id).set(data)
        // `await()` de un Task<Void> da null aunque termine bien: por eso el bloque devuelve Unit.
        val confirmed = withTimeoutOrNull(WRITE_WAIT_MILLIS) { task.await(); Unit } != null
        if (confirmed) WriteOutcome.SYNCED else WriteOutcome.PENDING
    }.mapInventoryError()

    private companion object {
        const val WRITE_WAIT_MILLIS = 3_000L
    }
}
