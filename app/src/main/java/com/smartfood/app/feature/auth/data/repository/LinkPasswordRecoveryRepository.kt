package com.smartfood.app.feature.auth.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: LinkPasswordRecoveryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Recuperación del modo gratuito (spec 012, RF-SPK-03): sin Cloud Functions, Firebase envía el correo
 * de restablecimiento con un enlace a su página para crear la contraseña nueva. No hay código, así que
 * [verifyCode] y [resetPassword] no se usan en este modo. Responde igual exista o no la cuenta (CA-REC-01-02).
 * @param auth instancia de Firebase Authentication.
 */
class LinkPasswordRecoveryRepository @Inject constructor(
    private val auth: FirebaseAuth
) : PasswordRecoveryRepository {

    override val usesCode: Boolean = false

    /** Envía el enlace en español y devuelve la espera para reenviarlo (CA-SPK-02-01, CA-SPK-02-03). */
    override suspend fun requestCode(email: String): Result<Duration> {
        val sent = safeRunCatching {
            auth.setLanguageCode(LANGUAGE)
            auth.sendPasswordResetEmail(email).await()
        }.mapFirebaseError()
        // Si la protección contra la enumeración está apagada, Firebase avisa que la cuenta no existe: se calla
        // para no revelarlo, igual que el servidor de la spec 003.
        val failure = sent.exceptionOrNull()
        return if (failure == null || failure.toAuthError() == AuthError.AccountNotFound) {
            Result.success(RESEND_WAIT)
        } else {
            Result.failure(failure)
        }
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> =
        authFailure(AuthError.ServiceUnavailable)

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        authFailure(AuthError.ServiceUnavailable)

    private companion object {
        const val LANGUAGE = "es"
        val RESEND_WAIT = 60.seconds
    }
}
