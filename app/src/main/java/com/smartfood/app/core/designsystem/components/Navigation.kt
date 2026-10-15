package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: Navigation.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Barra inferior de la app (spec 006, T026, CA-INV-09-02), según la barra de 2 pestañas de la sección
 * "Natalia" del Figma (`476:5570`): fondo blanco con una línea arriba, pestañas centradas con ícono y texto, la
 * actual en verde y las demás en gris. No conoce las rutas: las pestañas llegan por parámetro.
 */

/**
 * Una pestaña de [SmartFoodBottomBar].
 *
 * @property label Texto de la pestaña.
 * @property icon Ícono de la pestaña (decorativo: el texto ya la nombra).
 * @property selected Si es la pestaña actual.
 * @property onClick Acción al tocarla.
 */
@Immutable
data class SmartFoodTab(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Barra inferior con las pestañas principales. Cada pestaña es un elemento seleccionable con rol de pestaña, así
 * que TalkBack anuncia "Inventario, pestaña, seleccionada". Respeta la barra de navegación del sistema.
 *
 * @param tabs Pestañas, en orden.
 * @param modifier Modificador opcional.
 */
@Composable
fun SmartFoodBottomBar(tabs: List<SmartFoodTab>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(TabSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab -> BottomBarTab(tab) }
        }
    }
}

@Composable
private fun BottomBarTab(tab: SmartFoodTab) {
    // La pestaña actual va en el verde del Figma (#16A34A, el mismo de las casillas); las demás en gris.
    val color = if (tab.selected) GreenCheck else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .selectable(selected = tab.selected, onClick = tab.onClick, role = Role.Tab)
            .padding(horizontal = TabTouchPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(TabIconSize))
        Text(
            text = tab.label,
            color = color,
            // labelLarge es el estilo de etiqueta con Poppins; el Figma usa Poppins SemiBold de 12 sp.
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

// Medidas de la barra del Figma (476:5570). No son de formulario, así que no van en FormMetrics.
private val BottomBarHeight = 75.dp
private val TabIconSize = 32.dp
private val TabSpacing = 40.dp
private val TabTouchPadding = 8.dp

@Preview(showBackground = true)
@Composable
private fun SmartFoodBottomBarPreview() {
    SmartfoodTheme {
        SmartFoodBottomBar(
            tabs = listOf(
                SmartFoodTab("Inventario", Icons.Outlined.Inventory2, selected = true, onClick = {}),
                SmartFoodTab("Perfil", Icons.Outlined.Person, selected = false, onClick = {}),
            )
        )
    }
}
