package com.smartfood.app.feature.profile.ui.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.clearFocusOnTap
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import com.smartfood.app.feature.auth.ui.components.FlexibleGap
import com.smartfood.app.feature.auth.ui.components.PasswordFieldWithChecklist
import com.smartfood.app.feature.auth.ui.messageRes

/**
 * Nombre: ChangePasswordScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla sin estado para cambiar la contraseña confirmando la actual (spec 008, HU-PER-05; Figma 603:2358 y 603:2460).
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
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.change_password_title),
            onBack = onBack
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    ChangePasswordForm(state = state, onEvent = onEvent)
                }
            }
        }
    }

    if (state.isLoading) {
        SmartFoodLoadingDialog(text = stringResource(id = R.string.change_password_button_loading))
    }

    if (state.isSavedSuccess) {
        SmartFoodResultDialog(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction)
                )
            },
            title = stringResource(id = R.string.change_password_saved),
            body = stringResource(id = R.string.change_password_saved_body)
        )
    }
}

@Composable
private fun ChangePasswordForm(
    state: ChangePasswordUiState,
    onEvent: (ChangePasswordEvent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val metrics = LocalFormMetrics.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
    ) {
        Text(
            text = stringResource(id = R.string.change_password_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = metrics.labelFontSize,
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
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(id = R.string.error_invalid_current_password),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
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

        val showPasswordsDontMatchError = (state.confirmPassword.isNotEmpty() && !state.passwordsMatch) ||
                state.generalError == AuthError.PasswordsDontMatch

        SmartFoodPasswordField(
            label = stringResource(id = R.string.change_password_confirm_label),
            value = state.confirmPassword,
            onValueChange = remember(onEvent) { { pass -> onEvent(ChangePasswordEvent.ConfirmPasswordChanged(pass)) } },
            enabled = !state.isLoading,
            isError = showPasswordsDontMatchError || state.generalError != null,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (state.canSubmit) onEvent(ChangePasswordEvent.SubmitClicked)
                }
            )
        )

        if (showPasswordsDontMatchError) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(id = R.string.error_passwords_dont_match),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
        } else if (state.generalError != null && state.generalError != AuthError.PasswordsDontMatch) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(id = state.generalError.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
        }

        FlexibleGap(minHeight = metrics.sectionSpacing)

        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.change_password_button),
            onClick = remember(onEvent) { { onEvent(ChangePasswordEvent.SubmitClicked) } },
            enabled = state.canSubmit,
            green = true
        )
    }
}

private const val GlyphFraction = 0.55f

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
