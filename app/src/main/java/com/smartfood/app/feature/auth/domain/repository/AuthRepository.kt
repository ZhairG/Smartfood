package com.smartfood.app.feature.auth.domain.repository

/**
 * <build>
 * <b>Nombre:</b> AuthRepository.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Contrato de las operaciones de cuenta y sesión (RF-AUTH-01, RF-AUTH-03, RF-AUTH-05). Toda<br>
 * falla se entrega como `Result.failure` con un [com.smartfood.app.feature.auth.domain.model.AuthException].<br>
 * </build>
 */
interface AuthRepository {

    /** Identificador del usuario con sesión activa, o `null` si no hay sesión. */
    fun currentUserId(): String?

    /** Inicia sesión con correo y contraseña. */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Crea una cuenta nueva. Al terminar, la sesión queda iniciada con esa cuenta.
     *
     * @return El `uid` de la cuenta creada.
     */
    suspend fun createAccount(email: String, password: String): Result<String>

    /** Envía el correo de verificación a la cuenta con sesión activa. */
    suspend fun sendEmailVerification(): Result<Unit>

    /** Elimina la cuenta con sesión activa. Se usa como rollback del registro. */
    suspend fun deleteCurrentAccount(): Result<Unit>

    /** Cierra la sesión activa. */
    fun logout()
}
