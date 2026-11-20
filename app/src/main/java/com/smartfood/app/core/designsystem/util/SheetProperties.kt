package com.smartfood.app.core.designsystem.util

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme

/**
 * Nombre: SheetProperties.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Propiedades de las hojas inferiores (`ModalBottomSheet`) según el tema de la app (spec 013,
 * CA-OSC-04-02). Sin ellas, Material deduce las barras del sistema del color del contenido: en oscuro, `TextMuted`
 * cuenta como "oscuro" y la barra de navegación queda clara. En claro dan el mismo resultado de antes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun smartFoodSheetProperties(): ModalBottomSheetProperties {
    val dark = LocalIsDarkTheme.current
    return remember(dark) {
        ModalBottomSheetProperties(
            isAppearanceLightStatusBars = !dark,
            isAppearanceLightNavigationBars = !dark
        )
    }
}
