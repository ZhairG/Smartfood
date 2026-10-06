package com.smartfood.app.feature.auth.ui.login

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.ui.marksEmpty

/**
 * Nombre: LoginContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UDF de la pantalla de inicio de sesión (HU-02): estado, eventos y efectos.
 */

/**
 * Estado inmutable de [LoginScreen]. Los campos en rojo se derivan de [error], así hay una sola fuente de verdad.
 *
 * @property email Correo ingresado.
 * @property password Contraseña ingresada.
 * @property rememberMe Casilla "Recuérdame" (activa por defecto, CA-03-01).
 * @property isLoading Hay un inicio de sesión en curso.
 * @property error Último error de negocio, o `null`.
 */
@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val error: AuthError? = null
) {
    val isOffline: Boolean get() = error == AuthError.Network

    val emailHasError: Boolean
        get() = error.marksEmpty(AuthField.EMAIL) || error == AuthError.InvalidEmail ||
            error == AuthError.InvalidCredentials || error == AuthError.AccountNotFound

    val passwordHasError: Boolean
        get() = error.marksEmpty(AuthField.PASSWORD) || error == AuthError.InvalidCredentials
}

/** Acciones del usuario en [LoginScreen]. */
sealed interface LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data class RememberMeChanged(val isChecked: Boolean) : LoginEvent
    data object LoginClicked : LoginEvent
    data object RetryConnectionClicked : LoginEvent
}

/** Efectos de un solo uso emitidos por [LoginViewModel]. */
sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}
