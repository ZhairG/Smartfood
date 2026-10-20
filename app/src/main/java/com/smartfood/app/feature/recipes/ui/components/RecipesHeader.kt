package com.smartfood.app.feature.recipes.ui.components

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
 * Nombre: RecipesHeader.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Encabezado de la pestaña Recetas (spec 009, T006, Figma `557:3028`): fondo verde, el tenedor y cuchillo
 * del Figma a la izquierda y el título "Recetas" centrado. No es clicable; el ícono es decorativo.
 */
@Composable
fun RecipesHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(RecipesHeaderHeight)
    ) {
        Icon(
            // Ícono del Figma (`557:3030`), teñido con el tema.
            painter = painterResource(id = R.drawable.ic_figma_fork_knife),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = RecipesHeaderSidePadding)
                .size(RecipesHeaderIconSize)
        )
        Text(
            text = stringResource(id = R.string.recipes_title),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = RecipesHeaderTitlePadding)
                .semantics { heading() }
        )
    }
}

// Medidas del encabezado del Figma (557:3028). No son de formulario, así que no van en FormMetrics.
private val RecipesHeaderHeight = 56.dp
private val RecipesHeaderSidePadding = 20.dp
private val RecipesHeaderIconSize = 20.dp
private val RecipesHeaderTitlePadding = 64.dp

@Preview(showBackground = true)
@Composable
private fun RecipesHeaderPreview() {
    SmartfoodTheme {
        RecipesHeader()
    }
}
