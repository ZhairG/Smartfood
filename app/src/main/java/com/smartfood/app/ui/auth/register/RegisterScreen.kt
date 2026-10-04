package com.smartfood.app.ui.auth.register

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.smartfood.app.R
import com.smartfood.app.ui.auth.AuthErrorType
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.auth.components.AuthErrorMessage
import com.smartfood.app.ui.auth.components.AuthScreenScaffold
import com.smartfood.app.ui.auth.components.PasswordRequirementChecklist
import com.smartfood.app.ui.components.SmartFoodPasswordField
import com.smartfood.app.ui.components.SmartFoodPrimaryButton
import com.smartfood.app.ui.components.SmartFoodPromptLink
import com.smartfood.app.ui.components.SmartFoodTermsCheckbox
import com.smartfood.app.ui.components.SmartFoodTextField
import com.smartfood.app.ui.theme.SmartfoodTheme
import com.smartfood.app.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * Nombre: RegisterScreen.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pantalla de Registro ("Crea tu cuenta") de la aplicación Smartfood.
 * Reutiliza [AuthScreenScaffold] para el layout base, insets y gestión de foco/teclado.
 *
 * @param state Objeto de estado inmutable [AuthUiState] proveniente del ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [AuthEvent] hacia el ViewModel.
 * @param onNavigateToLogin Callback para navegar de regreso a la pantalla de Inicio de Sesión.
 * @param onNavigateToSuccess Callback notificado al completarse el registro exitosamente.
 * @param modifier Modificador opcional para la apariencia exterior.
 */
@Composable
fun RegisterScreen(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateToSuccess()
        }
    }

    AuthScreenScaffold(
        state = state,
        onEvent = onEvent,
        headerContent = {
            RegisterWelcomeHeader()
            Spacer(Modifier.height(MaterialTheme.spacing.large))
        },
        modifier = modifier
    ) { scrollState ->
        RegisterFormFields(state = state, scrollState = scrollState, onEvent = onEvent)
        Spacer(Modifier.height(20.dp))

        SmartFoodTermsCheckbox(
            checked = state.acceptedTerms,
            onCheckedChange = remember(onEvent) {
                { checked ->
                    onEvent(
                        AuthEvent.AcceptedTermsChanged(
                            checked
                        )
                    )
                }
            }
        )
        Spacer(Modifier.height(24.dp))

        RegisterActionButtons(
            isLoading = state.isLoading,
            enabled = state.acceptedTerms,
            onRegisterClicked = remember(onEvent) { { onEvent(AuthEvent.RegisterClicked) } },
            onNavigateToLogin = onNavigateToLogin
        )
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
            style = MaterialTheme.typography.headlineLarge,
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
 * Convierte el tipo de error en el texto correspondiente provisto por los recursos de cadenas.
 */
@Composable
private fun AuthUiState.registerErrorText(): String? = when (errorType) {
    AuthErrorType.ACCOUNT_ALREADY_EXISTS -> stringResource(id = R.string.error_account_already_exists)
    AuthErrorType.EMPTY_FIELDS -> stringResource(id = R.string.error_empty_fields)
    AuthErrorType.INVALID_EMAIL -> stringResource(id = R.string.error_invalid_email)
    else -> errorMessage
}

/**
 * Campos de entrada de datos para Nombre Completo, Correo y Contraseña en el Registro.
 */
@Composable
private fun RegisterFormFields(
    state: AuthUiState,
    scrollState: ScrollState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isPasswordFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        SmartFoodTextField(
            label = stringResource(id = R.string.register_name_label),
            value = state.fullName,
            onValueChange = remember(onEvent) { { name -> onEvent(AuthEvent.FullNameChanged(name)) } },
            placeholder = stringResource(id = R.string.register_name_placeholder),
            enabled = !state.isLoading,
            isError = state.fullNameHasError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            value = state.email,
            onValueChange = remember(onEvent) { { email -> onEvent(AuthEvent.EmailChanged(email)) } },
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

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        Box(modifier = Modifier.fillMaxWidth()) {
            SmartFoodPasswordField(
                label = stringResource(id = R.string.login_password_label),
                value = state.password,
                onValueChange = remember(onEvent) { { pass -> onEvent(AuthEvent.PasswordChanged(pass)) } },
                enabled = !state.isLoading,
                isError = state.passwordHasError,
                imeAction = ImeAction.Done,
                onFocusChanged = { focused -> isPasswordFocused = focused },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        coroutineScope.launch { scrollState.scrollTo(0) }
                        onEvent(AuthEvent.RegisterClicked)
                    }
                )
            )

            if (isPasswordFocused) {
                val yOffsetPx = with(density) { (-100).dp.roundToPx() }

                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, yOffsetPx),
                    properties = PopupProperties(focusable = false)
                ) {
                    PasswordRequirementChecklist(
                        hasMinimumLength = state.hasMinimumLength,
                        hasUppercase = state.hasUppercase,
                        hasSpecialCharacter = state.hasSpecialCharacter,
                        modifier = Modifier.width(342.dp)
                    )
                }
            }
        }

        AuthErrorMessage(
            message = state.registerErrorText(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Botones de acción principal (Crear Cuenta e Iniciar Sesión).
 */
@Composable
private fun RegisterActionButtons(
    isLoading: Boolean,
    enabled: Boolean,
    onRegisterClicked: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.register_button_text),
            loadingText = stringResource(id = R.string.register_button_loading),
            onClick = onRegisterClicked,
            isLoading = isLoading,
            enabled = enabled
        )

        Spacer(Modifier.height(26.dp))

        SmartFoodPromptLink(
            prompt = stringResource(id = R.string.register_already_have_account_prompt),
            action = stringResource(id = R.string.register_login_action),
            onClick = onNavigateToLogin
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    SmartfoodTheme {
        RegisterScreen(
            state = AuthUiState(),
            onEvent = {},
            onNavigateToLogin = {},
            onNavigateToSuccess = {}
        )
    }
}
