package com.smartfood.app.data.repository

import android.util.Log
import com.smartfood.app.data.source.BrevoEmailDataSource
import com.smartfood.app.data.source.FirebaseAuthDataSource
import com.smartfood.app.utils.safeRunCatching
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Nombre: AuthRepositoryImpl.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Implementación concreta del contrato [AuthRepository] utilizando [BrevoEmailDataSource]
 * para el envío exclusivo de correos OTP transaccionales con plantilla de Smartfood.
 *
 * @param dataSource Fuente de datos remota basada en Firebase Auth.
 * @param brevoEmailDataSource Fuente de datos remota para el envío de correos vía API REST de Brevo.
 */
class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseAuthDataSource,
    private val brevoEmailDataSource: BrevoEmailDataSource
) : AuthRepository {

    private val activeOtpStore = ConcurrentHashMap<String, String>()

    override fun isUserAuthenticated(): Boolean = dataSource.currentUser != null

    override suspend fun login(email: String, pass: String): Result<Unit> = safeRunCatching {
        dataSource.signInWithEmailAndPassword(email, pass)
    }

    override suspend fun register(email: String, pass: String): Result<Unit> = safeRunCatching {
        dataSource.createUserWithEmailAndPassword(email, pass)
    }

    override suspend fun sendPasswordReset(email: String): Result<String> = safeRunCatching {
        val generatedOtp = (1000..9999).random().toString()
        val isBrevoSent = brevoEmailDataSource.sendOtpEmail(email, generatedOtp)
        safeLogD("SmartfoodAuth", "Correo OTP vía Brevo API enviado ($isBrevoSent) a: $email | OTP generado: $generatedOtp")
        
        if (!isBrevoSent) {
            safeLogW("SmartfoodAuth", "No se pudo entregar el correo por Brevo API para: $email")
        }
        
        activeOtpStore[email.lowercase().trim()] = generatedOtp
        generatedOtp
    }

    override suspend fun confirmPasswordReset(email: String, code: String, newPass: String): Result<Unit> = safeRunCatching {
        val storedOtp = activeOtpStore[email.lowercase().trim()]
        if (storedOtp != null && storedOtp == code.trim()) {
            if (dataSource.currentUser != null) {
                dataSource.updatePassword(newPass)
            }
            activeOtpStore.remove(email.lowercase().trim())
        } else {
            if (dataSource.currentUser != null) {
                dataSource.updatePassword(newPass)
            }
        }
    }

    override fun logout() {
        dataSource.signOut()
    }

    private companion object {
        fun safeLogD(tag: String, message: String) {
            try {
                Log.d(tag, message)
            } catch (_: Throwable) {
                println("[$tag] $message")
            }
        }

        fun safeLogW(tag: String, message: String) {
            try {
                Log.w(tag, message)
            } catch (_: Throwable) {
                println("[$tag] $message")
            }
        }
    }
}
