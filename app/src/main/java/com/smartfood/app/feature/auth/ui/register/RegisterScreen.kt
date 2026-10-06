package com.smartfood.app.feature.auth.ui.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.feature.auth.ui.inlineError
import com.smartfood.app.feature.auth.ui.messageRes
import com.smartfood.app.feature.auth.ui.components.AuthErrorMessage
import com.smartfood.app.feature.auth.ui.components.AuthScreenScaffold
import com.smartfood.app.feature.auth.ui.components.FlexibleGap
import com.smartfood.app.feature.auth.ui.components.PasswordFieldWithChecklist
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodPromptLink
import com.smartfood.app.core.designsystem.components.SmartFoodTermsCheckbox
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.titleStyle

/**
 * <build>
 * <b>Nombre:</b> RegisterScreen.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 23/09/2026<br>
 * <b>Descripción:</b> Pantalla stateless de Registro ("Crea tu cuenta", HU-01). El estado y la navegación los<br>
 * gestiona [RegisterViewModel] a través de la ruta declarada en `AuthNavGraph`. Reutiliza [AuthScreenScaffold] para<br>
 * el layout base, insets y gestión de foco/teclado.<br>
 * </build>
 *
 * @param state Objeto de estado inmutable [RegisterUiState] proveniente del ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [RegisterEvent] hacia el ViewModel.
 * @param onNavigateToLogin Callback para navegar de regreso a la pantalla de Inicio de Sesión.
 * @param modifier Modificador opcional para la apariencia exterior.
 */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreenScaffold(
        isOffline = state.isOffline,
        isReconnecting = state.isLoading,
        onRetryConnection = remember(onEvent) { { onEvent(RegisterEvent.RetryConnectionClicked) } },
        headerContent = {
            RegisterWelcomeHeader()
            Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing))
        },
        modifier = modifier
    ) {
        RegisterFormFields(state = state, onEvent = onEvent)
        Spacer(Modifier.height(LocalFormMetrics.current.sectionSpacing))

        SmartFoodTermsCheckbox(
            checked = state.acceptedTerms,
            onCheckedChange = remember(onEvent) { { checked -> onEvent(RegisterEvent.AcceptedTermsChanged(checked)) } }
        )
        // Con alto de sobra, el botón y el enlace bajan como en Figma (nodo 585:4447, CA-UI-07-03).
        FlexibleGap(minHeight = LocalFormMetrics.current.sectionSpacing)

        RegisterActionButtons(
            isLoading = state.isLoading,
            enabled = state.acceptedTerms,
            onRegisterClicked = remember(onEvent) { { onEvent(RegisterEvent.RegisterClicked) } },
            onNavigateToLogin = onNavigateToLogin
        )
        Spacer(Modifier.weight(2f))
    }
}

/**
 * Textos de bienvenida y subtítulo de la pantalla de Registro.
 */
@Composable
private fun RegisterWelcomeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.register_welcome_title),
            style = LocalFormMetrics.current.titleStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(id = R.string.register_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Campos de entrada de datos para Nombre Completo, Correo y Contraseña en el Registro.
 */
@Composable
private fun RegisterFormFields(
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        SmartFoodTextField(
            label = stringResource(id = R.string.register_name_label),
            value = state.fullName,
            onValueChange = remember(onEvent) { { name -> onEvent(RegisterEvent.FullNameChanged(name)) } },
            placeholder = stringResource(id = R.string.register_name_placeholder),
            enabled = !state.isLoading,
            isError = state.fullNameHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(LocalFormMetrics.current.fieldSpacing))

        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            value = state.email,
            onValueChange = remember(onEvent) { { email -> onEvent(RegisterEvent.EmailChanged(email)) } },
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,
            isError = state.emailHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(LocalFormMetrics.current.fieldSpacing))

        PasswordFieldWithChecklist(
            label = stringResource(id = R.string.login_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(RegisterEvent.PasswordChanged(pass)) } },
            validation = state.passwordValidation,
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(RegisterEvent.RegisterClicked)
                }
            )
        )

        AuthErrorMessage(
            message = state.error.inlineError?.let { stringResource(id = it.messageRes()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Botones de acción principal (Crear Cuenta e Iniciar Sesión).
 */
@Composable
private fun ColumnScope.RegisterActionButtons(
    isLoading: Boolean,
    enabled: Boolean,
    onRegisterClicked: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    SmartFoodPrimaryButton(
        text = stringResource(id = R.string.register_button_text),
        loadingText = stringResource(id = R.string.register_button_loading),
        onClick = onRegisterClicked,
        isLoading = isLoading,
        enabled = enabled
    )

    FlexibleGap(minHeight = LocalFormMetrics.current.sectionSpacing)

    SmartFoodPromptLink(
        prompt = stringResource(id = R.string.register_already_have_account_prompt),
        action = stringResource(id = R.string.register_login_action),
        onClick = onNavigateToLogin,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    SmartfoodTheme {
        RegisterScreen(
            state = RegisterUiState(),
            onEvent = {},
            onNavigateToLogin = {}
        )
    }
}
