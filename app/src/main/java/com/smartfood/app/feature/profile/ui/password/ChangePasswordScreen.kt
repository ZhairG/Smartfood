package com.smartfood.app.feature.profile.ui.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import com.smartfood.app.feature.auth.ui.components.FlexibleGap
import com.smartfood.app.feature.auth.ui.components.PasswordFieldWithChecklist
import com.smartfood.app.feature.auth.ui.messageRes

/**
 * Nombre: ChangePasswordScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla para cambiar la contraseña confirmando la actual (spec 008, HU-PER-05; Figma 603:2358 y 603:2460).
 */

@Composable
fun ChangePasswordRoute(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            ChangePasswordEffect.NavigateBack -> onBack()
        }
    }

    ChangePasswordScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState,
    onEvent: (ChangePasswordEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemBarEffect(isLightBackground = true)

    val focusManager = LocalFocusManager.current
    val metrics = LocalFormMetrics.current

    if (state.isSavedSuccess) {
        SmartFoodResultDialog(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize)
                )
            },
            title = stringResource(id = R.string.change_password_saved),
            body = stringResource(id = R.string.change_password_saved_body)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.formHorizontalPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.offline_dialog_button),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.change_password_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = metrics.titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Text(
                    text = stringResource(id = R.string.change_password_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(metrics.sectionSpacing))

                SmartFoodPasswordField(
                    label = stringResource(id = R.string.change_password_current_label),
                    value = state.currentPassword,
                    onValueChange = remember(onEvent) { { pass -> onEvent(ChangePasswordEvent.CurrentPasswordChanged(pass)) } },
                    enabled = !state.isLoading,
                    isError = state.currentPasswordError != null,
                    imeAction = ImeAction.Next
                )

                if (state.currentPasswordError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.error_invalid_current_password),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(metrics.fieldSpacing))

                PasswordFieldWithChecklist(
                    label = stringResource(id = R.string.change_password_new_label),
                    value = state.newPassword,
                    onValueChange = remember(onEvent) { { pass -> onEvent(ChangePasswordEvent.NewPasswordChanged(pass)) } },
                    validation = state.passwordValidation,
                    passwordsMatch = state.passwordsMatch,
                    enabled = !state.isLoading,
                    isError = state.generalError != null,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(metrics.fieldSpacing))

                SmartFoodPasswordField(
                    label = stringResource(id = R.string.change_password_confirm_label),
                    value = state.confirmPassword,
                    onValueChange = remember(onEvent) { { pass -> onEvent(ChangePasswordEvent.ConfirmPasswordChanged(pass)) } },
                    enabled = !state.isLoading,
                    isError = state.generalError != null,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (state.canSubmit) onEvent(ChangePasswordEvent.SubmitClicked)
                        }
                    )
                )

                state.generalError?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = error.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                FlexibleGap(minHeight = metrics.sectionSpacing)

                SmartFoodPrimaryButton(
                    text = stringResource(id = R.string.change_password_button),
                    loadingText = stringResource(id = R.string.change_password_button_loading),
                    onClick = remember(onEvent) { { onEvent(ChangePasswordEvent.SubmitClicked) } },
                    enabled = state.canSubmit,
                    isLoading = state.isLoading
                )

                Spacer(modifier = Modifier.height(metrics.sectionSpacing))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordScreenPreview() {
    SmartfoodTheme {
        ChangePasswordScreen(
            state = ChangePasswordUiState(
                currentPassword = "123",
                newPassword = "Pass123!",
                confirmPassword = "Pass123!",
                passwordValidation = PasswordValidationResult(
                    hasMinimumLength = true,
                    hasUppercase = true,
                    hasSpecialCharacter = true
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
