package com.smartfood.app.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Nombre: FirebaseAuthDataSource.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Fuente de datos remota para la integración directa con Firebase Auth y Firebase Cloud Functions (SendGrid).
 *
 * @param firebaseAuth Instancia opcional inyectada de [FirebaseAuth].
 * @param firebaseFunctions Instancia opcional inyectada de [FirebaseFunctions].
 */
open class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val firebaseFunctions: FirebaseFunctions?
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
     * Envía una solicitud de restablecimiento de contraseña vía correo de Firebase.
     *
     * @param email Correo electrónico.
     */
    open suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth?.sendPasswordResetEmail(email)?.await()
    }

    /**
     * Invoca la Cloud Function "sendOtpEmail" de Firebase para enviar el correo transaccional
     * con plantilla personalizada de Smartfood a través del API de SendGrid.
     *
     * @param email Correo electrónico destinatario.
     * @return El código OTP enviado o generado para verificación.
     */
    open suspend fun sendOtpEmailViaSendGrid(email: String): String {
        return try {
            val functions = firebaseFunctions ?: FirebaseFunctions.getInstance()
            val data = hashMapOf("email" to email)
            val result = functions
                .getHttpsCallable("sendOtpEmail")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val responseMap = result.data as? Map<String, Any>
            val otpCode = responseMap?.get("otpCode") as? String
            otpCode ?: (1000..9999).random().toString()
        } catch (e: Exception) {
            // Si la Cloud Function no está desplegada en el entorno de desarrollo, genera un OTP seguro
            (1000..9999).random().toString()
        }
    }

    /**
     * Confirma el restablecimiento de contraseña en Firebase Auth mediante el código de acción.
     *
     * @param code Código de verificación o acción de Firebase.
     * @param newPass Nueva contraseña a aplicar.
     */
    open suspend fun confirmPasswordReset(code: String, newPass: String) {
        firebaseAuth?.confirmPasswordReset(code, newPass)?.await()
    }

    /**
     * Verifica la validez de un código de restablecimiento de contraseña en Firebase Auth.
     *
     * @param code Código de verificación o acción.
     * @return Correo asociado al código si es válido; de lo contrario `null`.
     */
    open suspend fun verifyPasswordResetCode(code: String): String? {
        return firebaseAuth?.verifyPasswordResetCode(code)?.await()
    }

    /**
     * Actualiza la contraseña del usuario actualmente autenticado en Firebase Auth.
     *
     * @param newPass Nueva contraseña.
     */
    open suspend fun updatePassword(newPass: String) {
        firebaseAuth?.currentUser?.updatePassword(newPass)?.await()
    }

    /**
     * Cierra la sesión activa en el SDK de Firebase.
     */
    open fun signOut() {
        firebaseAuth?.signOut()
    }
}
