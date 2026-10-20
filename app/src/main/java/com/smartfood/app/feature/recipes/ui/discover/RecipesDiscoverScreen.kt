package com.smartfood.app.feature.recipes.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartfood.app.feature.recipes.ui.components.RecipesHeader

/**
 * Nombre: RecipesDiscoverScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla Descubrir de la pestaña Recetas (spec 009, HU-RIA-01). En T006 solo muestra el encabezado
 * del Figma; el contenido (búsqueda, tarjeta "¿Qué cocinamos hoy?", filtros y tarjetas) llega en T007.
 */
@Composable
fun RecipesDiscoverScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RecipesHeader()
    }
}
