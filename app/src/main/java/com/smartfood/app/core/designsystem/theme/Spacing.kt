package com.smartfood.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nombre: Spacing.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Define el Design System de espaciados estandarizados para la aplicación.
 * Garantiza proporciones visuales consistentes en todas las pantallas.
 *
 * @property extraSmall Espaciado de 4dp para márgenes mínimos.
 * @property small Espaciado de 8dp para separación pequeña.
 * @property medium Espaciado de 16dp para separación estándar.
 * @property large Espaciado de 24dp para separación amplia.
 * @property extraLarge Espaciado de 32dp para separaciones de secciones principales.
 * @property paddingForm Margen horizontal estándar del formulario (34dp).
 * @property topPanel Margen superior del panel inferior (47dp).
 * @property buttonRadius Radio de curvatura de esquinas de botón (10dp).
 * @property inputRadius Radio de curvatura de esquinas de caja de texto (10dp).
 * @property panelRadius Radio de curvatura de la tarjeta de los diálogos de inventario (50dp, Figma `476:3810`). El
 * diálogo de desconexión usa el suyo, de 45dp (Figma `700:1913`, spec 001).
 * @property buttonHeight Alto del botón primario (51dp).
 * @property inputHeight Alto de los campos de texto (50dp).
 * @property iconDialogSize Tamaño del círculo del ícono de los diálogos de inventario (100dp, Figma `476:3811`). El
 * diálogo de desconexión usa el suyo, de 72dp (Figma `700:1913`).
 */
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val paddingForm: Dp = 34.dp,
    val topPanel: Dp = 47.dp,
    val buttonRadius: Dp = 10.dp,
    val inputRadius: Dp = 10.dp,
    val panelRadius: Dp = 50.dp,
    val buttonHeight: Dp = 51.dp,
    val inputHeight: Dp = 50.dp,
    val iconDialogSize: Dp = 100.dp
)

/** CompositionLocal para proveer el objeto de espaciado en la jerarquía de Composables. */
val LocalSpacing = compositionLocalOf { Spacing() }

/**
 * Extensión para acceder al objeto [Spacing] directamente desde [MaterialTheme].
 * Ejemplo de uso: `Modifier.padding(MaterialTheme.spacing.medium)`
 *
 * @return El objeto [Spacing] actual proveído por el tema.
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
