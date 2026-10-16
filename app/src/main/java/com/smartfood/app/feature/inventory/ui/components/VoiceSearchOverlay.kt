/**
 * Nombre: VoiceSearchOverlay.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Aviso de voz que se dibuja encima de la búsqueda (spec 006, HU-INV-07, ADR-INV-04): "Habla ahora" con
 * ondas mientras escucha (CA-INV-07-01), "…" mientras procesa (CA-INV-07-02) y "No se entendió. Reinténtalo" con
 * "Reintentar" cuando falla (CA-INV-07-05). Tocar fuera o volver atrás cancela la escucha (CA-INV-07-07). Las ondas
 * se dibujan con Compose (plan §8): no son un asset de Figma. Medidas del componente "Grabadora" (`476:4507`).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.feature.inventory.ui.messageRes
import com.smartfood.app.feature.inventory.ui.search.VoiceUiState

/**
 * @param state Estado del aviso de voz; con [VoiceUiState.Hidden] no dibuja nada.
 * @param onRetry Se llama al tocar "Reintentar" (CA-INV-07-05).
 * @param onDismiss Se llama al tocar fuera o volver atrás (CA-INV-07-07).
 * @param modifier Modificador opcional.
 */
@Composable
fun VoiceSearchOverlay(
    state: VoiceUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == VoiceUiState.Hidden) return

    BackHandler(onBack = onDismiss)

    val dismissLabel = stringResource(id = R.string.cd_voice_dismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrimColor)
            .clickable(onClick = onDismiss)
            .semantics { contentDescription = dismissLabel },
        contentAlignment = Alignment.Center
    ) {
        Card(
            // Consume los toques sobre la tarjeta para que solo el fondo cierre el aviso (CA-INV-07-07).
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(MaterialTheme.spacing.inputRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                when (state) {
                    VoiceUiState.Listening -> {
                        VoiceWaves(modifier = Modifier.size(WavesSize))
                        Text(
                            text = stringResource(id = R.string.search_voice_listening),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    VoiceUiState.Processing -> {
                        VoiceWaves(modifier = Modifier.size(WavesSize))
                        Text(
                            text = stringResource(id = R.string.search_voice_processing),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    is VoiceUiState.Failed -> {
                        Box(
                            modifier = Modifier
                                .size(ErrorCircleSize)
                                .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(ErrorIconSize)
                            )
                        }
                        Text(
                            text = stringResource(id = state.error.messageRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        SmartFoodPrimaryButton(
                            text = stringResource(id = R.string.search_voice_retry),
                            onClick = onRetry
                        )
                    }

                    VoiceUiState.Hidden -> Unit
                }
            }
        }
    }
}

/**
 * Ondas de la escucha, dibujadas con Compose (plan §8): tres aros que crecen desde el micrófono en bucle.
 */
@Composable
private fun VoiceWaves(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voice_waves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WavePeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_waves_phase"
    )
    val waveColor = MaterialTheme.colorScheme.primary
    val micColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            repeat(WaveCount) { index ->
                val progress = (phase + index / WaveCount.toFloat()) % 1f
                drawCircle(
                    color = waveColor.copy(alpha = (1f - progress) * MaxWaveAlpha),
                    radius = maxRadius * progress,
                    center = center,
                    style = Stroke(width = WaveStroke.toPx())
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = micColor,
            modifier = Modifier.size(MicIconSize)
        )
    }
}

/** Velo oscuro que separa el aviso del contenido de la búsqueda. */
private val ScrimColor = Color.Black.copy(alpha = 0.6f)

// Medidas del componente "Grabadora" (`476:4507`): ondas de 132 px y círculo de error de 100 px. No son medidas de
// formulario, así que no van en FormMetrics.
private val WavesSize = 132.dp
private val MicIconSize = 48.dp
private val ErrorCircleSize = 100.dp
private val ErrorIconSize = 52.dp
private const val WaveCount = 3
private const val WavePeriodMs = 1400
private const val MaxWaveAlpha = 0.5f
private val WaveStroke = 3.dp
