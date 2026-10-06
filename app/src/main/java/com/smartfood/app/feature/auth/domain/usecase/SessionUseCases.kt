package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.auth.domain.repository.SessionPreferences
import com.smartfood.app.feature.auth.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Nombre: SessionUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso de la sesión activa: decidir el destino inicial (HU-03), leer el perfil del
 * usuario actual (HU-05) y cerrar sesión (CA-05-03).
 */

/**
 * Determina si la app debe abrir directamente en Home.
 *
 * @param authRepository Repositorio de cuenta y sesión.
 * @param sessionPreferences Preferencias locales de sesión.
 */
class ResolveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionPreferences: SessionPreferences
) {
    /**
     * @return `true` si hay sesión y "Recuérdame" está activo (CA-03-01). Si hay sesión pero el usuario no pidió
     * recordarla, la cierra y devuelve `false` (CA-03-02).
     */
    suspend operator fun invoke(): Boolean {
        if (authRepository.currentUserId() == null) return false
        if (sessionPreferences.isRememberMeEnabled()) return true
        authRepository.logout()
        return false
    }
}

/**
 * Obtiene el perfil del usuario con sesión activa.
 *
 * @param authRepository Repositorio de cuenta y sesión.
 * @param userRepository Repositorio del perfil del usuario.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    /** @return El perfil, o un fallo con [AuthError.InvalidCredentials] si no hay sesión. */
    suspend operator fun invoke(): Result<User> {
        val uid = authRepository.currentUserId() ?: return authFailure(AuthError.InvalidCredentials)
        return userRepository.getProfile(uid)
    }
}

/**
 * Cierra la sesión activa.
 *
 * @param authRepository Repositorio de cuenta y sesión.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.logout()
}
