package com.smartfood.app.feature.auth.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirestoreUserRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [UserRepository] sobre Cloud Firestore (RF-AUTH-02).
 * Cada perfil vive en `users/{uid}` y se protege con `firestore.rules` (RNF-02).
 * Se escribe y se lee con mapas para no depender de reflexión con R8.
 *
 * @param firestore Instancia de [FirebaseFirestore] provista por Hilt.
 */
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun createProfile(user: User): Result<Unit> = safeRunCatching {
        val document = mapOf(
            FIELD_FULL_NAME to user.fullName,
            FIELD_EMAIL to user.email,
            FIELD_CREATED_AT to FieldValue.serverTimestamp()
        )
        firestore.collection(USERS_COLLECTION).document(user.uid).set(document).await()
        Unit
    }.mapFirebaseError()

    override suspend fun getProfile(uid: String): Result<User> = safeRunCatching {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        if (!snapshot.exists()) throw AuthException(AuthError.AccountNotFound)
        User(
            uid = uid,
            fullName = snapshot.getString(FIELD_FULL_NAME).orEmpty(),
            email = snapshot.getString(FIELD_EMAIL).orEmpty()
        )
    }.mapFirebaseError()

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_EMAIL = "email"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
