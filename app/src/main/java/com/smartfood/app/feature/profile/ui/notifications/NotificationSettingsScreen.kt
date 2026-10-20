package com.smartfood.app.feature.profile.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.components.SmartFoodWarningDialog
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.White
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * Nombre: NotificationSettingsScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla sin estado de "Notificaciones" (spec 008, HU-PER-04; Figma `593:2079`). Barra verde con la
 * flecha y una tarjeta con tres interruptores, encendidos la primera vez (CA-PER-04-01), que se guardan al tocarlos
 * sin botón "Guardar" (CA-PER-04-02). "Recetas sugeridas" solo se guarda (CA-PER-04-05). Si el permiso de Android 13+
 * quedó negado, el aviso deja abrir los ajustes (CA-PER-04-06). La barra inferior la pone `MainTabScaffold`.
 *
 * @param state Estado de la pantalla.
 * @param onEvent Recibe lo que hace el usuario.
 */
@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onEvent: (NotificationSettingsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.notifications_title),
            onBack = { onEvent(NotificationSettingsEvent.BackClicked) }
        )
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            NotificationSettingsContent(state = state, onEvent = onEvent)
        }
    }

    if (state.dialog is NotificationSettingsDialog.PermissionDenied) {
        SmartFoodWarningDialog(
            title = stringResource(id = R.string.notification_permission_title),
            body = stringResource(id = R.string.notification_permission_body),
            confirmText = stringResource(id = R.string.notification_permission_open_settings),
            dismissText = stringResource(id = R.string.edit_profile_cancel),
            onConfirm = { onEvent(NotificationSettingsEvent.OpenSettingsClicked) },
            onDismiss = { onEvent(NotificationSettingsEvent.DialogDismissed) }
        )
    }
}

/** Tarjeta de la Figma `593:2079`: una fila por aviso, separadas por líneas. */
@Composable
private fun NotificationSettingsContent(
    state: NotificationSettingsUiState,
    onEvent: (NotificationSettingsEvent) -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium, vertical = spacing.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(spacing.medium))
        ) {
            val settings = listOf(
                NotificationSetting.EXPIRING_PRODUCTS to
                    (R.string.notification_option_expiring to R.string.notification_option_expiring_description),
                NotificationSetting.SUGGESTED_RECIPES to
                    (R.string.notification_option_suggested_recipes to R.string.notification_option_suggested_recipes_description),
                NotificationSetting.EMPTY_INVENTORY to
                    (R.string.notification_option_empty_inventory to R.string.notification_option_empty_inventory_description)
            )
            settings.forEachIndexed { index, (setting, texts) ->
                NotificationSettingRow(
                    title = stringResource(id = texts.first),
                    description = stringResource(id = texts.second),
                    checked = state.isOn(setting),
                    onCheckedChange = { enabled ->
                        onEvent(NotificationSettingsEvent.ToggleRequested(setting, enabled))
                    }
                )
                if (index < settings.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** Título y descripción a la izquierda, interruptor verde a la derecha (Figma `593:2079`). */
@Composable
private fun NotificationSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.medium)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = notificationSwitchColors(),
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

/** Interruptor verde de la Figma `593:2079`, con el pulgar blanco. */
@Composable
private fun notificationSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = White,
    checkedTrackColor = GreenCheck,
    checkedBorderColor = GreenCheck
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun NotificationSettingsScreenPreview() {
    SmartfoodTheme {
        NotificationSettingsScreen(state = NotificationSettingsUiState(isLoading = false), onEvent = {})
    }
}
