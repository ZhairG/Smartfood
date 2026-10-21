package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.components.SmartFoodWarningDialog
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import com.smartfood.app.feature.auth.ui.components.FlexibleGap
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto

/**
 * Nombre: CookingScreens.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantallas stateless de preparación previa ("Antes de empezar") y pasos de cocina guiada (spec 009, HU-RIA-04; Figma 557:2819 y 557:2781).
 */

private const val GlyphFraction = 0.55f

/** Pantalla "Antes de empezar" (Figma 557:2819). Foto de la receta, título y tarjeta de preparación inicial. */
@Composable
fun CookingIntroScreen(
    state: CookingUiState,
    onEvent: (CookingEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.recipes_cooking_intro_title),
            onBack = onBack
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                when {
                    state.isLoading -> Loading()
                    state.hasError -> Error()
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RecipePhoto(
                            photo = state.photo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(16.dp)
                                )
                        )

                        Text(
                            text = "${state.name}, paso a paso",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (state.beforeStarting.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.recipes_prepare_before),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    state.beforeStarting.forEach { item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = GreenCheck,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FlexibleGap(minHeight = 16.dp)

                        SmartFoodPrimaryButton(
                            text = stringResource(id = R.string.recipes_start_guide),
                            onClick = { onEvent(CookingEvent.StartGuideClicked) },
                            green = true
                        )
                    }
                }
            }
        }
    }
}

/** Pantalla "Cocina guiada" (Figma 557:2781). Avance de paso a paso, temporizador e imágenes. */
@Composable
fun CookingScreen(
    state: CookingUiState,
    onEvent: (CookingEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isFinishConfirmVisible) {
        SmartFoodWarningDialog(
            title = stringResource(id = R.string.recipes_confirm_finish_title),
            body = "",
            confirmText = stringResource(id = R.string.recipes_finish),
            dismissText = stringResource(id = R.string.edit_profile_cancel),
            onConfirm = { onEvent(CookingEvent.ConfirmFinishClicked) },
            onDismiss = { onEvent(CookingEvent.DismissFinishDialog) }
        )
    }

    if (state.isSaving) {
        SmartFoodLoadingDialog(text = stringResource(id = R.string.edit_profile_saving))
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
            title = stringResource(
                id = if (state.isSavedOnPhone) R.string.recipes_cooking_saved_phone else R.string.recipes_cooking_saved
            ),
            body = null
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.recipes_cooking_title),
            onBack = { onEvent(CookingEvent.ExitClicked) }
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                when {
                    state.isLoading -> Loading()
                    state.hasError || state.currentStep == null -> Error()
                    else -> {
                        val step = requireNotNull(state.currentStep)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { state.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Text(
                                text = stringResource(id = R.string.recipes_step_label, state.currentStepIndex + 1),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )

                            RecipePhoto(
                                photo = state.photo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(16.dp)
                                    )
                            )

                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Text(
                                text = step.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            if (step.minutes != null) {
                                val minutes = state.timerSecondsRemaining / 60
                                val seconds = state.timerSecondsRemaining % 60
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // Amarillo pálido Figma
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = Color(0xFFF57F17),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = stringResource(id = R.string.recipes_timer_label, minutes, seconds),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF57F17)
                                            )
                                        )
                                    }
                                }
                            }

                            FlexibleGap(minHeight = 16.dp)

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onEvent(CookingEvent.PreviousStepClicked) },
                                    enabled = !state.isFirstStep,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = stringResource(id = R.string.recipes_previous))
                                }

                                if (state.isLastStep) {
                                    SmartFoodPrimaryButton(
                                        text = stringResource(id = R.string.recipes_finish),
                                        onClick = { onEvent(CookingEvent.FinishClicked) },
                                        green = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    SmartFoodPrimaryButton(
                                        text = stringResource(id = R.string.recipes_next),
                                        onClick = { onEvent(CookingEvent.NextStepClicked) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onEvent(CookingEvent.ExitClicked) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(id = R.string.recipes_exit))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun Error() = Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
        text = stringResource(id = R.string.recipes_detail_error),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
