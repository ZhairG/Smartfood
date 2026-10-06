package com.smartfood.app.feature.auth.domain.repository

/**
 * <build>
 * <b>Nombre:</b> SessionPreferences.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Contrato de las preferencias locales de sesión, como "Recuérdame" (RF-AUTH-06).<br>
 * </build>
 */
interface SessionPreferences {

    /** Indica si el usuario pidió mantener la sesión abierta. Por defecto es `true`. */
    suspend fun isRememberMeEnabled(): Boolean

    /** Guarda la preferencia "Recuérdame". */
    suspend fun setRememberMe(enabled: Boolean)
}
