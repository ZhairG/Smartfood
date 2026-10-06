package com.smartfood.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodBrandHeader
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.SystemBarEffect

/**
 * <build>
 * <b>Nombre:</b> HomeScreen.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pantalla de inicio provisional (HU-05). Saluda al usuario por su nombre y permite cerrar<br>
 * sesión. Aquí se integrarán el inventario y las recetas recomendadas en las próximas features.<br>
 * </build>
 *
 * @param state Estado inmutable [HomeUiState].
 * @param onEvent Callback para despachar eventos [HomeEvent].
 * @param modifier Modificador opcional.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SystemBarEffect(isLightBackground = !LocalIsDarkTheme.current)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = MaterialTheme.spacing.paddingForm, vertical = MaterialTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SmartFoodBrandHeader(smartColor = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))

            Text(
                text = state.firstName?.let { stringResource(id = R.string.home_greeting, it) }
                    ?: stringResource(id = R.string.home_greeting_generic),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = stringResource(id = R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))

            SmartFoodPrimaryButton(
                text = stringResource(id = R.string.action_logout),
                onClick = remember(onEvent) { { onEvent(HomeEvent.LogoutClicked) } }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    SmartfoodTheme {
        HomeScreen(state = HomeUiState(isLoading = false, firstName = "Pepito"), onEvent = {})
    }
}
