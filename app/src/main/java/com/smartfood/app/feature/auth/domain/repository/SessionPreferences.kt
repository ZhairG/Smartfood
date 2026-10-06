package com.smartfood.app.feature.auth.domain.repository

/**
 * Nombre: SessionPreferences.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de las preferencias locales de sesión, como "Recuérdame" (RF-AUTH-06).
 */
interface SessionPreferences {

    /** Indica si el usuario pidió mantener la sesión abierta. Por defecto es `true`. */
    suspend fun isRememberMeEnabled(): Boolean

    /** Guarda la preferencia "Recuérdame". */
    suspend fun setRememberMe(enabled: Boolean)
}
