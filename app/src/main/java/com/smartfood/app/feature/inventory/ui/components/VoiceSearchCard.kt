/**
 * Nombre: VoiceSearchCard.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Tarjeta "Usa tu voz" de la búsqueda del inventario (spec 006, CA-INV-06-01 y CA-INV-07-01). Vive en
 * `ui/components/` porque la comparte la pantalla de búsqueda; solo se muestra cuando el teléfono tiene servicio de
 * reconocimiento de voz (CA-INV-07-08).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * @param onClick Se llama al tocar la tarjeta o su micrófono (CA-INV-07-01).
 * @param modifier Modificador opcional.
 */
@Composable
fun VoiceSearchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        // Figma 476:3190: tarjeta blanca con borde verde y esquinas de 20 px.
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorder, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(id = R.string.search_voice_card_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.search_voice_card_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(MicButtonSize),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(id = R.string.cd_mic_button)
                )
            }
        }
    }
}

// Medidas del Figma (`476:3190`): el micrófono es un círculo de 60 px centrado en la tarjeta. No son medidas de
// formulario, así que no van en FormMetrics.
private val MicButtonSize = 60.dp
private val CardRadius = 20.dp
private val CardBorder = 1.dp
