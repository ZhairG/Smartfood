package com.smartfood.app.feature.profile.ui.password

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult

/**
 * Nombre: ChangePasswordContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UI para la pantalla Cambiar contraseña (spec 008, HU-PER-05; Figma 603:2358 y 603:2460).
 */

@Immutable
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordValidation: PasswordValidationResult = PasswordValidationResult.NONE,
    val isLoading: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val currentPasswordError: AuthError? = null,
    val generalError: AuthError? = null,
) {
    val passwordsMatch: Boolean
        get() = newPassword.isNotEmpty() && newPassword == confirmPassword

    val canSubmit: Boolean
        get() = currentPassword.isNotEmpty() && passwordValidation.isValid && passwordsMatch && !isLoading
}

sealed interface ChangePasswordEvent {
    data class CurrentPasswordChanged(val value: String) : ChangePasswordEvent
    data class NewPasswordChanged(val value: String) : ChangePasswordEvent
    data class ConfirmPasswordChanged(val value: String) : ChangePasswordEvent
    data object SubmitClicked : ChangePasswordEvent
    data object SuccessDialogDismissed : ChangePasswordEvent
}

sealed interface ChangePasswordEffect {
    data object NavigateBack : ChangePasswordEffect
}
