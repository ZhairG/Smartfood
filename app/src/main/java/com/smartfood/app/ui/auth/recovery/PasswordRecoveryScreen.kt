package com.smartfood.app.ui.auth.recovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.ui.auth.AuthErrorType
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.auth.RecoveryStep
import com.smartfood.app.ui.auth.components.AuthErrorMessage
import com.smartfood.app.ui.auth.components.AuthScreenScaffold
import com.smartfood.app.ui.auth.components.PasswordRequirementChecklist
import com.smartfood.app.ui.components.SmartFoodOtpCodeInput
import com.smartfood.app.ui.components.SmartFoodPasswordField
import com.smartfood.app.ui.components.SmartFoodPrimaryButton
import com.smartfood.app.ui.components.SmartFoodPromptLink
import com.smartfood.app.ui.components.SmartFoodStepIndicator
import com.smartfood.app.ui.components.SmartFoodTextField
import com.smartfood.app.ui.components.SmartFoodTextLink
import com.smartfood.app.ui.theme.SmartfoodTheme
import kotlinx.coroutines.launch

/**
 * Nombre: PasswordRecoveryScreen.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pantalla principal del flujo de Recuperación de Contraseña según diseño de Figma.
 * Gestiona de forma unificada las 3 etapas (Paso 1: Solicitud de código, Paso 2: Verificación OTP,
 * Paso 3: Definición de nueva contraseña) con el indicador de pasos animado [SmartFoodStepIndicator]
 * y desplazamiento automático sobre el teclado mediante [ScrollState].
 *
 * @param state Objeto de estado inmutable [AuthUiState] expuesto por el ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [AuthEvent] al ViewModel.
 * @param onNavigateToLogin Callback para regresar a la pantalla de Inicio de Sesión.
 * @param onNavigateToSuccess Callback ejecutado al restablecer exitosamente la contraseña para mostrar [com.smartfood.app.ui.components.ActionSuccessScreen].
 * @param modifier Modificador opcional.
 */
