package com.smartfood.app.ui.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
 * 
 * Pantalla principal de Inicio de Sesión de la Aplicación (SmartFood).
 * Construida respetando el diseño original de Figma (nodo 51:11, 412x917)
 * y la arquitectura Clean (UDF).
 *
 * Esta vista carece de lógica de negocio o referencias de inyección; funciona 
 * como un "Stateless Composable" alimentado directamente desde la capa de navegación.
 *
 * Estructura:
 *  - Logo de marca: fijo (se contrae mientras el teclado está abierto para dejar espacio).
 *  - Panel: esquinas redondeadas + "Bienvenido": fijos.
 *  - Formulario (campos, opciones y botones): es lo ÚNICO que hace scroll.
 *
 * @param state Objeto de estado inmutable [AuthUiState] proveniente del ViewModel.
 * @param onEvent Callback centralizado para emitir [AuthEvent] hacia el ViewModel.
 * @param onNavigateToRegister Acción para delegar la navegación a la pantalla de Registro.
 * @param onNavigateToRecovery Acción para delegar la navegación hacia la Recuperación.
 * @param modifier Modificador opcional para inyectar configuraciones externas a la UI.
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

    // true mientras el teclado ocupa espacio. derivedStateOf evita recomponer en cada frame
    // de la animación del teclado: solo se recompone cuando cambia el booleano.
    val isImeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    // Quita el foco de los campos cuando el teclado se oculta (por ejemplo, con la flecha
    // hacia abajo de la barra de navegación). Se observa la transición "visible -> oculto"
    // a partir del inset real del teclado y se fuerza la limpieza del foco.
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
            onReconnect = { onEvent(AuthEvent.RetryConnectionClicked) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            // 1) Respeta solo la status bar. La navigation bar NO se aplica aquí: se aplica dentro
            //    del panel ([LoginContentCard]) para que su fondo tenga el color del panel.
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        // 77 dp arriba y 53 dp abajo => el panel empieza en y = 296 dp, como en Figma.
        // Con el teclado abierto se contrae para que el formulario tenga espacio suficiente y
        // los campos no queden tapados. (Para mantener siempre el logo, basta con quitar este wrapper.)
        /*AnimatedVisibility(
            visible = !isImeVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SmartFoodBrandHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 77.dp, bottom = 53.dp),
                smartColor = MaterialTheme.colorScheme.onBackground
            )
        }*/

        SmartFoodBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 77.dp, bottom = 53.dp),
            smartColor = MaterialTheme.colorScheme.onBackground
        )

        // Contenedor blanco/oscuro inferior con los campos
        LoginContentCard(
            state = state,
            onEvent = onEvent,
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToRecovery = onNavigateToRecovery
        )
    }
}

/**
 * Contenedor principal del formulario de inicio de sesión.
 * Utiliza un diseño adaptativo (`widthIn(max = 480.dp)`).
 *
 * Tiene dos zonas:
 *  1. **Fija**: título y subtítulo de bienvenida ([LoginWelcomeHeader]).
 *  2. **Scrollable** (ocupa el resto del alto): campos, opciones y botones. Es la única zona
 *     que se desplaza, y su viewport se reduce por encima del teclado gracias al `imePadding()`
 *     de [LoginScreen].
 *
 * El fondo del panel se extiende hasta el borde inferior de la pantalla y el inset de la
 * navigation bar se aplica *dentro* del panel, de modo que la barra de navegación queda con el
 * mismo color del panel. Cuando el teclado está abierto, `imePadding()` ya consumió ese inset
 * y no se agrega espacio extra.
 *
 * @param state Estado actual de la UI de autenticación.
 * @param onEvent Despachador de eventos al ViewModel.
 * @param onNavigateToRegister Acción para ir a la vista de registro.
 * @param onNavigateToRecovery Acción para ir a la recuperación de contraseña.
 * @param modifier Modificador base.
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
    val isImeVisible = WindowInsets.isImeVisible

    // Apenas el teclado deja de ser visible, resetea de forma inmediata y automática el scroll
    // a la posición inicial (0). Usar scrollTo(0) garantiza una respuesta instantánea (1 frame)
    // que previene que el usuario rompa el desplazamiento con gestos.
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
            // El inset va DESPUÉS del fondo: el color del panel llega hasta abajo del todo,
            // pero el contenido queda por encima de la barra de navegación.
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(top = 47.dp), // Margen superior del panel según Figma
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------- ZONA FIJA ----------
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 34.dp),
        ) {
            LoginWelcomeHeader()
            Spacer(Modifier.height(MaterialTheme.spacing.large))
        }

        // ---------- ZONA CON SCROLL (solo el formulario) ----------
        // imePadding() limita el alto del viewport para que termine justo encima del teclado.
        // Al estar combinado con verticalScroll, Compose desplaza automáticamente el campo enfocado
        // para que permanezca visible sin mover la cabecera fija.
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 34.dp)
                .padding(bottom = 48.dp) // Margen inferior para que el contenido "respire" al final del scroll
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
                onLoginClicked = { onEvent(AuthEvent.LoginClicked) },
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
 * Traduce el estado de error del login al texto que ve el usuario (según Figma).
 *
 * Los errores conocidos ([AuthErrorType.CREDENTIALS_ERROR], [AuthErrorType.EMPTY_FIELDS],
 * [AuthErrorType.INVALID_EMAIL]) usan textos localizados de `strings.xml`; el resto recurre al
 * mensaje que haya reportado el ViewModel. Los errores de red los maneja el diálogo offline.
 */
