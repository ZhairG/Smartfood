package com.smartfood.app.feature.profile.domain.repository

import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: ProfileRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para leer y actualizar el perfil del usuario con sesión activa (spec 008, plan §3,
 * RF-PER-01). Los fallos llegan como `Result.failure` o como error del `Flow`, con una `AuthException`.
 */
interface ProfileRepository {

    /** Perfil del usuario con sesión. Emite de nuevo con cada cambio, también los hechos sin conexión. */
    fun observeProfile(): Flow<UserProfile>

    /**
     * Guarda el nombre, el teléfono, la fecha de nacimiento y la foto de [profile]. El correo no se escribe aquí: lo
     * cambia la cuenta con verificación (CA-PER-02-10) y luego [syncEmail].
     */
    suspend fun updateProfile(profile: UserProfile): Result<Unit>

    /** Iguala el correo del perfil al de la cuenta (CA-PER-02-12). */
    suspend fun syncEmail(email: String): Result<Unit>

    /** Preferencias alimentarias guardadas (CA-PER-03-04); si nunca se guardaron, devuelve unas vacías. */
    suspend fun getPreferences(): Result<FoodPreferences>

    /** Reemplaza el mapa `preferences` del perfil (CA-PER-03-04). Quitar una elección también la borra. */
    suspend fun savePreferences(preferences: FoodPreferences): Result<Unit>
}
