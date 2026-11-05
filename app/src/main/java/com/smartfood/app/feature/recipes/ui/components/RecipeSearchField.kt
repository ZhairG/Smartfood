package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SurfaceLight

/**
 * Nombre: RecipeSearchField.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Búsqueda de Recetas (Figma `557:3035`) y de Guardadas (`633:2723`): píldora gris con la lupa y la "x" al
 * escribir. Se comparte para que las dos pantallas se vean igual (spec 010, CA-SAV-02-02).
 *
 * @param query Texto escrito.
 * @param placeholder Texto de ayuda; también lo anuncia TalkBack.
 * @param onQueryChanged Se llama al escribir.
 * @param onClearQuery Se llama al tocar la "x".
 * @param modifier Modificador opcional.
 */
@Composable
fun RecipeSearchField(
    query: String,
    placeholder: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SearchHeight)
                    .background(color = SurfaceLight, shape = CircleShape)
                    .padding(horizontal = SearchSidePadding)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_search_bar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SearchIconSize)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = SearchTextGap)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_clear),
                            contentDescription = stringResource(id = R.string.cd_recipes_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ClearIconSize)
                        )
                    }
                }
            }
        }
    )
}

// Medidas del Figma 557:3035. No son de formulario, así que no van en FormMetrics.
private val SearchHeight = 48.dp
private val SearchSidePadding = 16.dp
private val SearchTextGap = 8.dp
private val SearchIconSize = 20.dp
private val ClearIconSize = 18.dp
