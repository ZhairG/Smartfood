package com.smartfood.app.data.source

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Fuente de datos remota para gestionar la autenticación directa con Firebase.
 */
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val currentUser get() = firebaseAuth.currentUser

    suspend fun signInWithEmailAndPassword(email: String, pass: String) {
        firebaseAuth.signInWithEmailAndPassword(email, pass).await()
    }

    suspend fun createUserWithEmailAndPassword(email: String, pass: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
