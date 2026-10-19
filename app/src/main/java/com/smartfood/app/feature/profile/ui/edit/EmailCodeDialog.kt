package com.smartfood.app.feature.profile.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodContentDialog
import com.smartfood.app.core.designsystem.components.SmartFoodOtpCodeInput
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodPromptCountdown
import com.smartfood.app.core.designsystem.components.SmartFoodPromptLink
import com.smartfood.app.core.designsystem.components.formatCountdown
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.ui.messageRes

/**
 * Nombre: EmailCodeDialog.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Aviso "Revisa tu correo" de "Editar información" (spec 008, T009b, CA-PER-02-10 y 02-11). Pide el
 * código de 4 dígitos enviado al correo nuevo con las mismas casillas y el mismo "Reenviar código" de la recuperación
 * de contraseña (spec 003). Desviación aprobada: el Figma no tiene este paso.
 *
 * @param state Estado de la pantalla: el código, la espera para reenviar, si confirma y el error.
 * @param email Correo nuevo al que se envió el código.
 * @param onEvent Recibe lo que hace el usuario.
 */
@Composable
fun EmailCodeDialog(state: EditProfileUiState, email: String, onEvent: (EditProfileEvent) -> Unit) {
    SmartFoodContentDialog(
        title = stringResource(id = R.string.edit_profile_code_title),
        onDismissRequest = { onEvent(EditProfileEvent.CancelEmailCode) }
    ) {
        Text(
            text = stringResource(id = R.string.edit_profile_code_body, email),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        SmartFoodOtpCodeInput(
            code = state.emailCode,
            onCodeChanged = { onEvent(EditProfileEvent.EmailCodeChanged(it)) },
            isError = state.errorField == EditProfileField.EMAIL_CODE,
            enabled = !state.isSaving,
            onDone = { onEvent(EditProfileEvent.ConfirmEmailCode) }
        )
        state.error?.let { error ->
            Text(
                text = stringResource(id = error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.edit_profile_code_confirm),
            onClick = { onEvent(EditProfileEvent.ConfirmEmailCode) },
            enabled = state.emailCode.length == EditProfileViewModel.EMAIL_CODE_LENGTH,
            isLoading = state.isSaving
        )
        if (state.resendCooldownSeconds > 0) {
            SmartFoodPromptCountdown(
                prompt = stringResource(id = R.string.recovery_step2_resend_countdown_prompt),
                countdown = formatCountdown(state.resendCooldownSeconds),
                contentDescription = pluralStringResource(
                    id = R.plurals.recovery_resend_countdown_a11y,
                    count = state.resendCooldownSeconds,
                    state.resendCooldownSeconds
                )
            )
        } else {
            SmartFoodPromptLink(
                prompt = stringResource(id = R.string.recovery_step2_resend_prompt),
                action = stringResource(id = R.string.recovery_step2_resend_action),
                onClick = { onEvent(EditProfileEvent.ResendEmailCode) }
            )
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onEvent(EditProfileEvent.CancelEmailCode) }, enabled = !state.isSaving) {
                Text(
                    text = stringResource(id = R.string.edit_profile_cancel),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailCodeDialogPreview() {
    SmartfoodTheme {
        EmailCodeDialog(
            state = EditProfileUiState(isLoading = false, emailCode = "48", resendCooldownSeconds = 42),
            email = "nuevo@correo.com",
            onEvent = {}
        )
    }
}
