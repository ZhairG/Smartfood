package com.smartfood.app.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para operaciones de autenticación con Firebase u otras fuentes.
 */
interface AuthRepository {
    fun isUserAuthenticated(): Boolean
    suspend fun login(email: String, pass: String): Result<Unit>
    suspend fun register(email: String, pass: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}
