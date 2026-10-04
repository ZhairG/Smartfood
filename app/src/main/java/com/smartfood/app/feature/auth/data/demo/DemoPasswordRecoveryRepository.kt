package com.smartfood.app.feature.auth.data.demo

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.PasswordRecoveryRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Nombre: DemoPasswordRecoveryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación **simulada** de [PasswordRecoveryRepository] (RF-AUTH-07, ADR-01).
 * No envía correos ni cambia contraseñas reales: valida contra los datos fijos de [DemoRecoveryData]
 * para demostrar el flujo de 3 pasos. Para pasar a la versión real basta con enlazar otra implementación
 * en `AuthDataModule`.
 */
class DemoPasswordRecoveryRepository @Inject constructor() : PasswordRecoveryRepository {

    override suspend fun requestCode(email: String): Result<Unit> {
        delay(DemoRecoveryData.SIMULATED_LATENCY_MS)
        return if (email.isRegistered()) Result.success(Unit) else authFailure(AuthError.AccountNotFound)
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> {
        delay(DemoRecoveryData.SIMULATED_LATENCY_MS)
        return when {
            !email.isRegistered() -> authFailure(AuthError.AccountNotFound)
            code != DemoRecoveryData.VALID_CODE -> authFailure(AuthError.InvalidOtpCode)
            else -> Result.success(Unit)
        }
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        verifyCode(email, code)

    private fun String.isRegistered(): Boolean = lowercase() in DemoRecoveryData.REGISTERED_EMAILS
}

/**
 * Datos de prueba de la recuperación simulada, documentados en `specs/001-autenticacion/spec.md` (HU-04).
 */
object DemoRecoveryData {
    /** Correos que la simulación considera registrados. */
    val REGISTERED_EMAILS = setOf("demo@smartfood.com", "prueba@smartfood.com")

    /** Único código de verificación aceptado. */
    const val VALID_CODE = "1234"

    /** Latencia artificial para que la UI muestre el estado de carga. */
    const val SIMULATED_LATENCY_MS = 800L
}
