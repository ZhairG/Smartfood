package com.smartfood.app.feature.auth.ui.register

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import com.smartfood.app.feature.auth.ui.marksEmpty

/**
 * Nombre: RegisterContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UDF de la pantalla de registro (HU-01): estado, eventos y efectos.
 */

/**
 * Estado inmutable de [RegisterScreen].
 *
 * @property fullName Nombre completo ingresado.
 * @property email Correo ingresado.
 * @property password Contraseña ingresada.
 * @property passwordValidation Cumplimiento en tiempo real de cada regla de la contraseña (CA-01-03).
 * @property acceptedTerms Casilla de Términos y condiciones. Sin aceptarla no se puede registrar (CA-01-05).
 * @property isLoading Hay un registro en curso.
 * @property error Último error de negocio, o `null`.
 */
@Immutable
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val passwordValidation: PasswordValidationResult = PasswordValidationResult.NONE,
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val error: AuthError? = null
) {
    val isOffline: Boolean get() = error == AuthError.Network

    val fullNameHasError: Boolean get() = error.marksEmpty(AuthField.FULL_NAME)

    val emailHasError: Boolean
        get() = error.marksEmpty(AuthField.EMAIL) || error == AuthError.InvalidEmail ||
            error == AuthError.EmailAlreadyInUse

    val passwordHasError: Boolean
        get() = error.marksEmpty(AuthField.PASSWORD) || error == AuthError.WeakPassword
}

/** Acciones del usuario en [RegisterScreen]. */
sealed interface RegisterEvent {
    data class FullNameChanged(val fullName: String) : RegisterEvent
    data class EmailChanged(val email: String) : RegisterEvent
    data class PasswordChanged(val password: String) : RegisterEvent
    data class AcceptedTermsChanged(val isChecked: Boolean) : RegisterEvent
    data object RegisterClicked : RegisterEvent
    data object RetryConnectionClicked : RegisterEvent
}

/** Efectos de un solo uso emitidos por [RegisterViewModel]. */
sealed interface RegisterEffect {
    data object NavigateToSuccess : RegisterEffect
}
