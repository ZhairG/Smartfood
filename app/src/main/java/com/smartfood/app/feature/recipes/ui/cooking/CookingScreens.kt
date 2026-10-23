package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodTextLink
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.components.SmartFoodWarningDialog
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreyLight
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.YellowSoft
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.figmaShadow
import com.smartfood.app.feature.inventory.ui.messageRes
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto

/**
 * Nombre: CookingScreens.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantallas stateless de "Antes de empezar" y de la cocina guiada (spec 009, HU-RIA-04, CA-RIA-04-03 a
 * 04-05), fieles al Figma `557:2819` y `557:2781` con las desviaciones aprobadas de la spec §9: sin video ni "Omitir
 * video", la foto de la receta en cada paso, sin "¿Necesitas ayuda? Ver técnica…" y la tarjeta del temporizador que se
 * toca para iniciarlo o pausarlo, con "Reiniciar".
 */

/**
 * "Antes de empezar" (Figma `557:2819`): foto de la receta, "<Receta>, paso a paso", la tarjeta "Prepara antes de
 * iniciar" y "Comenzar guía" (CA-RIA-04-03).
 *
 * @param state Estado de la cocina.
 * @param onEvent Acciones de la persona.
 * @param onBack Flecha atrás: vuelve al detalle.
 * @param modifier Modificador opcional.
 */
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
        SmartFoodTopBar(title = stringResource(id = R.string.recipes_cooking_intro_title), onBack = onBack)
        when {
            state.isLoading -> Loading(Modifier.weight(1f))
            state.hasError -> Error(Modifier.weight(1f))
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(ContentGap),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(ContentPadding)
            ) {
                RecipePhoto(
                    photo = state.photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(IntroPhotoRatio)
                        .clip(RoundedCornerShape(CardRadius))
                )
                Text(
                    text = stringResource(id = R.string.recipes_cooking_intro_heading, state.name),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TitleStyle(),
                    modifier = Modifier.semantics { heading() }
                )
                if (state.beforeStarting.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardShadow()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(CardPadding)
                    ) {
                        Text(
                            text = stringResource(id = R.string.recipes_prepare_before),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = CardTitleStyle()
                        )
                        // Una sola línea con "✓" entre cada preparativo, como el Figma (557:2834).
                        Text(
                            text = state.beforeStarting.joinToString(separator = " ") { "✓ $it" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = BodyStyle()
                        )
                    }
                }
                RecipeActionButton(
                    text = stringResource(id = R.string.recipes_start_guide),
                    onClick = { onEvent(CookingEvent.StartGuideClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Cocina guiada (Figma `557:2781`): "Cocinando · paso N de T" con "Salir", la barra de avance, "PASO N", la foto, el
 * título y el texto del paso, el temporizador si el paso tiene tiempo y "Anterior" / "Siguiente" ("Terminar" en el
 * último paso, CA-RIA-04-04 y 04-05).
 *
 * @param state Estado de la cocina.
 * @param onEvent Acciones de la persona.
 * @param onBack Flecha atrás: vuelve a "Antes de empezar" (Figma `633:2973`).
 * @param modifier Modificador opcional.
 */
@Composable
fun CookingScreen(
    state: CookingUiState,
    onEvent: (CookingEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CookingDialogs(dialog = state.dialog, onEvent = onEvent)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.recipes_cooking_progress, state.currentStepIndex + 1, state.steps.size),
            onBack = onBack,
            action = {
                Text(
                    text = stringResource(id = R.string.recipes_exit),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = CardTitleStyle(),
                    modifier = Modifier.clickable(role = Role.Button) { onEvent(CookingEvent.ExitClicked) }
                )
            }
        )
        val step = state.currentStep
        when {
            state.isLoading -> Loading(Modifier.weight(1f))
            state.hasError || step == null -> Error(Modifier.weight(1f))
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(ContentGap),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(ContentPadding)
            ) {
                StepProgress(fraction = state.progressFraction)
                Text(
                    text = stringResource(id = R.string.recipes_step_label, state.currentStepIndex + 1),
                    color = GreenCheck,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                RecipePhoto(
                    photo = state.photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(StepPhotoRatio)
                        .clip(RoundedCornerShape(CardRadius))
                )
                Text(
                    text = step.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TitleStyle(),
                    modifier = Modifier.semantics { heading() }
                )
                Text(text = step.text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = BodyStyle())
                if (state.hasTimer) {
                    TimerCard(
                        secondsRemaining = state.timerSecondsRemaining,
                        running = state.isTimerRunning,
                        ringing = state.isAlarmRinging,
                        onToggle = { onEvent(CookingEvent.TimerToggled) },
                        onReset = { onEvent(CookingEvent.ResetTimerClicked) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ActionsGap), modifier = Modifier.fillMaxWidth()) {
                    OutlineActionButton(
                        text = stringResource(id = R.string.recipes_previous),
                        enabled = !state.isFirstStep,
                        onClick = { onEvent(CookingEvent.PreviousStepClicked) },
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isLastStep) {
                        RecipeActionButton(
                            text = stringResource(id = R.string.recipes_finish),
                            onClick = { onEvent(CookingEvent.FinishClicked) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        RecipeActionButton(
                            text = stringResource(id = R.string.recipes_next),
                            onClick = { onEvent(CookingEvent.NextStepClicked) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Barra de avance del Figma (`633:2856`): gris de fondo y naranja hasta el paso actual. */
@Composable
private fun StepProgress(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProgressHeight)
            .background(GreyLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(ProgressHeight)
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
}

/**
 * Tarjeta amarilla del temporizador (Figma `557:2793`). Tocarla lo inicia o lo pausa y "Reiniciar" vuelve al tiempo del
 * paso (desviación aprobada, spec §9). Al llegar a 0 dice "¡Tiempo!" y tocarla detiene la alarma. TalkBack anuncia la
 * acción y el estado.
 */
@Composable
private fun TimerCard(
    secondsRemaining: Int,
    running: Boolean,
    ringing: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) MaterialTheme.colorScheme.surfaceVariant else YellowSoft
    val toggleLabel = stringResource(
        id = when {
            ringing -> R.string.recipes_timer_stop_alarm
            running -> R.string.recipes_timer_pause
            else -> R.string.recipes_timer_start
        }
    )
    val stateLabel = stringResource(
        id = when {
            ringing -> R.string.recipes_timer_ringing
            running -> R.string.recipes_timer_running
            else -> R.string.recipes_timer_paused
        }
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(RoundedCornerShape(CardRadius))
            .background(container)
            .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
            .semantics { stateDescription = stateLabel }
            .padding(CardPadding)
    ) {
        Text(
            text = if (ringing) {
                stringResource(id = R.string.recipes_timer_done)
            } else {
                stringResource(id = R.string.recipes_timer_label, secondsRemaining / 60, secondsRemaining % 60)
            },
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        Text(
            text = stringResource(id = if (ringing) R.string.recipes_timer_ringing_hint else R.string.recipes_timer_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
        SmartFoodTextLink(
            text = stringResource(id = R.string.recipes_timer_reset),
            onClick = onReset,
            fontSize = 13.sp
        )
    }
}

/** "¿Cocinaste esta receta?", guardando y el resultado (CA-RIA-04-05; sin conexión, spec 006 §13). */
@Composable
private fun CookingDialogs(dialog: CookingDialog?, onEvent: (CookingEvent) -> Unit) {
    when (dialog) {
        CookingDialog.ConfirmFinish -> SmartFoodWarningDialog(
            title = stringResource(id = R.string.recipes_confirm_finish_title),
            body = stringResource(id = R.string.recipes_confirm_finish_body),
            confirmText = stringResource(id = R.string.recipes_finish),
            dismissText = stringResource(id = R.string.recipes_cancel),
            onConfirm = { onEvent(CookingEvent.ConfirmFinishClicked) },
            onDismiss = { onEvent(CookingEvent.DismissDialog) }
        )
        CookingDialog.Saving -> SmartFoodLoadingDialog(text = stringResource(id = R.string.recipes_cooking_saving))
        is CookingDialog.Saved -> if (dialog.onPhone) {
            SmartFoodResultDialog(
                icon = { ResultIcon(Icons.Outlined.CloudUpload, MaterialTheme.colorScheme.primary) },
                title = stringResource(id = R.string.recipes_cooking_saved_phone),
                body = stringResource(id = R.string.recipes_cooking_saved_phone_body)
            )
        } else {
            SmartFoodResultDialog(
                icon = { ResultIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
                title = stringResource(id = R.string.recipes_cooking_saved),
                body = stringResource(id = R.string.recipes_cooking_saved_body)
            )
        }
        is CookingDialog.Failed -> SmartFoodResultDialog(
            icon = { ResultIcon(Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error) },
            title = stringResource(id = R.string.recipes_cooking_failed),
            body = stringResource(id = dialog.error.messageRes()),
            destructive = true
        )
        null -> Unit
    }
}

/** Ícono del resultado: el glifo ocupa el 40 % del círculo, como en el inventario (T025). */
@Composable
private fun ResultIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction)
    )
}

@Composable
private fun Loading(modifier: Modifier) = Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Error(modifier: Modifier) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .fillMaxWidth()
        .padding(ContentPadding)
) {
    Text(
        text = stringResource(id = R.string.recipes_detail_error),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        textAlign = TextAlign.Center
    )
}

/** Sombra suave de las tarjetas del Figma: `0 3 6` verde oscuro al 9 %. */
private fun Modifier.cardShadow(): Modifier =
    figmaShadow(color = GreenDark.copy(alpha = CardShadowAlpha), borderRadius = CardRadius, blurRadius = 6.dp, offsetY = 3.dp)

/** Poppins Bold de 20 sp (título de la receta y del paso). */
@Composable
private fun TitleStyle() = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)

/** Poppins SemiBold de 13 sp (título de las tarjetas y "Salir"). */
@Composable
private fun CardTitleStyle() = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 19.5.sp, fontWeight = FontWeight.SemiBold)

/** Poppins Regular de 13 sp con interlineado 1,45. */
@Composable
private fun BodyStyle() = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 18.85.sp)

// Medidas del Figma 557:2819 y 557:2781. No son de formulario, así que no van en FormMetrics.
private val ContentPadding = 20.dp
private val ContentGap = 16.dp
private const val IntroPhotoRatio = 372f / 230f
private const val StepPhotoRatio = 372f / 250f
private val CardRadius = 20.dp
private val CardPadding = 16.dp
private val CardInnerGap = 12.dp
private const val CardShadowAlpha = 0.09f
private val ProgressHeight = 5.dp
private val ActionsGap = 10.dp
private const val GlyphFraction = 0.4f

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CookingScreenPreview() {
    SmartfoodTheme {
        CookingScreen(
            state = CookingUiState(
                isLoading = false,
                name = "Pollo guisado",
                photo = "receta_pollo_guisado",
                steps = listOf(
                    RecipeStep("Sofríe la cebolla y el tomate", "Cocina a fuego medio durante 5 minutos.", minutes = 5),
                    RecipeStep("Agrega el pollo", "Cocina tapado 20 minutos.", minutes = 20),
                ),
                timerSecondsRemaining = 222
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
