package com.smartfood.app.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirebaseAuthDataSource.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Fuente de datos remota para la integración directa con el SDK de Firebase Authentication.
 *
 * @param firebaseAuth Instancia opcional inyectada de [FirebaseAuth].
 */
open class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth?
) {
    /** Usuario actualmente autenticado en Firebase. */
    open val currentUser: FirebaseUser? get() = firebaseAuth?.currentUser

    /**
     * Autentica al usuario con correo y contraseña en Firebase Auth.
     *
     * @param email Correo electrónico.
     * @param pass Contraseña.
     */
    open suspend fun signInWithEmailAndPassword(email: String, pass: String) {
        firebaseAuth?.signInWithEmailAndPassword(email, pass)?.await()
    }

    /**
     * Registra un nuevo usuario en Firebase Auth y envía automáticamente el correo de confirmación predeterminado.
     *
     * @param email Correo electrónico.
     * @param pass Contraseña.
     */
    open suspend fun createUserWithEmailAndPassword(email: String, pass: String) {
        firebaseAuth?.createUserWithEmailAndPassword(email, pass)?.await()
        firebaseAuth?.currentUser?.sendEmailVerification()?.await()
    }

    /**
     * Envía una solicitud de restablecimiento de contraseña vía correo.
     *
     * @param email Correo electrónico.
     */
    open suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth?.sendPasswordResetEmail(email)?.await()
    }

    /**
     * Cierra la sesión activa en el SDK de Firebase.
     */
    open fun signOut() {
        firebaseAuth?.signOut()
    }
}
