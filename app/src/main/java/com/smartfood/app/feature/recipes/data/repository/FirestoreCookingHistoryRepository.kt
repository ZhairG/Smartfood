package com.smartfood.app.feature.recipes.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: FirestoreCookingHistoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [CookingHistoryRepository] sobre Cloud Firestore (`users/{uid}/cookings`): lee el
 * historial para "Ahorrados este mes". El registro lo escribe [FirestoreCookingRepository] (plan §6).
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
}
