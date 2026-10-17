/**
 * Nombre: InventorySearchScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla de búsqueda del inventario (spec 006, HU-INV-06 y HU-INV-07, CA-INV-06-01 a 06-08 y
 * CA-INV-07-01 a 07-09). Es stateless: recibe el estado y avisa por eventos, y adapta sus medidas a los tokens del
 * Design System (RNF-INV-06). Usa `SmartFoodTopBar` (Figma `476:5960`), `CategoryChips` y una fila propia
 * (`SearchResultRow`), sin miniatura ni lápiz, como el Figma `476:3190`. La tarjeta "Usa tu voz" solo se muestra si hay servicio (CA-INV-07-08) y el
 * aviso de voz (`VoiceSearchOverlay`) se dibuja encima cuando escucha, procesa o falla.
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceLight
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.ui.components.CategoryChips
import com.smartfood.app.feature.inventory.ui.components.PermissionRationale
import com.smartfood.app.feature.inventory.ui.components.VoiceSearchCard
import com.smartfood.app.feature.inventory.ui.components.VoiceSearchOverlay
import java.time.LocalDate

/**
 * Muestra la búsqueda del inventario.
 *
 * @param state Estado de la pantalla (texto, filtro, resultados, voz y carga).
 * @param onEvent Se llama con cada acción del usuario.
 * @param modifier Modificador opcional.
 * @param showPermissionRationale `true` para mostrar el aviso de permiso de micrófono denegado (CA-INV-07-06).
 * @param onOpenSettings Se llama al tocar "Abrir ajustes" en el aviso de permiso.
 * @param onDismissPermissionRationale Se llama al cerrar el aviso de permiso.
 */
@Composable
fun InventorySearchScreen(
    state: InventorySearchUiState,
    onEvent: (InventorySearchEvent) -> Unit,
    modifier: Modifier = Modifier,
    showPermissionRationale: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onDismissPermissionRationale: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Con el aviso de voz abierto, la búsqueda se desenfoca detrás (Figma 476:4507; API 31+).
        val blurRadius = if (state.voice == VoiceUiState.Hidden) 0.dp else VoiceBlur
        Column(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            SmartFoodTopBar(
                title = stringResource(id = R.string.inventory_title),
                onBack = { onEvent(InventorySearchEvent.BackClicked) }
            )

            // Figma 476:3190: la barra, la tarjeta de voz y los filtros van separados unos 24 dp.
            Column(
                verticalArrangement = Arrangement.spacedBy(BlockGap),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = SidePadding)
                    .padding(top = MaterialTheme.spacing.medium)
            ) {
                SearchField(
                    query = state.query,
                    onQueryChanged = { onEvent(InventorySearchEvent.QueryChanged(it)) },
                    onClearQuery = { onEvent(InventorySearchEvent.ClearQuery) }
                )

                if (state.isVoiceAvailable) {
                    VoiceSearchCard(onClick = { onEvent(InventorySearchEvent.VoiceClicked) })
                }

                CategoryChips(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { onEvent(InventorySearchEvent.CategorySelected(it)) }
                )

                // Todo el ancho: así el texto de "sin resultados" queda centrado, como en el Figma (476:3551).
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                        // Figma 476:3551: Poppins Bold de 15 px en verde oscuro, centrado en el espacio libre.
                        state.isEmptyResult -> Text(
                            text = stringResource(id = R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = state.results, key = { it.item.id }) { result ->
                                SearchResultRow(
                                    result = result,
                                    onClick = { id -> onEvent(InventorySearchEvent.ResultClicked(id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        VoiceSearchOverlay(
            state = state.voice,
            onRetry = { onEvent(InventorySearchEvent.VoiceRetry) },
            onDismiss = { onEvent(InventorySearchEvent.VoiceDismissed) }
        )

        if (showPermissionRationale) {
            PermissionRationale(
                icon = Icons.Outlined.Mic,
                title = stringResource(id = R.string.search_voice_permission_title),
                message = stringResource(id = R.string.search_voice_permission_body),
                onOpenSettings = onOpenSettings,
                onDismiss = onDismissPermissionRationale
            )
        }
    }
}

/**
 * Barra de texto de la búsqueda (CA-INV-06-01, CA-INV-06-07).
 *
 * @param query Texto actual de la barra.
 * @param onQueryChanged Se llama con cada carácter escrito.
 * @param onClearQuery Se llama al tocar la "x", que aparece solo con texto (CA-INV-06-07).
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val placeholder = stringResource(id = R.string.inventory_search_placeholder)
    // Figma 476:3203: Poppins Regular de 14 px.
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

    // Figma 476:3190: píldora gris sin borde, con la lupa a la izquierda y la "x" al escribir (CA-INV-06-07).
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SearchBarHeight)
                    // Figma 476:3200: relleno #F3F4F6 con borde #E5E7EB de 1 px y radio 24.
                    .background(color = SurfaceLight, shape = CircleShape)
                    .border(width = SearchBarBorder, color = GreyDivider, shape = CircleShape)
                    .padding(horizontal = MaterialTheme.spacing.medium)
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
                        .padding(horizontal = MaterialTheme.spacing.small)
                ) {
                    if (query.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    // Figma 476:3526: el x-circle gris de 18 px.
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_clear),
                            contentDescription = stringResource(id = R.string.cd_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ClearIconSize)
                        )
                    }
                }
            }
        }
    )
}

// Medida del Figma (476:3190): barra de búsqueda de 48 px de alto. No es de formulario, así que no va en FormMetrics.
private val SearchBarHeight = 48.dp

// Desenfoque del contenido con el aviso de voz abierto.
private val VoiceBlur = 8.dp

// Medidas del Figma (476:3190): 22 px entre bloques, márgenes de 20, lupa de 20 y borde de 1.
private val BlockGap = 22.dp
private val SidePadding = 20.dp
private val SearchIconSize = 20.dp
private val SearchBarBorder = 1.dp

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun InventorySearchScreenPreview() {
    SmartfoodTheme {
        InventorySearchScreen(
            state = InventorySearchUiState(
                query = "leche",
                isVoiceAvailable = true,
                results = listOf(
                    SearchResultUi(
                        InventoryItem("1", "Leche Entera", FoodCategory.DAIRY, LocalDate.of(2026, 10, 20)),
                        ExpiryStatus.FRESH
                    )
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun InventorySearchScreenEmptyPreview() {
    SmartfoodTheme { InventorySearchScreen(state = InventorySearchUiState(query = "pera"), onEvent = {}) }
}

/** La "x" para borrar la búsqueda: 18 px en el Figma (476:3526). */
private val ClearIconSize = 18.dp
