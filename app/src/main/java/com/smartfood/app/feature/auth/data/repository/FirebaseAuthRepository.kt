package com.smartfood.app.feature.auth.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirebaseAuthRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [AuthRepository] sobre Firebase Authentication (RF-AUTH-01 y 05). El correo de verificación lo
 * envía el servidor (spec 004).
 * Cada fallo se clasifica con [com.smartfood.app.feature.auth.data.mapper.FirebaseErrorMapper].
 *
 * @param firebaseAuth Instancia de [FirebaseAuth] provista por Hilt.
 */
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<Unit> = safeRunCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }.mapFirebaseError()

    override suspend fun createAccount(email: String, password: String): Result<String> = safeRunCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user?.uid ?: throw AuthException(AuthError.Unknown("Firebase no devolvió el usuario creado"))
    }.mapFirebaseError()

    override suspend fun deleteCurrentAccount(): Result<Unit> = safeRunCatching {
        firebaseAuth.currentUser?.delete()?.await()
        Unit
    }.mapFirebaseError()

    override fun logout() = firebaseAuth.signOut()
}
