package com.smartfood.app.data.repository

/**
 * Nombre: AuthRepository.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Contrato de repositorio para operaciones de autenticación de la aplicación.
 */
interface AuthRepository {
    /**
     * Comprueba si existe un usuario actualmente autenticado en la sesión.
     *
     * @return `true` si hay un usuario activo; `false` en caso contrario.
     */
    fun isUserAuthenticated(): Boolean

    /**
     * Inicia sesión con credenciales de correo y contraseña.
     *
     * @param email Correo electrónico.
     * @param pass Contraseña.
     * @return [Result] que encapsula el resultado de la operación.
     */
    suspend fun login(email: String, pass: String): Result<Unit>

    /**
     * Registra un nuevo usuario en la plataforma.
     *
     * @param email Correo electrónico.
     * @param pass Contraseña.
     * @return [Result] que encapsula el resultado de la operación.
     */
    suspend fun register(email: String, pass: String): Result<Unit>

    /**
     * Envía un correo de restablecimiento de contraseña.
     *
     * @param email Correo electrónico registrado.
     * @return [Result] que encapsula el resultado de la operación.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Cierra la sesión activa del usuario.
     */
    fun logout()
}
