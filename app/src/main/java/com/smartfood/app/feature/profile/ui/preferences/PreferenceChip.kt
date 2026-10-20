package com.smartfood.app.feature.profile.ui.preferences

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.White

/**
 * Nombre: PreferenceChip.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Chip de "Preferencias alimentarias" (spec 008, CA-PER-03-01; Figma `376:1260`). Está basado en el chip de
 * `CategoryChips`: el elegido va en verde con texto blanco y los demás claros con borde gris; TalkBack lo anuncia como
 * seleccionado (RNF-PER-03).
 *
 * @param text Nombre visible del chip.
 * @param selected `true` si el chip está elegido.
 * @param onClick Se llama al tocar el chip.
 * @param modifier Modificador opcional.
 */
@Composable
fun PreferenceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = GreenCheck,
            selectedLabelColor = White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = GreenCheck
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun PreferenceChipPreview() {
    SmartfoodTheme {
        PreferenceChip(text = "Vegetariano", selected = true, onClick = {})
    }
}
