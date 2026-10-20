package com.smartfood.app.feature.profile.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: RemoteEmailChangeRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [EmailChangeRepository] con las Cloud Functions `requestEmailChangeCode` y
 * `confirmEmailChange` (spec 008, T009b). Copia el patrón de `RemotePasswordRecoveryRepository`: los datos viajan como
 * `Map` (sin reflexión con R8) y los fallos se clasifican por su `details.reason` en `FirebaseErrorMapper`.
 *
 * @param functions Instancia de [FirebaseFunctions] en la región de las funciones, provista por Hilt.
 */
class RemoteEmailChangeRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : EmailChangeRepository {

    override suspend fun requestCode(newEmail: String): Result<Duration> =
        call(REQUEST_CODE, mapOf("newEmail" to newEmail)).map { response ->
            ((response as? Map<*, *>)?.get(RESEND_AFTER_SECONDS) as? Number)?.toLong()?.seconds ?: DEFAULT_RESEND_WAIT
        }

    override suspend fun confirm(code: String): Result<String> =
        call(CONFIRM, mapOf("code" to code)).map { response ->
            (response as? Map<*, *>)?.get(EMAIL) as? String
                ?: throw AuthException(AuthError.Unknown("La función no devolvió el correo nuevo"))
        }

    /** Llama a la función y devuelve su respuesta (`data`), ya decodificada por el SDK. */
    private suspend fun call(function: String, data: Map<String, String>): Result<Any?> = safeRunCatching {
        functions.getHttpsCallable(function).call(data).await().data
    }.mapFirebaseError()

    private companion object {
        // Nombres de las funciones exportadas en `functions/src/index.ts`.
        const val REQUEST_CODE = "requestEmailChangeCode"
        const val CONFIRM = "confirmEmailChange"

        const val RESEND_AFTER_SECONDS = "resendAfterSeconds"
        const val EMAIL = "email"

        /** Espera por defecto si la respuesta no la informa; coincide con la del servidor. */
        val DEFAULT_RESEND_WAIT = 60.seconds
    }
}