@Composable
private fun AuthUiState.loginErrorText(): String? = when (errorType) {
    AuthErrorType.CREDENTIALS_ERROR -> stringResource(id = R.string.error_invalid_credentials)
    AuthErrorType.EMPTY_FIELDS -> stringResource(id = R.string.error_empty_fields)
    AuthErrorType.INVALID_EMAIL -> stringResource(id = R.string.error_invalid_email)
    else -> errorMessage
}

/**
 * Campos de entrada de datos (Correo y Contraseña) y el mensaje de error debajo de ellos.
 *
 * Estados de los campos:
 *  - **Normal / enfocado**: borde gris / verde.
 *  - **Error** ([AuthUiState.emailHasError] / [AuthUiState.passwordHasError]): borde rojo.
 *  - **Deshabilitado** mientras hay una petición en curso ([AuthUiState.isLoading]).
 *
 * Flujo del teclado: "Siguiente" en el correo pasa al campo de contraseña; "Listo" en la
 * contraseña cierra el teclado, quita el foco y envía el formulario.
 *
 * @param state Estado actual (contiene los valores de texto y los errores).
 * @param onEvent Función para despachar los cambios de texto.
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
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
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
            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            enabled = !state.isLoading,
            isError = state.passwordHasError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Cierra el teclado y quita el foco antes de enviar
                    onEvent(AuthEvent.LoginClicked)
                }
            ),
        )

        // Mensaje de error debajo de la contraseña, centrado (como en Figma)
        AuthErrorMessage(
            message = state.loginErrorText(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fila de opciones secundarias (Recordar cuenta y Olvidé contraseña).
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
            onCheckedChange = { onEvent(AuthEvent.RememberMeChanged(it)) }
        )
        SmartFoodTextLink(
            text = stringResource(id = R.string.login_forgot_password),
            onClick = onNavigateToRecovery
        )
    }
}

/**
 * Botones de acción principal (Iniciar Sesión y Regístrate).
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

// ============================================================================
// VISTAS PREVIAS (@Preview) DE SUBCOMPONENTES
// ============================================================================
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenErrorPreview() {
    SmartfoodTheme {
        LoginScreen(
            state = AuthUiState(
                email = "pepitoperez@gmail.com",
                password = "1234567890",
                errorType = AuthErrorType.CREDENTIALS_ERROR,
                emailHasError = true,
                passwordHasError = true
            ),
            onEvent = {},
            onNavigateToRegister = {},
            onNavigateToRecovery = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentCardPreview() {
    SmartfoodTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            LoginContentCard(
                state = AuthUiState(
                    errorType = AuthErrorType.EMPTY_FIELDS,
                    emailHasError = true,
                    passwordHasError = true
                ),
                onEvent = {},
                onNavigateToRegister = {},
                onNavigateToRecovery = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginWelcomeHeaderPreview() {
    SmartfoodTheme {
        LoginWelcomeHeader(modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginFormFieldsPreview() {
    SmartfoodTheme {
        LoginFormFields(
            state = AuthUiState(email = "test@correo.com", password = "123"),
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginFormFieldsErrorPreview() {
    SmartfoodTheme {
        LoginFormFields(
            state = AuthUiState(
                email = "pepitoperez@gmail.com",
                password = "123",
                errorType = AuthErrorType.CREDENTIALS_ERROR,
                emailHasError = true,
                passwordHasError = true
            ),
            onEvent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginOptionsRowPreview() {
    SmartfoodTheme {
        LoginOptionsRow(
            state = AuthUiState(rememberMe = true),
            onEvent = {},
            onNavigateToRecovery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginActionButtonsPreview() {
    SmartfoodTheme {
        LoginActionButtons(
            isLoading = false,
            onLoginClicked = {},
            onNavigateToRegister = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}