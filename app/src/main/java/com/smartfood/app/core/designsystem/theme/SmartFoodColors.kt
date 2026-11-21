package com.smartfood.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Nombre: SmartFoodColors.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Roles de color propios de la app (spec 013, RF-OSC-01). Cubren lo que los roles de Material no
 * distinguen; por ejemplo, el encabezado es `primary` en claro pero no en oscuro. Cada rol claro vale el token que las
 * pantallas usaban antes (RNF-OSC-01), así que el modo claro no cambia.
 *
 * @param header Encabezados, tarjeta "¿Qué cocinamos hoy?" y píldora de precios.
 * @param onHeader Texto e íconos sobre [header].
 * @param onHeaderVariant Correo, círculo del avatar y su borde sobre [header].
 * @param checkedFill Relleno verde de los botones "Guardar" de hojas y avisos y del chip marcado de Preferencias.
 * @param onCheckedFill Texto sobre [checkedFill].
 * @param softFill Buscadores y chips sin seleccionar.
 * @param divider Bordes y divisores grises.
 * @param greenSoft Fotos vacías y tarjetas verdes suaves.
 * @param highlight Tarjetas amarillas (temporizador, ayuda de la IA, chip resaltado).
 * @param onHighlight Texto sobre [highlight].
 * @param tag Fondo de la etiqueta de categoría de la lista del inventario.
 * @param onTag Texto sobre [tag].
 * @param greenIcon Íconos verdes dentro de `primaryContainer`.
 * @param veil Velo que separa el aviso de voz del contenido.
 */
@Immutable
data class SmartFoodColors(
    val header: Color,
    val onHeader: Color,
    val onHeaderVariant: Color,
    val checkedFill: Color,
    val onCheckedFill: Color,
    val softFill: Color,
    val divider: Color,
    val greenSoft: Color,
    val highlight: Color,
    val onHighlight: Color,
    val tag: Color,
    val onTag: Color,
    val greenIcon: Color,
    val veil: Color,
)

/** Opacidad del velo del aviso de voz (Figma `476:4507`). */
private const val VeilAlpha = 0.35f

/** Roles del modo claro: los mismos tokens que usaban las pantallas (CA-OSC-01-03). */
val LightSmartFoodColors = SmartFoodColors(
    header = GreenDark,
    onHeader = White,
    onHeaderVariant = GreenLight,
    checkedFill = GreenCheck,
    onCheckedFill = White,
    softFill = SurfaceLight,
    divider = GreyDivider,
    greenSoft = GreenSoft,
    highlight = YellowSoft,
    onHighlight = GreenDark,
    tag = GreenMain20,
    onTag = GreenCheck,
    greenIcon = GreenCheck,
    veil = White.copy(alpha = VeilAlpha)
)

/** Roles del modo oscuro (spec 013 §9). */
val DarkSmartFoodColors = SmartFoodColors(
    header = GreenDark,
    onHeader = White,
    onHeaderVariant = GreenLight,
    checkedFill = GreenMain,
    onCheckedFill = GreenDark2,
    softFill = InputDark,
    divider = BorderDark,
    greenSoft = InputDark,
    highlight = InputDark,
    onHighlight = White,
    tag = GreenDark,
    onTag = GreenMain,
    greenIcon = GreenMain,
    veil = Black.copy(alpha = VeilAlpha)
)

/** Roles vigentes; los provee `SmartfoodTheme` según el modo. */
val LocalSmartFoodColors = staticCompositionLocalOf { LightSmartFoodColors }

/** Acceso a los roles propios, como `MaterialTheme.spacing`. */
val MaterialTheme.smartFoodColors: SmartFoodColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSmartFoodColors.current
