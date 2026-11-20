package com.smartfood.app.feature.profile.ui.preferences

import androidx.compose.animation.animateColorAsState
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion
import androidx.compose.runtime.getValue
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
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.smartFoodColors

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
    val container by animateColorAsState(if (selected) MaterialTheme.smartFoodColors.checkedFill else MaterialTheme.colorScheme.surfaceVariant, SmartFoodMotion.standard(), label = "container")
    val label by animateColorAsState(if (selected) MaterialTheme.smartFoodColors.onCheckedFill else MaterialTheme.colorScheme.onSurface, SmartFoodMotion.standard(), label = "label")
    val border by animateColorAsState(if (selected) MaterialTheme.smartFoodColors.checkedFill else MaterialTheme.colorScheme.outline, SmartFoodMotion.standard(), label = "border")
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
            // Los colores cambian en 300 ms (spec 007, CA-ANI-05-03).
            containerColor = container,
            labelColor = label,
            selectedContainerColor = container,
            selectedLabelColor = label
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = border,
            selectedBorderColor = border
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
