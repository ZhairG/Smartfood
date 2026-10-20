package com.smartfood.app.feature.recipes.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.figmaShadow

/**
 * Nombre: RecipeActionButton.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Botón naranja de las pantallas de recetas, como en el Figma: "Sugerirme recetas" (`557:3039`) y
 * "Empezar a cocinar" (`557:2884`): 48 dp de alto, radio 10, Poppins Bold de 15 sp y un ícono opcional a la derecha
 * (spec 009, T007 y T008). Mientras su pantalla no exista (#115, #116) se ve en un naranja pálido opaco y no responde;
 * TalkBack lo anuncia deshabilitado. Decisión de Zhair del 2026-10-20.
 *
 * @param text Texto del botón.
 * @param onClick Acción al tocarlo.
 * @param modifier Modificador opcional; el ancho lo pone quien lo usa.
 * @param enabled `false` lo atenúa y lo deshabilita.
 * @param trailingIcon Ícono del Figma a la derecha del texto, o `null`.
 */
@Composable
fun RecipeActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
) {
    val shape = RoundedCornerShape(ButtonRadius)
    // Atenuado con colores opacos (no con transparencia): así no se ve la lista a través ni se corta la sombra.
    val container = if (enabled) {
        MaterialTheme.colorScheme.secondary
    } else {
        lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondary, DisabledMix)
    }
    val content = MaterialTheme.colorScheme.onSecondary.copy(alpha = if (enabled) 1f else DisabledContentAlpha)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ButtonHeight)
            .figmaShadow(borderRadius = ButtonRadius)
            .clip(shape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.small)
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        if (trailingIcon != null) {
            // Decorativo: el texto ya dice qué hace el botón.
            Icon(
                painter = painterResource(id = trailingIcon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(IconSize)
            )
        }
    }
}

// Medidas del Figma (557:2884). No son de formulario, así que no van en FormMetrics.
private val ButtonHeight = 48.dp
private val ButtonRadius = 10.dp
private val IconSize = 24.dp
private const val DisabledMix = 0.5f
private const val DisabledContentAlpha = 0.85f

@Preview(showBackground = true)
@Composable
private fun RecipeActionButtonPreview() {
    SmartfoodTheme {
        RecipeActionButton(text = "Empezar a cocinar", onClick = {}, trailingIcon = R.drawable.ic_figma_play)
    }
}
