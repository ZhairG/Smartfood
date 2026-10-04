package com.smartfood.app.feature.auth.ui.recovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.smartfood.app.core.designsystem.components.SmartFoodOtpCodeInput
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodPromptLink
import com.smartfood.app.core.designsystem.components.SmartFoodStepIndicator
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTextLink
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.OrangeDark
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.titleStyle
import com.smartfood.app.feature.auth.ui.components.AuthErrorMessage
import com.smartfood.app.feature.auth.ui.components.AuthScreenScaffold
import com.smartfood.app.feature.auth.ui.components.FlexibleGap
import com.smartfood.app.feature.auth.ui.components.LocalFormViewport
import com.smartfood.app.feature.auth.ui.components.fillFormViewport
import com.smartfood.app.feature.auth.ui.components.PasswordFieldWithChecklist
import com.smartfood.app.feature.auth.ui.recoveryMessageRes

/**
 * Nombre: PasswordRecoveryScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla stateless del flujo de Recuperación de Contraseña (HU-04, **simulado** en la v1).
 * Gestiona las 3 etapas (correo, código de 4 dígitos y nueva contraseña) con el indicador de pasos
 * [SmartFoodStepIndicator]. Cada paso cabe sin scroll y el campo enfocado queda visible sin scroll programático
 * (spec 002); en el paso 3 los requisitos aparecen en un popup encima del campo enfocado (CA-UI-03-02).
 *
 * @param state Objeto de estado inmutable [PasswordRecoveryUiState] expuesto por el ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [PasswordRecoveryEvent] al ViewModel.
 * @param onNavigateToLogin Callback para regresar a la pantalla de Inicio de Sesión.
 * @param modifier Modificador opcional.
 */
@Composable
fun PasswordRecoveryScreen(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenScaffold(
        topHeaderContent = {
            SmartFoodStepIndicator(currentStep = state.step.stepIndex)
        },
        headerContent = {
            RecoveryHeader(step = state.step, email = state.email, onEvent = onEvent)
            Spacer(modifier = Modifier.height(LocalFormMetrics.current.sectionSpacing))
        },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "recovery_step_transition"
        ) { targetStep ->
            when (targetStep) {
                RecoveryStep.EMAIL_REQUEST -> ForgotPasswordStep(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToLogin = onNavigateToLogin
                )

                RecoveryStep.OTP_VERIFICATION -> VerifyCodeStep(state = state, onEvent = onEvent)

                RecoveryStep.NEW_PASSWORD -> CreateNewPasswordStep(state = state, onEvent = onEvent)
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
    onEvent: (PasswordRecoveryEvent) -> Unit,
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
            style = LocalFormMetrics.current.titleStyle,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (step == RecoveryStep.OTP_VERIFICATION) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email.ifBlank { stringResource(id = R.string.login_email_placeholder) },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            SmartFoodTextLink(
                text = stringResource(id = R.string.recovery_step2_wrong_email_link),
                onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.ChangeEmailClicked) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = OrangeDark  // Figma: #DE7D1C (nodo 585:4479)
            )
        }
    }
}

/** Texto del error actual según los mensajes propios del flujo de recuperación. */
@Composable
private fun PasswordRecoveryUiState.errorText(): String? =
    error?.let { stringResource(id = it.recoveryMessageRes()) }

/**
 * Formulario del Paso 1: Solicitud de código mediante correo.
 */
@Composable
private fun ForgotPasswordStep(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val metrics = LocalFormMetrics.current

    // Cada paso llena el alto visible del formulario para repartir el sobrante con sus FlexibleGap (CA-UI-07-03).
    Column(
        modifier = modifier.fillFormViewport(LocalFormViewport.current),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            value = state.email,
            onValueChange = remember(onEvent) { { email -> onEvent(PasswordRecoveryEvent.EmailChanged(email)) } },
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,
            isError = state.emailHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(PasswordRecoveryEvent.SendCodeClicked)
                }
            )
        )

        AuthErrorMessage(message = state.errorText(), modifier = Modifier.fillMaxWidth())

        FlexibleGap(minHeight = metrics.sectionSpacing, weight = 2f)  // Figma: nodo 585:4458

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step1_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.SendCodeClicked) } },
            isLoading = state.isLoading
        )

        FlexibleGap(minHeight = metrics.sectionSpacing, weight = 2f)

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.recovery_step1_back_prompt),
            action = stringResource(id = R.string.recovery_step1_back_action),
            onClick = onNavigateToLogin,
            fontSize = 18.sp  // Figma: nodo 585:4465
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Formulario del Paso 2: Verificación de código OTP de 4 dígitos.
 */
