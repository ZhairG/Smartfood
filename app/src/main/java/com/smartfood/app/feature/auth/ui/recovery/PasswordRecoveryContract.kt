package com.smartfood.app.feature.auth.ui.recovery

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import com.smartfood.app.feature.auth.ui.marksEmpty

/**
 * Nombre: PasswordRecoveryContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato UDF del flujo de recuperación de contraseña (HU-04 y spec 003): pasos, estado, eventos
 * y efectos.
 */

/** Pasos del flujo, en el orden del indicador de pasos. */
enum class RecoveryStep(val stepIndex: Int) {
    /** Paso 1: ingresar el correo. */
    EMAIL_REQUEST(0),

    /** Paso 2: ingresar el código de 4 dígitos. */
    OTP_VERIFICATION(1),

    /** Paso 3: definir y confirmar la nueva contraseña. */
    NEW_PASSWORD(2)
}

/**
 * Estado inmutable de [PasswordRecoveryScreen]. El código válido **no** forma parte del estado:
 * solo lo conoce el repositorio.
 *
 * @property step Paso actual.
 * @property email Correo ingresado en el paso 1.
 * @property otpCode Código ingresado en el paso 2.
 * @property newPassword Nueva contraseña del paso 3.
 * @property confirmPassword Confirmación del paso 3.
 * @property passwordValidation Cumplimiento en tiempo real de la política de contraseña (CA-04-10).
 * @property isLoading Hay una operación en curso.
 * @property error Último error de negocio, o `null`.
 */
@Immutable
data class PasswordRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.EMAIL_REQUEST,
    val email: String = "",
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordValidation: PasswordValidationResult = PasswordValidationResult.NONE,
    val isLoading: Boolean = false,
    val error: AuthError? = null
) {
    val emailHasError: Boolean
        get() = error.marksEmpty(AuthField.EMAIL) || error == AuthError.InvalidEmail

    /** Las casillas se marcan con cualquier error del código (CA-04-06, CA-REC-03-06). */
    val otpCodeHasError: Boolean
        get() = error.marksEmpty(AuthField.OTP_CODE) || error.isOtpCodeError

    val passwordHasError: Boolean
        get() = error == AuthError.WeakPassword || error == AuthError.PasswordsDontMatch

    val passwordsMatch: Boolean get() = newPassword.isNotEmpty() && newPassword == confirmPassword

    /** El botón del paso 3 solo se habilita con una contraseña válida y confirmada (CA-04-10). */
    val canResetPassword: Boolean get() = passwordValidation.isValid && passwordsMatch
}

/** Errores que invalidan el código escrito: incorrecto, vencido o con los intentos agotados (spec 003). */
val AuthError?.isOtpCodeError: Boolean
    get() = this == AuthError.InvalidOtpCode || this == AuthError.OtpCodeExpired || this == AuthError.TooManyAttempts

/** Acciones del usuario en [PasswordRecoveryScreen]. */
sealed interface PasswordRecoveryEvent {
    data class EmailChanged(val email: String) : PasswordRecoveryEvent
    data object SendCodeClicked : PasswordRecoveryEvent
    data class OtpCodeChanged(val code: String) : PasswordRecoveryEvent
    data object VerifyCodeClicked : PasswordRecoveryEvent
    data object ResendCodeClicked : PasswordRecoveryEvent
    data object ChangeEmailClicked : PasswordRecoveryEvent
    data class NewPasswordChanged(val password: String) : PasswordRecoveryEvent
    data class ConfirmPasswordChanged(val password: String) : PasswordRecoveryEvent
    data object ResetPasswordClicked : PasswordRecoveryEvent
}

/** Efectos de un solo uso emitidos por [PasswordRecoveryViewModel]. */
sealed interface PasswordRecoveryEffect {
    data object NavigateToSuccess : PasswordRecoveryEffect
}
