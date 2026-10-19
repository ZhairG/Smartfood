package com.smartfood.app.feature.profile.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodSaveChangesDialog
import com.smartfood.app.core.designsystem.components.SmartFoodSelectorField
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.clearFocusOnTap
import com.smartfood.app.feature.auth.ui.messageRes
import com.smartfood.app.feature.inventory.ui.components.PhotoSourceSheet
import com.smartfood.app.feature.profile.domain.model.UserProfile
import com.smartfood.app.feature.profile.ui.ProfileAvatar
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Nombre: EditProfileScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla sin estado de "Editar información" (spec 008, HU-PER-02; Figma `361:649`, `369:1195` y
 * `369:905`). Barra superior con la flecha y "Guardar", la foto o las iniciales y los cuatro campos. Si el correo
 * cambia, aparece "Contraseña actual" debajo (CA-PER-02-10). La barra inferior la pone `MainTabScaffold`.
 *
 * @param state Estado de la pantalla.
 * @param onEvent Recibe lo que hace el usuario.
 */
@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onEvent: (EditProfileEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.edit_profile_title),
            onBack = { onEvent(EditProfileEvent.BackClicked) },
            action = {
                // Figma 362:723: "Guardar" naranja en Bold de 20 sp; sin cambios se ve deshabilitado (CA-PER-02-02).
                TextButton(onClick = { onEvent(EditProfileEvent.SaveClicked) }, enabled = state.isSaveEnabled) {
                    Text(
                        text = stringResource(id = R.string.edit_profile_save),
                        color = MaterialTheme.colorScheme.secondary.copy(
                            alpha = if (state.isSaveEnabled) 1f else DisabledAlpha
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        )

        // Igual que el editor de productos: el perfil de medidas se toma sin el teclado y el teclado se aplica por
        // dentro, fuera del desplazable (spec 002).
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        EditProfileForm(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }

    EditProfileDialogs(state = state, onEvent = onEvent)
}

/** Foto y campos del formulario, dentro de un solo desplazable. */
@Composable
private fun EditProfileForm(state: EditProfileUiState, onEvent: (EditProfileEvent) -> Unit) {
    val metrics = LocalFormMetrics.current
    val spacing = MaterialTheme.spacing
    val errorField = state.errorField

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.fieldSpacing),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatar(
                photo = state.newPhotoPath?.let(::File) ?: state.photoUrl,
                initials = state.initials,
                size = spacing.iconDialogSize,
                initialsSize = InitialsSize,
                badgeBorder = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(id = R.string.cd_profile_change_photo),
                onClick = { onEvent(EditProfileEvent.PhotoClicked) }
            )
            // Figma 362:735: Poppins Medium de 12 sp en gris.
            Text(
                text = stringResource(id = R.string.edit_profile_change_photo),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(top = spacing.medium)
            )
        }

        SmartFoodTextField(
            label = stringResource(id = R.string.edit_profile_name),
            value = state.fullName,
            onValueChange = { onEvent(EditProfileEvent.FullNameChanged(it)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = errorField == EditProfileField.FULL_NAME
        )
        SmartFoodTextField(
            label = stringResource(id = R.string.edit_profile_email),
            value = state.email,
            onValueChange = { onEvent(EditProfileEvent.EmailChanged(it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            isError = errorField == EditProfileField.EMAIL
        )
        // El correo nuevo se confirma con la contraseña actual (CA-PER-02-10).
        if (state.isEmailChanged) {
            SmartFoodPasswordField(
                label = stringResource(id = R.string.edit_profile_current_password),
                value = state.currentPassword,
                onValueChange = { onEvent(EditProfileEvent.CurrentPasswordChanged(it)) },
                placeholder = stringResource(id = R.string.edit_profile_current_password_hint),
                isError = errorField == EditProfileField.CURRENT_PASSWORD
            )
        }
        SmartFoodTextField(
            label = stringResource(id = R.string.edit_profile_phone),
            value = state.phone,
            onValueChange = { onEvent(EditProfileEvent.PhoneChanged(it)) },
            placeholder = stringResource(id = R.string.edit_profile_phone_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            isError = errorField == EditProfileField.PHONE
        )
        SmartFoodSelectorField(
            label = stringResource(id = R.string.edit_profile_birth_date),
            value = state.birthDate?.format(DateFormatter).orEmpty(),
            onClick = { onEvent(EditProfileEvent.BirthDateClicked) },
            placeholder = stringResource(id = R.string.edit_profile_birth_date_placeholder),
            trailingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(metrics.fieldIconSize)
                )
            },
            isError = errorField == EditProfileField.BIRTH_DATE
        )

        state.error?.takeIf { state.dialog !is EditProfileDialog.EmailCode }?.let { error ->
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
    }
}

/** Hoja de foto, calendario y avisos que dependen de [EditProfileUiState.dialog]. */
@Composable
private fun EditProfileDialogs(state: EditProfileUiState, onEvent: (EditProfileEvent) -> Unit) {
    val dismiss = { onEvent(EditProfileEvent.DialogDismissed) }
    // Con el aviso del código abierto, la carga se ve en su botón "Confirmar".
    if (state.isSaving && state.dialog !is EditProfileDialog.EmailCode) {
        SmartFoodLoadingDialog(text = stringResource(id = R.string.edit_profile_saving))
    }
    when (val dialog = state.dialog) {
        EditProfileDialog.PhotoSourceSheet -> PhotoSourceSheet(
            onGallery = { onEvent(EditProfileEvent.GalleryChosen) },
            onCamera = { onEvent(EditProfileEvent.CameraChosen) },
            onDismiss = dismiss
        )
        EditProfileDialog.BirthDatePicker -> BirthDatePicker(
            selected = state.birthDate,
            onConfirm = { onEvent(EditProfileEvent.BirthDatePicked(it)) },
            onDismiss = dismiss
        )
        EditProfileDialog.ConfirmSave -> SmartFoodSaveChangesDialog(
            question = stringResource(id = R.string.edit_profile_confirm_save),
            confirmText = stringResource(id = R.string.edit_profile_save),
            dismissText = stringResource(id = R.string.edit_profile_cancel),
            onConfirm = { onEvent(EditProfileEvent.SaveConfirmed) },
            onDismiss = dismiss
        )
        is EditProfileDialog.EmailCode -> EmailCodeDialog(state = state, email = dialog.email, onEvent = onEvent)
        // Figma 369:905: círculo verde claro con el chulo verde y "Perfil se actualizó" en naranja.
        is EditProfileDialog.Saved -> SmartFoodResultDialog(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction)
                )
            },
            title = stringResource(id = R.string.edit_profile_saved),
            body = dialog.emailChangedTo?.let { stringResource(id = R.string.edit_profile_email_changed, it) }
                ?: stringResource(id = R.string.edit_profile_saved_body)
        )
        null -> Unit
    }
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Figma 362:728: iniciales en Bold de 35 sp dentro del círculo de 100 px.
private val InitialsSize = 35.sp
private const val GlyphFraction = 0.4f
private const val DisabledAlpha = 0.5f

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun EditProfileScreenPreview() {
    SmartfoodTheme {
        val profile = UserProfile(uid = "u", fullName = "Pepito Perez", email = "pepitoperez@gmail.com")
        EditProfileScreen(
            state = EditProfileUiState(
                isLoading = false,
                original = profile,
                fullName = profile.fullName,
                email = profile.email,
                birthDate = LocalDate.of(2000, 1, 31)
            ),
            onEvent = {}
        )
    }
}
