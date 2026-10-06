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
 * Descripción: Implementación **simulada** de [PasswordRecoveryRepository], el modo demo de la spec 003
 * (RF-REC-06). No envía correos ni cambia contraseñas reales: valida contra los datos fijos de [DemoRecoveryData]
 * para demostrar el flujo de 3 pasos sin backend. Responde igual que la versión real ante un correo sin cuenta
 * (CA-REC-01-02). Solo se usa con `-PrecoveryDemo=true` en debug (ver `AuthDataModule`).
 */
class DemoPasswordRecoveryRepository @Inject constructor() : PasswordRecoveryRepository {

    override suspend fun requestCode(email: String): Result<Unit> {
        delay(DemoRecoveryData.SIMULATED_LATENCY_MS)
        // Respuesta neutra: no revela si el correo tiene cuenta.
        return Result.success(Unit)
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> {
        delay(DemoRecoveryData.SIMULATED_LATENCY_MS)
        return if (email.isRegistered() && code == DemoRecoveryData.VALID_CODE) {
            Result.success(Unit)
        } else {
            authFailure(AuthError.InvalidOtpCode)
        }
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        verifyCode(email, code)

    private fun String.isRegistered(): Boolean = lowercase() in DemoRecoveryData.REGISTERED_EMAILS
}

/**
 * Datos de prueba del modo demo, documentados en `specs/001-autenticacion/spec.md` (HU-04) y en el README.
 */
object DemoRecoveryData {
    /** Correos que la simulación considera registrados. */
    val REGISTERED_EMAILS = setOf("demo@smartfood.com", "prueba@smartfood.com")

    /** Único código de verificación aceptado. */
    const val VALID_CODE = "1234"

    /** Latencia artificial para que la UI muestre el estado de carga. */
    const val SIMULATED_LATENCY_MS = 800L
}
