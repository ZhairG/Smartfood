package com.smartfood.app.feature.auth.domain.repository

import com.smartfood.app.feature.auth.domain.model.User

/**
 * Nombre: UserRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para guardar y leer el perfil del usuario (RF-AUTH-02).
 */
interface UserRepository {

    /** Guarda el perfil de un usuario recién registrado. */
    suspend fun createProfile(user: User): Result<Unit>

    /** Obtiene el perfil del usuario con el [uid] indicado. */
    suspend fun getProfile(uid: String): Result<User>
}
