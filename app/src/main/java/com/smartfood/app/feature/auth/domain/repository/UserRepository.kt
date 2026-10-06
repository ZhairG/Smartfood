package com.smartfood.app.feature.auth.domain.repository

import com.smartfood.app.feature.auth.domain.model.User

/**
 * <build>
 * <b>Nombre:</b> UserRepository.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Contrato para guardar y leer el perfil del usuario (RF-AUTH-02).<br>
 * </build>
 */
interface UserRepository {

    /** Guarda el perfil de un usuario recién registrado. */
    suspend fun createProfile(user: User): Result<Unit>

    /** Obtiene el perfil del usuario con el [uid] indicado. */
    suspend fun getProfile(uid: String): Result<User>
}
