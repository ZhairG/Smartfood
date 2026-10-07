package com.smartfood.app.feature.auth.domain.repository

import com.smartfood.app.feature.auth.domain.model.User

/**
 * Nombre: UserRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para guardar y leer el perfil del usuario (RF-AUTH-02, RF-LEG-03).
 */
interface UserRepository {

    /**
     * Guarda el perfil de un usuario recién registrado junto con la constancia de aceptación de los documentos
     * legales (spec 005, CA-LEG-02-01). La fecha de aceptación la pone el servidor.
     *
     * @param acceptedTermsVersion Versión de los Términos y la Política que aceptó.
     */
    suspend fun createProfile(user: User, acceptedTermsVersion: String): Result<Unit>

    /** Obtiene el perfil del usuario con el [uid] indicado. */
    suspend fun getProfile(uid: String): Result<User>
}
