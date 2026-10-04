package com.smartfood.app.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.smartfood.app.core.designsystem.components.SmartFoodCheckbox
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodPromptLink
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTextLink
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * Nombre: LoginScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla stateless de Inicio de Sesión (HU-02). El estado y la navegación los gestiona
 * [LoginViewModel] a través de la ruta declarada en `AuthNavGraph`.
 * Reutiliza [AuthScreenScaffold] para el layout base, insets y gestión de foco/teclado.
 *
 * @param state Objeto de estado inmutable [LoginUiState] proveniente del ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [LoginEvent] hacia el ViewModel.
 * @param onNavigateToRegister Callback para navegar a la pantalla de Registro.
 * @param onNavigateToForgotPassword Callback para navegar a la pantalla de Recuperación de Contraseña.
 * @param modifier Modificador opcional para configurar la apariencia exterior.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScreenScaffold(
        isOffline = state.isOffline,
        isReconnecting = state.isLoading,
        onRetryConnection = remember(onEvent) { { onEvent(LoginEvent.RetryConnectionClicked) } },
        headerContent = {
            LoginWelcomeHeader()
            Spacer(Modifier.height(MaterialTheme.spacing.large))
        },
        modifier = modifier
    ) {
        LoginFormFields(state = state, onEvent = onEvent)
        Spacer(Modifier.height(MaterialTheme.spacing.large))

        LoginOptionsRow(
            state = state,
            onEvent = onEvent,
            onNavigateToForgotPassword = onNavigateToForgotPassword
        )
        Spacer(Modifier.height(27.dp))

        LoginActionButtons(
            isLoading = state.isLoading,
            onLoginClicked = remember(onEvent) { { onEvent(LoginEvent.LoginClicked) } },
            onNavigateToRegister = onNavigateToRegister
        )
    }
}

/**
 * Textos de bienvenida y subtítulo de la pantalla.
 */
@Composable
private fun LoginWelcomeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.login_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(id = R.string.login_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Campos de entrada de datos para correo y contraseña con su correspondiente mensaje de error.
 */
@Composable
private fun LoginFormFields(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            value = state.email,
            onValueChange = remember(onEvent) { { email -> onEvent(LoginEvent.EmailChanged(email)) } },
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,
            isError = state.emailHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
        )

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        SmartFoodPasswordField(
            label = stringResource(id = R.string.login_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(LoginEvent.PasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(LoginEvent.LoginClicked)
                }
            ),
        )

        AuthErrorMessage(
            message = state.error.inlineError?.let { stringResource(id = it.messageRes()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fila de opciones de inicio de sesión (Recordar cuenta y ¿Olvidaste tu contraseña?).
 */
@Composable
private fun LoginOptionsRow(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartFoodCheckbox(
            label = stringResource(id = R.string.login_remember_me),
            checked = state.rememberMe,
            onCheckedChange = remember(onEvent) { { checked -> onEvent(LoginEvent.RememberMeChanged(checked)) } }
        )

        SmartFoodTextLink(
            text = stringResource(id = R.string.login_forgot_password),
            onClick = onNavigateToForgotPassword
        )
    }
}

/**
 * Botones de acción principal (Iniciar Sesión y Registro).
 */
@Composable
private fun LoginActionButtons(
    isLoading: Boolean,
    onLoginClicked: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.login_button_text),
            loadingText = stringResource(id = R.string.login_button_loading),
            onClick = onLoginClicked,
            isLoading = isLoading
        )

        Spacer(Modifier.height(26.dp))

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.login_no_account_prompt),
            action = stringResource(id = R.string.login_register_action),
            onClick = onNavigateToRegister,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    SmartfoodTheme {
        LoginScreen(
            state = LoginUiState(),
            onEvent = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}
