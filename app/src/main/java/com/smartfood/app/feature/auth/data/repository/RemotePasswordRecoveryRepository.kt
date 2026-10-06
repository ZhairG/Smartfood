package com.smartfood.app.feature.auth.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: RemotePasswordRecoveryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación **real** de [PasswordRecoveryRepository] (spec 003). Llama a las Cloud Functions de
 * `functions/`, que envían el código por correo y cambian la contraseña con el Admin SDK. Los datos viajan como
 * `Map` para no depender de reflexión con R8, y cada fallo se clasifica con
 * [com.smartfood.app.feature.auth.data.mapper.FirebaseErrorMapper] según su `details.reason`.
 *
 * @param functions Instancia de [FirebaseFunctions] en la región de las funciones, provista por Hilt.
 */
class RemotePasswordRecoveryRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : PasswordRecoveryRepository {

    /** La respuesta trae `resendAfterSeconds`: la espera antes de poder pedir otro código (RF-REC-07). */
    override suspend fun requestCode(email: String): Result<Duration> =
        call(REQUEST_CODE, mapOf("email" to email)).map { response ->
            ((response as? Map<*, *>)?.get(RESEND_AFTER_SECONDS) as? Number)?.toLong()?.seconds ?: DEFAULT_RESEND_WAIT
        }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> =
        call(VERIFY_CODE, mapOf("email" to email, "code" to code)).map { }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        call(RESET_PASSWORD, mapOf("email" to email, "code" to code, "newPassword" to newPassword)).map { }

    /** Llama a la función y devuelve su respuesta (`data`), ya decodificada por el SDK. */
    private suspend fun call(function: String, data: Map<String, String>): Result<Any?> = safeRunCatching {
        functions.getHttpsCallable(function).call(data).await().data
    }.mapFirebaseError()

    private companion object {
        // Nombres de las funciones exportadas en `functions/src/index.ts`.
        const val REQUEST_CODE = "requestPasswordResetCode"
        const val VERIFY_CODE = "verifyPasswordResetCode"
        const val RESET_PASSWORD = "resetPasswordWithCode"

        const val RESEND_AFTER_SECONDS = "resendAfterSeconds"

        /** Espera por defecto si la respuesta no la informa; coincide con la del servidor. */
        val DEFAULT_RESEND_WAIT = 60.seconds
    }
}
