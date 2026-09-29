package com.smartfood.app.ui.auth.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.components.SmartFoodBrandHeader
import com.smartfood.app.ui.components.SmartFoodOfflineDialog
import com.smartfood.app.utils.SystemBarEffect
import com.smartfood.app.utils.clearFocusOnTap
import com.smartfood.app.utils.panelTopCorners
import kotlinx.coroutines.launch

/**
 * Nombre: AuthScreenScaffold.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Contenedor reutilizable de nivel superior para las pantallas del flujo de autenticación (Login, Registro).
 * Unifica el manejo de insets del sistema, adaptación del Status Bar, scroll automático con IME, encabezado de marca
 * y diálogo de desconexión fuera de línea para eliminar duplicación de código.
 *
 * @param state Objeto de estado [AuthUiState] para gestionar diálogos y errores de red.
 * @param onEvent Callback centralizado para despachar eventos [AuthEvent] al ViewModel.
 * @param headerContent Contenido Composable opcional situado en la parte superior del panel (ej. Título de bienvenida).
 * @param modifier Modificador base para el contenedor.
 * @param formContent Contenido Composable con los campos del formulario y botones de acción dentro del área scrollable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreenScaffold(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    headerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    formContent: @Composable ColumnScope.(ScrollState) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime

    SystemBarEffect(isLightBackground = false)

    LaunchedEffect(imeInsets, density) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { bottom ->
                if (bottom == 0) {
                    focusManager.clearFocus(force = true)
                    if (scrollState.value != 0) {
                        scrollState.scrollTo(0)
                    }
                }
            }
    }

    if (state.isOffline) {
        SmartFoodOfflineDialog(
            isReconnecting = state.isLoading,
            onReconnect = remember(onEvent) { { onEvent(AuthEvent.RetryConnectionClicked) } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clearFocusOnTap(focusManager) {
                coroutineScope.launch { scrollState.scrollTo(0) }
            }
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        SmartFoodBrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 77.dp, bottom = 53.dp),
            smartColor = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .panelTopCorners(MaterialTheme.colorScheme.surface, 45.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(top = 47.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp)
            ) {
                headerContent()
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 34.dp)
                    .padding(bottom = 48.dp)
            ) {
                formContent(scrollState)
            }
        }
    }
}
