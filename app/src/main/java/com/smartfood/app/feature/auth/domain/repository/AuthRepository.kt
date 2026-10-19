package com.smartfood.app.feature.auth.domain.repository

/**
 * Nombre: AuthRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de las operaciones de cuenta y sesión (RF-AUTH-01, RF-AUTH-05).
 * Toda falla se entrega como `Result.failure` con un
 * [com.smartfood.app.feature.auth.domain.model.AuthException].
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

    /** Elimina la cuenta con sesión activa. Se usa como rollback del registro. */
    suspend fun deleteCurrentAccount(): Result<Unit>

    /** Cierra la sesión activa. */
    fun logout()

    /** Correo de la cuenta con sesión, o `null` si no hay sesión (spec 008, CA-PER-02-12). */
    fun currentEmail(): String?

    /**
     * Confirma la identidad con la contraseña actual, como exige Firebase antes de cambiar el correo o la contraseña
     * (spec 008, CA-PER-02-10 y HU-PER-05). Una contraseña incorrecta falla con `InvalidCredentials`.
     */
    suspend fun reauthenticate(currentPassword: String): Result<Unit>

    /**
     * Pide cambiar el correo: Firebase envía un enlace a [newEmail] y el cambio se aplica cuando se abre
     * (spec 008, CA-PER-02-10). Requiere haber llamado antes a [reauthenticate].
     */
    suspend fun requestEmailChange(newEmail: String): Result<Unit>
}
