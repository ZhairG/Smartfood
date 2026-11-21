package com.smartfood.app.feature.profile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: LinkEmailChangeRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Cambio de correo del modo gratuito (spec 012, RF-SPK-04): sin Cloud Functions, Firebase envía un enlace
 * de verificación al correo nuevo y cambia la cuenta cuando se abre. El perfil toma el correo nuevo después, con
 * `SyncProfileEmailUseCase` (CA-SPK-03-03). No hay código, así que [confirm] no se usa en este modo.
 * @param auth instancia de Firebase Authentication.
 */
class LinkEmailChangeRepository @Inject constructor(
    private val auth: FirebaseAuth
) : EmailChangeRepository {

    override val usesCode: Boolean = false

    /** Requiere un inicio de sesión reciente: el caso de uso reautentica justo antes (CA-SPK-03-04). */
    override suspend fun requestCode(newEmail: String): Result<Duration> = safeRunCatching {
        val user = auth.currentUser ?: throw AuthException(AuthError.InvalidCredentials)
        auth.setLanguageCode(LANGUAGE)
        user.verifyBeforeUpdateEmail(newEmail).await()
        RESEND_WAIT
    }.mapFirebaseError()

    override suspend fun confirm(code: String): Result<String> = authFailure(AuthError.ServiceUnavailable)

    private companion object {
        const val LANGUAGE = "es"
        val RESEND_WAIT = 60.seconds
    }
}
