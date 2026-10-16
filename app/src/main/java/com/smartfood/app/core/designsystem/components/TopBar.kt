package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: TopBar.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Barra superior de las pantallas internas del inventario (spec 006, CA-INV-09-04): fondo verde oscuro,
 * flecha atrás y título centrado, como en los frames "Añadir Producto" y "Foto_Producto" del Figma (`476:5960`). Tiene
 * un hueco opcional a la derecha para una acción como "Guardar" (editor, #40).
 */

/**
 * @param title Título centrado. Los lectores de pantalla lo anuncian como encabezado.
 * @param onBack Acción de la flecha atrás.
 * @param modifier Modificador opcional.
 * @param action Acción opcional a la derecha.
 */
@Composable
fun SmartFoodTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(TopBarHeight)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = TopBarSidePadding)) {
            Icon(
                // Ícono del Figma (`476:3670`, flecha "átras"), teñido con el tema.
                painter = painterResource(id = R.drawable.ic_figma_back),
                contentDescription = stringResource(id = R.string.cd_navigate_back),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(TopBarIconSize)
            )
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TitleSidePadding)
                .semantics { heading() }
        )
        if (action != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = TopBarSidePadding)) { action() }
        }
    }
}

// Figma (476:5960) mide 74 px contando la barra de estado; sin ella quedan unos 50, así que se usa el alto estándar
// de una barra superior (56 dp). No son medidas de formulario, por eso no van en FormMetrics.
private val TopBarHeight = 56.dp
private val TopBarSidePadding = 12.dp
private val TitleSidePadding = 64.dp
private val TopBarIconSize = 20.dp

@Preview(showBackground = true)
@Composable
private fun SmartFoodTopBarPreview() {
    SmartfoodTheme {
        SmartFoodTopBar(title = "Foto del producto", onBack = {})
    }
}
