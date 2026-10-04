package com.smartfood.app.feature.auth.ui.components

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.SmartFoodBrandHeader
import com.smartfood.app.core.designsystem.components.SmartFoodOfflineDialog
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.core.designsystem.util.clearFocusOnTap
import com.smartfood.app.core.designsystem.util.panelTopCorners
import kotlinx.coroutines.launch

/**
 * Nombre: AuthScreenScaffold.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contenedor reutilizable de nivel superior respaldado por [Scaffold] de Material 3 para las pantallas
 * del flujo de autenticación (Login, Registro, Recuperación de contraseña).
 * Unifica el manejo de insets del sistema, adaptación del Status Bar, scroll automático con IME, encabezado de marca
 * y diálogo de desconexión fuera de línea para eliminar duplicación de código.
 *
 * @param isOffline Muestra el diálogo de desconexión (HU-06).
 * @param isReconnecting Indica si hay un reintento en curso dentro del diálogo.
 * @param onRetryConnection Acción del botón "Reconectar".
 * @param headerContent Contenido Composable opcional situado en la parte superior del panel (ej. Título de bienvenida).
 * @param modifier Modificador base para el contenedor.
 * @param topHeaderContent Contenido opcional sobre la marca (ej. indicador de pasos).
 * @param formContent Contenido Composable con los campos del formulario y botones de acción dentro del área scrollable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreenScaffold(
    headerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
    isReconnecting: Boolean = false,
    onRetryConnection: () -> Unit = {},
    topHeaderContent: (@Composable ColumnScope.() -> Unit)? = null,
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

    if (isOffline) {
        SmartFoodOfflineDialog(
            isReconnecting = isReconnecting,
            onReconnect = onRetryConnection
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .clearFocusOnTap(focusManager) {
                    coroutineScope.launch { scrollState.scrollTo(0) }
                }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (topHeaderContent != null) 20.dp else 40.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                topHeaderContent?.invoke(this)
                SmartFoodBrandHeader(
                    smartColor = MaterialTheme.colorScheme.onBackground
                )
            }

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
}
