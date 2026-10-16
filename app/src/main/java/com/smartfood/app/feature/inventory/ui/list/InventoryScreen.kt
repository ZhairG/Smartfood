/**
 * Nombre: InventoryScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla stateless de la lista de inventario (spec 006, HU-INV-01, CA-INV-01-01 a CA-INV-01-08):
 * barra superior con la lupa, las secciones de vencimiento con sus productos, el estado vacío, el error con reintento
 * y el botón "+" para añadir. Medidas del frame "Inventario" de Figma (`476:3095`). Es una pestaña, así que la barra
 * inferior la pone `MainTabScaffold`.
 */
package com.smartfood.app.feature.inventory.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventorySection
import com.smartfood.app.feature.inventory.ui.components.ExpirySectionHeader
import com.smartfood.app.feature.inventory.ui.components.InventoryItemRow
import com.smartfood.app.feature.inventory.ui.messageRes

/**
 * @param state Estado que pinta la pantalla.
 * @param onEvent Acción por cada evento de la UI.
 * @param modifier Modificador opcional.
 */
@Composable
fun InventoryScreen(
    state: InventoryUiState,
    onEvent: (InventoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        InventoryTopBar(onSearchClicked = { onEvent(InventoryEvent.SearchClicked) })
        Box(modifier = Modifier.weight(1f)) {
            val error = state.error
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> ErrorState(error = error, onRetry = { onEvent(InventoryEvent.RetryClicked) })
                state.isEmpty -> EmptyState(onAddClicked = { onEvent(InventoryEvent.AddClicked) })
                else -> InventoryList(sections = state.sections, onEvent = onEvent)
            }
            FloatingActionButton(
                onClick = { onEvent(InventoryEvent.AddClicked) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MaterialTheme.spacing.large),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_button)
                )
            }
        }
    }
}

/**
 * Barra superior de la pestaña: título centrado y lupa a la derecha (Figma `476:3096`). No usa `SmartFoodTopBar`
 * porque esa barra es de las pantallas internas y siempre lleva la flecha de volver (CA-INV-09-04).
 */
@Composable
private fun InventoryTopBar(
    onSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(TopBarHeight)
    ) {
        Text(
            text = stringResource(id = R.string.inventory_title),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = TopBarTitleSize,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TopBarTitleSidePadding)
                .semantics { heading() }
        )
        IconButton(
            onClick = onSearchClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.cd_search_button),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Lista de secciones con sus productos (CA-INV-01-01): cada sección encabeza con su estado y cada producto es una
 * fila con su separador.
 */
@Composable
private fun InventoryList(
    sections: List<InventorySection>,
    onEvent: (InventoryEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.extraLarge,
            end = MaterialTheme.spacing.extraLarge,
            top = MaterialTheme.spacing.large,
            bottom = FloatingActionButtonSpace
        )
    ) {
        sections.forEachIndexed { index, section ->
            item(key = "section_${section.status.name}") {
                // Figma 476:3095: unos 42 px entre el final de una sección y el encabezado de la siguiente.
                ExpirySectionHeader(
                    status = section.status,
                    modifier = if (index > 0) Modifier.padding(top = SectionGap) else Modifier
                )
            }
            items(items = section.items, key = { item -> item.id }) { item ->
                InventoryItemRow(
                    item = item,
                    status = section.status,
                    onEditClicked = { id -> onEvent(InventoryEvent.EditClicked(id)) }
                )
            }
        }
    }
}

/** Sin productos todavía: se invita a añadir el primero (CA-INV-01-05). */
@Composable
private fun EmptyState(onAddClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.inventory_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = stringResource(id = R.string.inventory_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.inventory_empty_action),
            onClick = onAddClicked
        )
    }
}

/** El inventario no se pudo leer: se muestra el error y se ofrece reintentar. */
@Composable
private fun ErrorState(
    error: InventoryError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = error.messageRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.inventory_retry),
            onClick = onRetry
        )
    }
}

// Medidas del Figma (`476:3096`): la barra mide 74 px contando la barra de estado y el título está a 20 px. Como en
// `SmartFoodTopBar`, se usa el alto estándar de una barra superior y 64 px de margen para que el título no se cruce
// con la lupa. No son medidas de formulario, así que no van en FormMetrics.
private val TopBarHeight = 56.dp
private val TopBarTitleSidePadding = 64.dp
private val TopBarTitleSize = 20.sp

/** Espacio al final de la lista para que el botón "+" no tape la última fila (FAB de 56 px más su margen). */
private val FloatingActionButtonSpace = 88.dp
private val SectionGap = 34.dp