@Composable
fun PasswordRecoveryScreen(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.isPasswordResetSuccess) {
        if (state.isPasswordResetSuccess) {
            onNavigateToSuccess()
        }
    }

    AuthScreenScaffold(
        state = state,
        onEvent = onEvent,
        topHeaderContent = {
            SmartFoodStepIndicator(currentStep = state.recoveryStep.stepIndex)
        },
        headerContent = {
            RecoveryHeader(step = state.recoveryStep, email = state.email, onEvent = onEvent)
            Spacer(modifier = Modifier.height(24.dp))
        },
        modifier = modifier
    ) { scrollState ->
        AnimatedContent(
            targetState = state.recoveryStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "recovery_step_transition"
        ) { targetStep ->
            when (targetStep) {
                RecoveryStep.EMAIL_REQUEST -> {
                    ForgotPasswordStep(
                        state = state,
                        scrollState = scrollState,
                        onEvent = onEvent,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }

                RecoveryStep.OTP_VERIFICATION -> {
                    VerifyCodeStep(
                        state = state,
                        scrollState = scrollState,
                        onEvent = onEvent
                    )
                }

                RecoveryStep.NEW_PASSWORD -> {
                    CreateNewPasswordStep(
                        state = state,
                        scrollState = scrollState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

/**
 * Encabezado con título y subtítulo adaptable para cada paso.
 */
@Composable
private fun RecoveryHeader(
    step: RecoveryStep,
    email: String,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val title = when (step) {
            RecoveryStep.EMAIL_REQUEST -> stringResource(id = R.string.recovery_step1_title)
            RecoveryStep.OTP_VERIFICATION -> stringResource(id = R.string.recovery_step2_title)
            RecoveryStep.NEW_PASSWORD -> stringResource(id = R.string.recovery_step3_title)
        }

        val subtitle = when (step) {
            RecoveryStep.EMAIL_REQUEST -> stringResource(id = R.string.recovery_step1_subtitle)
            RecoveryStep.OTP_VERIFICATION -> stringResource(id = R.string.recovery_step2_subtitle)
            RecoveryStep.NEW_PASSWORD -> stringResource(id = R.string.recovery_step3_subtitle)
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (step == RecoveryStep.OTP_VERIFICATION) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = email.ifBlank { "nombre@correo.com" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SmartFoodTextLink(
                text = stringResource(id = R.string.recovery_step2_wrong_email_link),
                onClick = remember(onEvent) { { onEvent(AuthEvent.ChangeEmailClicked) } },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Formulario del Paso 1: Solicitud de código mediante correo.
 */
@Composable
private fun ForgotPasswordStep(
    state: AuthUiState,
    scrollState: ScrollState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            value = state.email,
            onValueChange = remember(onEvent) { { email -> onEvent(AuthEvent.EmailChanged(email)) } },
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,
            isError = state.emailHasError,
            onFocusChanged = { focused ->
                if (focused) {
                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(AuthEvent.SendRecoveryCodeClicked)
                }
            )
        )

        AuthErrorMessage(
            message = when (state.errorType) {
                AuthErrorType.EMPTY_EMAIL -> stringResource(id = R.string.error_empty_email)
                AuthErrorType.ACCOUNT_NOT_FOUND_RECOVERY -> stringResource(id = R.string.error_account_not_found_recovery)
                AuthErrorType.INVALID_EMAIL -> stringResource(id = R.string.error_invalid_email)
                else -> state.errorMessage
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step1_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(AuthEvent.SendRecoveryCodeClicked) } },
            isLoading = state.isLoading
        )

        Spacer(modifier = Modifier.height(26.dp))

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.recovery_step1_back_prompt),
            action = stringResource(id = R.string.recovery_step1_back_action),
            onClick = onNavigateToLogin
        )
    }
}

/**
 * Formulario del Paso 2: Verificación de código OTP de 4 dígitos.
 */
@Composable
private fun VerifyCodeStep(
    state: AuthUiState,
    scrollState: ScrollState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartFoodOtpCodeInput(
            code = state.otpCode,
            onCodeChanged = remember(onEvent) {
                { code ->
                    onEvent(AuthEvent.OtpCodeChanged(code))
                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                }
            },
            isError = state.otpCodeHasError,
            enabled = !state.isLoading
        )

        AuthErrorMessage(
            message = when (state.errorType) {
                AuthErrorType.EMPTY_OTP_CODE -> stringResource(id = R.string.error_empty_otp_code)
                AuthErrorType.INVALID_OTP_CODE -> stringResource(id = R.string.error_invalid_otp_code)
                else -> state.errorMessage
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step2_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(AuthEvent.VerifyOtpCodeClicked) } },
            isLoading = state.isLoading
        )

        Spacer(modifier = Modifier.height(26.dp))

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.recovery_step2_resend_prompt),
            action = stringResource(id = R.string.recovery_step2_resend_action),
            onClick = remember(onEvent) { { onEvent(AuthEvent.ResendOtpCodeClicked) } }
        )
    }
}

/**
 * Formulario del Paso 3: Creación de nueva contraseña.
 */
@Composable
private fun CreateNewPasswordStep(
    state: AuthUiState,
    scrollState: ScrollState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val isFormValid = state.hasMinimumLength &&
            state.hasUppercase &&
            state.hasSpecialCharacter &&
            state.passwordsMatch

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartFoodPasswordField(
            label = stringResource(id = R.string.recovery_step3_new_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(AuthEvent.PasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            onFocusChanged = { focused ->
                if (focused) {
                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                }
            },
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(14.dp))

        SmartFoodPasswordField(
            label = stringResource(id = R.string.recovery_step3_confirm_password_label),
            value = state.confirmPassword,
            onValueChange = remember(onEvent) { { pass -> onEvent(AuthEvent.ConfirmPasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            onFocusChanged = { focused ->
                if (focused) {
                    coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                }
            },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isFormValid) {
                        onEvent(AuthEvent.ResetPasswordClicked)
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        PasswordRequirementChecklist(
            hasMinimumLength = state.hasMinimumLength,
            hasUppercase = state.hasUppercase,
            hasSpecialCharacter = state.hasSpecialCharacter,
            passwordsMatch = state.passwordsMatch
        )

        Spacer(modifier = Modifier.height(28.dp))

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step3_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(AuthEvent.ResetPasswordClicked) } },
            enabled = isFormValid,
            isLoading = state.isLoading
        )
    }
}

@Preview(showBackground = true, name = "Paso 1: Solicitud de Correo")
@Composable
private fun PasswordRecoveryStep1Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = AuthUiState(recoveryStep = RecoveryStep.EMAIL_REQUEST),
            onEvent = {},
            onNavigateToLogin = {},
            onNavigateToSuccess = {}
        )
    }
}

@Preview(showBackground = true, name = "Paso 2: Código OTP")
@Composable
private fun PasswordRecoveryStep2Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = AuthUiState(
                recoveryStep = RecoveryStep.OTP_VERIFICATION,
                email = "nombre@correo.com",
                otpCode = "442"
            ),
            onEvent = {},
            onNavigateToLogin = {},
            onNavigateToSuccess = {}
        )
    }
}

@Preview(showBackground = true, name = "Paso 3: Nueva Contraseña")
@Composable
private fun PasswordRecoveryStep3Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = AuthUiState(
                recoveryStep = RecoveryStep.NEW_PASSWORD,
                email = "nombre@correo.com"
            ),
            onEvent = {},
            onNavigateToLogin = {},
            onNavigateToSuccess = {}
        )
    }
}
