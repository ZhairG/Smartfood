package com.smartfood.app.feature.auth.domain.repository

import kotlin.time.Duration

/**
 * <build>
 * <b>Nombre:</b> PasswordRecoveryRepository.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Contrato del flujo de recuperación de contraseña en 3 pasos (RF-AUTH-07). En la v1 lo<br>
 * implementa una versión simulada con datos fijos (ver `specs/001-autenticacion/plan.md`, ADR-01); una<br>
 * implementación real solo tiene que respetar este contrato.<br>
 * </build>
 */
interface PasswordRecoveryRepository {

    /**
     * Paso 1: solicita el envío de un código de verificación al [email].
     *
     * @return El tiempo que falta para poder pedir otro código (spec 003, RF-REC-07).
     */
    suspend fun requestCode(email: String): Result<Duration>

    /** Paso 2: verifica que el [code] corresponda al [email]. */
    suspend fun verifyCode(email: String, code: String): Result<Unit>

    /** Paso 3: establece la [newPassword] de la cuenta, validando de nuevo el [code]. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>
}
