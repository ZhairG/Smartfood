package com.smartfood.app.feature.auth.data.repository

import com.google.firebase.auth.EmailAuthProvider
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

    override fun currentEmail(): String? = firebaseAuth.currentUser?.email

    override suspend fun reauthenticate(currentPassword: String): Result<Unit> = safeRunCatching {
        val user = firebaseAuth.currentUser ?: throw AuthException(AuthError.AccountNotFound)
        val email = user.email ?: throw AuthException(AuthError.AccountNotFound)
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        Unit
    }.mapFirebaseError()

    override suspend fun requestEmailChange(newEmail: String): Result<Unit> = safeRunCatching {
        val user = firebaseAuth.currentUser ?: throw AuthException(AuthError.AccountNotFound)
        user.verifyBeforeUpdateEmail(newEmail).await()
        Unit
    }.mapFirebaseError()
}
