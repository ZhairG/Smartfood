package com.smartfood.app.feature.profile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.FirebaseErrorMapper
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Nombre: FirestoreProfileRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [ProfileRepository] sobre el documento `users/{uid}` de Cloud Firestore (spec 008,
 * plan §2.1, RF-PER-01). Las escrituras usan la cola sin conexión de Firestore y esperan al servidor como mucho 3 s,
 * igual que el inventario (spec 006 §13): sin red, el cambio queda en el teléfono y se sube solo.
 *
 * @param firestore Instancia de [FirebaseFirestore] provista por Hilt.
 * @param auth Instancia de [FirebaseAuth], para saber de quién es el perfil.
 */
class FirestoreProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile> = flow {
        // El uid se lee al empezar a recolectar, no al crear el Flow.
        val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
        emitAll(
            userDocument(uid).snapshots().mapNotNull { snapshot ->
                snapshot.data?.let { profileFromDocument(uid, it) }
            }
        )
    }.catch { throw AuthException(FirebaseErrorMapper.map(it)) }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = write {
        userDocument(profile.uid).update(profile.toUpdateFields(FieldValue.delete(), FieldValue.serverTimestamp()))
    }

    override suspend fun syncEmail(email: String): Result<Unit> = write {
        val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
        userDocument(uid).update(
            mapOf(ProfileDocument.EMAIL to email, ProfileDocument.UPDATED_AT to FieldValue.serverTimestamp())
        )
    }

    override suspend fun getPreferences(): Result<FoodPreferences> = safeRunCatching {
        val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
        preferencesFromDocument(userDocument(uid).get().await().data.orEmpty())
    }.mapFirebaseError()

    override suspend fun savePreferences(preferences: FoodPreferences): Result<Unit> = write {
        val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
        // Al escribir el mapa completo se borran las claves que el usuario quitó (CA-PER-03-04). Firestore no acepta
        // `null`: `spice` y `budget` solo viajan cuando hay elección.
        userDocument(uid).update(
            mapOf(
                PreferencesDocument.PREFERENCES to preferences.toDocumentFields(),
                ProfileDocument.UPDATED_AT to FieldValue.serverTimestamp()
            )
        )
    }

    private fun userDocument(uid: String): DocumentReference = firestore.collection(USERS_COLLECTION).document(uid)

    /**
     * Encola la escritura y espera al servidor como mucho [SERVER_WAIT_MS]. Si no llega a tiempo, el cambio ya quedó
     * en la caché del teléfono (el Perfil lo muestra) y se sube cuando vuelva la conexión.
     */
    private suspend fun write(block: () -> com.google.android.gms.tasks.Task<Void>): Result<Unit> = safeRunCatching {
        val task = block()
        // `await` de un Task<Void> devuelve null: se devuelve Unit para distinguirlo del tiempo agotado.
        withTimeoutOrNull(SERVER_WAIT_MS) { task.await(); Unit }
        Unit
    }.mapFirebaseError()

    private companion object {
        const val USERS_COLLECTION = "users"
        const val SERVER_WAIT_MS = 3_000L
    }
}
