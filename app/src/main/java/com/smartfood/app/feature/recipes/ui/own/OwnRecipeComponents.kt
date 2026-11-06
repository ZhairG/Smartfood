package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar

/**
 * Nombre: OwnRecipeComponents.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Piezas comunes de las pantallas de recetas propias (spec 010, T013 y T014): la acción de texto de la barra
 * ("Guardar", "Eliminar"), la fila de ingrediente con su chip y la pantalla centrada de resultado (Figma `557:2551` y
 * `557:2620`).
 */

/**
 * Acción de texto de la barra verde.
 *
 * @param text Texto.
 * @param onClick Acción.
 */
@Composable
internal fun BarTextAction(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = BarActionHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BarActionPadding)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * Fila de ingrediente del Figma (`557:2706`): carrito, nombre y chip verde con la cantidad.
 *
 * @param name Nombre del ingrediente.
 * @param quantity Cantidad con su unidad.
 * @param onClick Abre la hoja para cambiarla; `null` en el detalle, que es de solo lectura.
 */
@Composable
internal fun IngredientRow(name: String, quantity: String, onClick: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = RowVerticalPadding)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_figma_cart),
            contentDescription = null,
            modifier = Modifier.size(RowIconSize)
        )
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = quantity,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = ChipSidePadding, vertical = ChipVerticalPadding)
        )
    }
}

/**
 * Pantalla de resultado del Figma: barra, círculo con ícono, título, texto y botones.
 *
 * @param title Título de la barra.
 * @param onBack Flecha.
 * @param heading Título grande.
 * @param body Texto.
 * @param icon Círculo con su ícono.
 * @param actions Botones.
 */
@Composable
internal fun OwnRecipeResultLayout(
    title: String,
    onBack: () -> Unit,
    heading: String,
    body: String,
    icon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(title = title, onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(ResultGap, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ResultSidePadding)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) { icon() }
            Text(
                text = heading,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp)
            )
            actions()
        }
    }
}

/**
 * Círculo de 100 dp con un ícono centrado, como el Figma `666:3708`.
 *
 * @param color Fondo del círculo.
 * @param content Dibuja el ícono.
 */
@Composable
internal fun ResultCircle(color: Color, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ResultCircleSize)
            .clip(CircleShape)
            .background(color)
    ) { content() }
}

// Medidas del Figma 557:2693 y 557:2551. No son de formulario, así que no van en FormMetrics.
private val BarActionHeight = 48.dp
private val BarActionPadding = 8.dp
private val RowGap = 12.dp
private val RowVerticalPadding = 4.dp
private val RowIconSize = 30.dp
private val ChipSidePadding = 12.dp
private val ChipVerticalPadding = 7.dp
private val ResultGap = 18.dp
private val ResultSidePadding = 20.dp
private val ResultCircleSize = 100.dp
