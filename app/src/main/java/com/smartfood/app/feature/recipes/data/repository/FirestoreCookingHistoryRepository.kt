package com.smartfood.app.feature.recipes.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirestoreCookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [CookingHistoryRepository] sobre Cloud Firestore (`users/{uid}/cookings`).
 * Usa [Map] para escribir en el servidor sin reflexión (R8).
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
                val cookedAtTimestamp = doc.getTimestamp("cookedAt")
                val cookedAt = cookedAtTimestamp?.toDate()?.time ?: System.currentTimeMillis()

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

    override suspend fun saveRecord(record: CookingRecord): Result<Unit> = safeRunCatching {
        val col = userCookingsCollection() ?: throw InventoryException(InventoryError.NotAuthenticated)
        val data = hashMapOf(
            "recipeId" to record.recipeId,
            "servings" to record.servings,
            "rescuedValueCop" to record.rescuedValueCop,
            "cookedAt" to FieldValue.serverTimestamp()
        )
        col.document(record.id).set(data).await()
        Unit
    }.mapInventoryError()
}
