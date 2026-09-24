package com.smartfood.app.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.ui.auth.AuthErrorType
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.auth.components.AuthErrorMessage
import com.smartfood.app.ui.components.SmartFoodBrandHeader
import com.smartfood.app.ui.components.SmartFoodCheckbox
import com.smartfood.app.ui.components.SmartFoodOfflineDialog
import com.smartfood.app.ui.components.SmartFoodPasswordField
import com.smartfood.app.ui.components.SmartFoodPrimaryButton
import com.smartfood.app.ui.components.SmartFoodPromptLink
import com.smartfood.app.ui.components.SmartFoodTextField
import com.smartfood.app.ui.components.SmartFoodTextLink
import com.smartfood.app.ui.theme.SmartfoodTheme
import com.smartfood.app.ui.theme.spacing

/**
 * Nombre: LoginScreen.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pantalla principal de Inicio de Sesión de la aplicación Smartfood.
 * Optimizado con [derivedStateOf] y lambdas estables para minimizar recomposiciones innecesarias.
 *
 * @param state Objeto de estado inmutable [AuthUiState] proveniente del ViewModel.
 * @param onEvent Callback centralizado para despachar eventos [AuthEvent] hacia el ViewModel.
 * @param onNavigateToRegister Callback para navegar a la pantalla de Registro.
 * @param onNavigateToRecovery Callback para navegar a la pantalla de Recuperación de Contraseña.
 * @param modifier Modificador opcional para configurar la apariencia exterior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime

    val isImeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    LaunchedEffect(imeInsets, density) {
        var wasImeVisible = false
        snapshotFlow { imeInsets.getBottom(density) > 0 }
            .collect { visible ->
                if (wasImeVisible && !visible) {
                    focusManager.clearFocus(force = true)
                }
                wasImeVisible = visible
            }
    }

    if (state.isOffline) {
        SmartFoodOfflineDialog(
            isReconnecting = state.isLoading,
            onReconnect = remember(onEvent) { { onEvent(AuthEvent.RetryConnectionClicked) } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        SmartFoodBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 77.dp, bottom = 53.dp),
            smartColor = MaterialTheme.colorScheme.onBackground
        )

        LoginContentCard(
            state = state,
            onEvent = onEvent,
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToRecovery = onNavigateToRecovery
        )
    }
}

/**
 * Contenedor del panel de formulario de inicio de sesión.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.LoginContentCard(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isImeVisibleRaw = WindowInsets.isImeVisible
    val isImeVisible by remember(isImeVisibleRaw) { derivedStateOf { isImeVisibleRaw } }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            scrollState.scrollTo(0)
        }
    }

    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 45.dp, topEnd = 45.dp),
            )
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(top = 47.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 34.dp),
        ) {
            LoginWelcomeHeader()
            Spacer(Modifier.height(MaterialTheme.spacing.large))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 34.dp)
                .padding(bottom = 48.dp)
        ) {
            LoginFormFields(state = state, onEvent = onEvent)
            Spacer(Modifier.height(MaterialTheme.spacing.large))

            LoginOptionsRow(
                state = state,
                onEvent = onEvent,
                onNavigateToRecovery = onNavigateToRecovery
            )
            Spacer(Modifier.height(27.dp))

            LoginActionButtons(
                isLoading = state.isLoading,
                onLoginClicked = remember(onEvent) { { onEvent(AuthEvent.LoginClicked) } },
                onNavigateToRegister = onNavigateToRegister
            )
        }
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
 * Convierte el tipo de error en el texto correspondiente provisto por los recursos de cadenas.
 */
@Composable
private fun AuthUiState.loginErrorText(): String? = when (errorType) {
    AuthErrorType.CREDENTIALS_ERROR -> stringResource(id = R.string.error_invalid_credentials)
    AuthErrorType.EMPTY_FIELDS -> stringResource(id = R.string.error_empty_fields)
    AuthErrorType.INVALID_EMAIL -> stringResource(id = R.string.error_invalid_email)
    else -> errorMessage
}

/**
 * Campos de entrada de datos para correo y contraseña con su correspondiente mensaje de error.
 */
@Composable
private fun LoginFormFields(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
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
            ),
        )

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        SmartFoodPasswordField(
            label = stringResource(id = R.string.login_password_label),
            value = state.password,
            onValueChange = remember(onEvent) { { pass -> onEvent(AuthEvent.PasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onEvent(AuthEvent.LoginClicked)
                }
            ),
        )

        AuthErrorMessage(
            message = state.loginErrorText(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fila de opciones de inicio de sesión (Recordar cuenta y Olvidé contraseña).
 */
@Composable
private fun LoginOptionsRow(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToRecovery: () -> Unit,
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
            onCheckedChange = remember(onEvent) { { checked -> onEvent(AuthEvent.RememberMeChanged(checked)) } }
        )
        SmartFoodTextLink(
            text = stringResource(id = R.string.login_forgot_password),
            onClick = onNavigateToRecovery
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
            state = AuthUiState(),
            onEvent = {},
            onNavigateToRegister = {},
            onNavigateToRecovery = {}
        )
    }
}
