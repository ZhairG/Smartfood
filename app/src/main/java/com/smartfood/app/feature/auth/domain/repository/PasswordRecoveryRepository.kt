package com.smartfood.app.feature.auth.domain.repository

/**
 * Nombre: PasswordRecoveryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato del flujo de recuperación de contraseña en 3 pasos (RF-AUTH-07).
 * En la v1 lo implementa una versión simulada con datos fijos (ver `specs/001-autenticacion/plan.md`, ADR-01);
 * una implementación real solo tiene que respetar este contrato.
 */
interface PasswordRecoveryRepository {

    /** Paso 1: solicita el envío de un código de verificación al [email]. */
    suspend fun requestCode(email: String): Result<Unit>

    /** Paso 2: verifica que el [code] corresponda al [email]. */
    suspend fun verifyCode(email: String, code: String): Result<Unit>

    /** Paso 3: establece la [newPassword] de la cuenta, validando de nuevo el [code]. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>
}
