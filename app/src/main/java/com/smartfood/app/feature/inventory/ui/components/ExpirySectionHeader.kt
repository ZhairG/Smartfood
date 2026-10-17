/**
 * Nombre: ExpirySectionHeader.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Encabezado de las secciones de la lista de inventario: punto del color del estado y título (spec 006,
 * CA-INV-01-01). El título y el punto juntos evitan que el estado se comunique solo con color (RNF-INV-04).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.ErrorRed
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.YellowExpiring
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus

/**
 * Texto de cada [ExpiryStatus], el de su sección de la lista (CA-INV-01-01).
 */
val ExpiryStatus.titleRes: Int
    get() = when (this) {
        ExpiryStatus.EXPIRED -> R.string.inventory_section_expired
        ExpiryStatus.EXPIRING_SOON -> R.string.inventory_section_expiring_soon
        ExpiryStatus.FRESH -> R.string.inventory_section_fresh
    }

/**
 * Color de cada [ExpiryStatus]. El Figma (`476:3114`) usa `#DC2626`, `#F6E938` y `#84CC16`; por acuerdo del equipo
 * (spec 006 §8) Vencido y Fresco usan los tokens que ya existían, [ErrorRed] y [GreenCheck], y el amarillo de
 * "Próximo a vencer" es el token nuevo [YellowExpiring] del nodo `476:3121` (T027, #37).
 */
val ExpiryStatus.color: Color
    get() = when (this) {
        ExpiryStatus.EXPIRED -> ErrorRed
        ExpiryStatus.EXPIRING_SOON -> YellowExpiring
        ExpiryStatus.FRESH -> GreenCheck
    }

/**
 * @param status Estado de la sección que encabeza.
 * @param modifier Modificador opcional.
 */
@Composable
fun ExpirySectionHeader(
    status: ExpiryStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SectionVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(SectionDotSize)
                .background(color = status.color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(SectionDotGap))
        Text(
            text = stringResource(id = status.titleRes),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = SectionTitleSize,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
    }
}

// Medidas del Figma (`476:3114`): punto de 25 px, 12 px hasta el título y título Poppins Bold 17. No son de
// formulario, así que no van en FormMetrics.
private val SectionDotSize = 25.dp
private val SectionDotGap = 12.dp
private val SectionVerticalPadding = 8.dp
private val SectionTitleSize = 17.sp

@Preview(showBackground = true)
@Composable
private fun ExpirySectionHeaderPreview() {
    SmartfoodTheme { ExpirySectionHeader(status = ExpiryStatus.EXPIRING_SOON) }
}
