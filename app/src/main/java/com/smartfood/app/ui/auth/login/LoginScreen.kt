package com.smartfood.app.ui.auth.login

import android.icu.util.MeasureUnit.PIXEL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.components.SmartFoodBrandHeader
import com.smartfood.app.ui.components.SmartFoodCheckbox
import com.smartfood.app.ui.components.SmartFoodPasswordField
import com.smartfood.app.ui.components.SmartFoodPrimaryButton
import com.smartfood.app.ui.components.SmartFoodPromptLink
import com.smartfood.app.ui.components.SmartFoodTextField
import com.smartfood.app.ui.components.SmartFoodTextLink
import com.smartfood.app.ui.theme.SmartfoodTheme

/**
 * Figma: "Inicio de sesión" (nodo 51:11), 412x917.
 * Respeta al 100% las medidas y tipografías originales pero adaptado a la Clean Architecture (M3).
 */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rememberMe by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fondo principal verde
    ) {
        // 77 dp arriba y 53 dp abajo => el panel empieza en y = 296 dp, como en Figma.
        SmartFoodBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars) // Evita choque con hora/batería SOLO en el logo
                .padding(top = 77.dp, bottom = 53.dp),
            smartColor = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .weight(1f) // Se expande
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 45.dp, topEnd = 45.dp),
                )
                .imePadding() // Empuja el contenedor hacia arriba para no ocultarse tras el teclado
                // El verticalScroll SÍ O SÍ debe ir ANTES del padding inferior/superior para que el contenido fluya libremente al desplazar.
                .verticalScroll(rememberScrollState())
                .padding(top = 47.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor interno que limita el ancho máximo en pantallas grandes (Tablets/Foldables)
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp) // Ancho máximo recomendado
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp), // El padding horizontal se aplica solo al contenido, no al scroll
            ) {
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

            if (!state.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(21.dp))
            SmartFoodTextField(
                label = stringResource(id = R.string.login_email_label),
                value = state.email,
                onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                placeholder = stringResource(id = R.string.login_email_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(11.dp))
            SmartFoodPasswordField(
                label = androidx.compose.ui.res.stringResource(id = com.smartfood.app.R.string.login_password_label),
                value = state.password,
                onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onEvent(AuthEvent.LoginClicked) }),
            )

            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmartFoodCheckbox(
                    label = androidx.compose.ui.res.stringResource(id = com.smartfood.app.R.string.login_remember_me), 
                    checked = rememberMe, 
                    onCheckedChange = { rememberMe = it }
                )
                SmartFoodTextLink(
                    text = stringResource(id = R.string.login_forgot_password),
                    onClick = onNavigateToRecovery
                )
            }

            Spacer(Modifier.height(27.dp))
            SmartFoodPrimaryButton(
                text = if (state.isLoading) androidx.compose.ui.res.stringResource(id = com.smartfood.app.R.string.login_button_loading) else stringResource(id = R.string.login_button_text),
                onClick = { onEvent(AuthEvent.LoginClicked) },
                enabled = !state.isLoading
            )

            Spacer(Modifier.height(26.dp))
            SmartFoodPromptLink(
                prompt = stringResource(id = R.string.login_no_account_prompt),
                action = stringResource(id = R.string.login_register_action),
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .windowInsetsPadding(WindowInsets.navigationBars), // Protección contra gestos
            )
            } // Fin de Columna Limitadora (widthIn)
        } // Fin de la Columna Tarjeta Curva (Surface)
    } // Fin de la Columna Raíz
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    SmartfoodTheme() {
        LoginScreen(
            state = AuthUiState(),
            onEvent = {},
            onNavigateToRegister = {},
            onNavigateToRecovery = {}
        )
    }
}
