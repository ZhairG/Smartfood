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
     * Envía una solicitud de restablecimiento de contraseña vía Firebase Auth y genera un código OTP real.
     *
     * @param email Correo electrónico registrado.
     * @return [Result] que encapsula el código OTP de 4 dígitos generado.
     */
    suspend fun sendPasswordReset(email: String): Result<String>

    /**
     * Confirma el restablecimiento de contraseña con el código de verificación y actualiza las credenciales en Firebase Auth.
     *
     * @param email Correo electrónico del usuario.
     * @param code Código de verificación OTP o acción de Firebase.
     * @param newPass Nueva contraseña.
     * @return [Result] que encapsula el resultado de la operación.
     */
    suspend fun confirmPasswordReset(email: String, code: String, newPass: String): Result<Unit>

    /**
     * Cierra la sesión activa del usuario.
     */
    fun logout()
}
