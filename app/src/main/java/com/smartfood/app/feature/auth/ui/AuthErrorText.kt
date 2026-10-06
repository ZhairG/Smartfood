package com.smartfood.app.feature.auth.ui

import androidx.annotation.StringRes
import com.smartfood.app.R
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField

/**
 * Nombre: AuthErrorText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único punto de traducción de [AuthError] a textos de `strings.xml` (RNF-03) y a campos
 * resaltados en rojo. Las pantallas no deciden mensajes por su cuenta.
 */

/** Mensaje para los formularios de inicio de sesión y registro. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    is AuthError.EmptyFields -> R.string.error_empty_fields
    AuthError.InvalidEmail -> R.string.error_invalid_email
    AuthError.WeakPassword -> R.string.error_weak_password
    AuthError.PasswordsDontMatch -> R.string.error_passwords_dont_match
    AuthError.TermsNotAccepted -> R.string.error_terms_not_accepted
    AuthError.InvalidCredentials -> R.string.error_invalid_credentials
    AuthError.AccountNotFound -> R.string.error_account_not_found
    AuthError.EmailAlreadyInUse -> R.string.error_account_already_exists
    AuthError.InvalidOtpCode -> R.string.error_invalid_otp_code
    AuthError.OtpCodeExpired -> R.string.error_otp_code_expired
    AuthError.TooManyAttempts -> R.string.error_too_many_attempts
    is AuthError.TooManyRequests -> R.string.error_too_many_requests
    AuthError.Network -> R.string.offline_dialog_title
    AuthError.ServiceUnavailable -> R.string.error_firebase_config_not_found
    is AuthError.Unknown -> R.string.error_unknown
}

/**
 * Mensaje para el flujo de recuperación, que tiene textos propios para los campos vacíos. No hay mensaje de
 * "cuenta no encontrada": la recuperación responde igual exista o no la cuenta (spec 003, CA-REC-01-02).
 */
@StringRes
fun AuthError.recoveryMessageRes(): Int = when {
    this is AuthError.EmptyFields && AuthField.OTP_CODE in fields -> R.string.error_empty_otp_code
    this is AuthError.EmptyFields -> R.string.error_empty_email
    else -> messageRes()
}

/** La falta de conexión se muestra con un diálogo (HU-06), no como mensaje dentro del formulario. */
val AuthError?.inlineError: AuthError?
    get() = this?.takeUnless { it == AuthError.Network }

/** Indica si el error señala el [field] como vacío. */
fun AuthError?.marksEmpty(field: AuthField): Boolean = this is AuthError.EmptyFields && field in fields