@Composable
private fun VerifyCodeStep(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalFormMetrics.current

    // Cada paso llena el alto visible del formulario para repartir el sobrante con sus FlexibleGap (CA-UI-07-03).
    Column(
        modifier = modifier.fillFormViewport(LocalFormViewport.current),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartFoodOtpCodeInput(
            code = state.otpCode,
            onCodeChanged = remember(onEvent) { { code -> onEvent(PasswordRecoveryEvent.OtpCodeChanged(code)) } },
            isError = state.otpCodeHasError,
            enabled = !state.isLoading,
            onDone = remember(onEvent) { { onEvent(PasswordRecoveryEvent.VerifyCodeClicked) } }
        )

        AuthErrorMessage(message = state.errorText(), modifier = Modifier.fillMaxWidth())

        FlexibleGap(minHeight = metrics.sectionSpacing, weight = 3f)  // Figma: nodo 585:4467

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step2_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.VerifyCodeClicked) } },
            isLoading = state.isLoading
        )

        FlexibleGap(minHeight = metrics.sectionSpacing)

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.recovery_step2_resend_prompt),
            action = stringResource(id = R.string.recovery_step2_resend_action),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.ResendCodeClicked) } },
            fontSize = 16.sp  // Figma: nodo 585:4474
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Formulario del Paso 3: Creación de nueva contraseña. Los requisitos se muestran en un popup encima del campo
 * enfocado, incluida la regla "¿Coinciden?" (CA-UI-03-02 y CA-UI-03-06).
 */
@Composable
private fun CreateNewPasswordStep(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val metrics = LocalFormMetrics.current

    // Cada paso llena el alto visible del formulario para repartir el sobrante con sus FlexibleGap (CA-UI-07-03).
    Column(
        modifier = modifier.fillFormViewport(LocalFormViewport.current),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PasswordFieldWithChecklist(
            label = stringResource(id = R.string.recovery_step3_new_password_label),
            value = state.newPassword,
            onValueChange = remember(onEvent) { { pass -> onEvent(PasswordRecoveryEvent.NewPasswordChanged(pass)) } },
            validation = state.passwordValidation,
            passwordsMatch = state.passwordsMatch,
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(metrics.fieldSpacing))

        PasswordFieldWithChecklist(
            label = stringResource(id = R.string.recovery_step3_confirm_password_label),
            value = state.confirmPassword,
            onValueChange = remember(onEvent) { { pass -> onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged(pass)) } },
            validation = state.passwordValidation,
            passwordsMatch = state.passwordsMatch,
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (state.canResetPassword) onEvent(PasswordRecoveryEvent.ResetPasswordClicked)
                }
            )
        )

        AuthErrorMessage(message = state.errorText(), modifier = Modifier.fillMaxWidth())

        // Figma (nodo 585:4481): el botón queda al pie del panel.
        FlexibleGap(minHeight = metrics.sectionSpacing)

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.recovery_step3_button),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = remember(onEvent) { { onEvent(PasswordRecoveryEvent.ResetPasswordClicked) } },
            enabled = state.canResetPassword,
            isLoading = state.isLoading
        )
    }
}

@Preview(showBackground = true, name = "Paso 1: Solicitud de Correo")
@Composable
private fun PasswordRecoveryStep1Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = PasswordRecoveryUiState(step = RecoveryStep.EMAIL_REQUEST),
            onEvent = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "Paso 2: Código OTP")
@Composable
private fun PasswordRecoveryStep2Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = PasswordRecoveryUiState(
                step = RecoveryStep.OTP_VERIFICATION,
                email = "nombre@correo.com",
                otpCode = "442"
            ),
            onEvent = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "Paso 3: Nueva Contraseña")
@Composable
private fun PasswordRecoveryStep3Preview() {
    SmartfoodTheme {
        PasswordRecoveryScreen(
            state = PasswordRecoveryUiState(
                step = RecoveryStep.NEW_PASSWORD,
                email = "nombre@correo.com"
            ),
            onEvent = {},
            onNavigateToLogin = {}
        )
    }
}
