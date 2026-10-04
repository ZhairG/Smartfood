package com.smartfood.app.core.designsystem.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nombre: FormMetrics.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Medidas de los formularios según el alto útil de la pantalla, es decir, sin las barras del
 * sistema (spec 002, RNF-UI-03). Hay tres perfiles: [Regular] (Figma, tablets), [Medium] (teléfonos altos) y
 * [Compact] (teléfonos desde 360×720 dp), para que Login, Registro y Recuperación quepan sin scroll (CA-UI-01-01).
 *
 * @property inlineBrand Si es `true`, el logo y el nombre van en una fila en lugar de una columna.
 * @property logoSize Diámetro del logo en el encabezado.
 * @property wordmarkFontSize Tamaño del nombre "SmartFood".
 * @property wordmarkLineHeight Alto de línea del nombre "SmartFood".
 * @property headerPaddingTop Margen superior del encabezado de marca.
 * @property headerPaddingBottom Margen inferior del encabezado de marca.
 * @property panelPaddingTop Margen superior dentro del panel del formulario.
 * @property panelPaddingBottom Margen inferior dentro del panel del formulario.
 * @property formHorizontalPadding Margen lateral del formulario.
 * @property titleFontSize Tamaño del título de cada pantalla.
 * @property titleLineHeight Alto de línea del título.
 * @property labelFontSize Tamaño de las etiquetas de los campos.
 * @property labelSpacing Separación entre la etiqueta y la caja del campo.
 * @property fieldSpacing Separación entre campos consecutivos.
 * @property sectionSpacing Separación entre bloques (campos, opciones, botón, enlaces).
 * @property inputHeight Alto de la caja de texto.
 * @property buttonHeight Alto del botón principal.
 * @property otpBoxSize Lado de cada casilla del código de verificación.
 * @property successIconSize Diámetro del ícono de las pantallas de éxito.
 * @property stepIndicatorPadding Margen vertical del indicador de pasos.
 */
@Immutable
data class FormMetrics(
    val inlineBrand: Boolean,
    val logoSize: Dp,
    val wordmarkFontSize: TextUnit,
    val wordmarkLineHeight: TextUnit,
    val headerPaddingTop: Dp,
    val headerPaddingBottom: Dp,
    val panelPaddingTop: Dp,
    val panelPaddingBottom: Dp,
    val formHorizontalPadding: Dp,
    val titleFontSize: TextUnit,
    val titleLineHeight: TextUnit,
    val labelFontSize: TextUnit,
    val labelSpacing: Dp,
    val fieldSpacing: Dp,
    val sectionSpacing: Dp,
    val inputHeight: Dp,
    val buttonHeight: Dp,
    val otpBoxSize: Dp,
    val successIconSize: Dp,
    val stepIndicatorPadding: Dp
) {
    companion object {
        /** Medidas exactas de Figma; el Registro necesita ~965 dp de alto útil (CA-UI-01-03). */
        val Regular = FormMetrics(
            inlineBrand = false,
            logoSize = 100.dp,
            wordmarkFontSize = 40.sp,
            wordmarkLineHeight = 60.sp,
            headerPaddingTop = 40.dp,
            headerPaddingBottom = 32.dp,
            panelPaddingTop = 47.dp,
            panelPaddingBottom = 48.dp,
            formHorizontalPadding = 34.dp,
            titleFontSize = 35.sp,
            titleLineHeight = 52.5.sp,
            labelFontSize = 15.sp,
            labelSpacing = 9.dp,
            fieldSpacing = 8.dp,
            sectionSpacing = 24.dp,
            inputHeight = 50.dp,
            buttonHeight = 51.dp,
            otpBoxSize = 60.dp,
            successIconSize = 176.dp,
            stepIndicatorPadding = 12.dp
        )

        /** Teléfonos altos: alto útil de 760 a 1000 dp (CA-UI-01-04). */
        val Medium = FormMetrics(
            inlineBrand = false,
            logoSize = 72.dp,
            wordmarkFontSize = 32.sp,
            wordmarkLineHeight = 40.sp,
            headerPaddingTop = 24.dp,
            headerPaddingBottom = 20.dp,
            panelPaddingTop = 28.dp,
            panelPaddingBottom = 16.dp,
            formHorizontalPadding = 30.dp,
            titleFontSize = 30.sp,
            titleLineHeight = 40.sp,
            labelFontSize = 15.sp,
            labelSpacing = 6.dp,
            fieldSpacing = 8.dp,
            sectionSpacing = 16.dp,
            inputHeight = 50.dp,
            buttonHeight = 50.dp,
            otpBoxSize = 56.dp,
            successIconSize = 150.dp,
            stepIndicatorPadding = 10.dp
        )

        /** Alto útil menor de 760 dp: logo y nombre en una fila (CA-UI-01-05). */
        val Compact = FormMetrics(
            inlineBrand = true,
            logoSize = 48.dp,
            wordmarkFontSize = 28.sp,
            wordmarkLineHeight = 36.sp,
            headerPaddingTop = 12.dp,
            headerPaddingBottom = 12.dp,
            panelPaddingTop = 20.dp,
            panelPaddingBottom = 12.dp,
            formHorizontalPadding = 24.dp,
            titleFontSize = 26.sp,
            titleLineHeight = 32.sp,
            labelFontSize = 14.sp,
            labelSpacing = 4.dp,
            fieldSpacing = 8.dp,
            sectionSpacing = 12.dp,
            inputHeight = 46.dp,
            buttonHeight = 48.dp,
            otpBoxSize = 52.dp,
            successIconSize = 120.dp,
            stepIndicatorPadding = 6.dp
        )

        /** Alto útil mínimo para usar [Medium]. */
        val MediumMinHeight = 760.dp

        /** Alto útil mínimo para usar [Regular]. */
        val RegularMinHeight = 1000.dp
    }
}

/**
 * Elige el perfil de medidas según el alto útil.
 *
 * @param availableHeight Alto útil de la pantalla, sin las barras del sistema.
 */
fun formMetricsFor(availableHeight: Dp): FormMetrics = when {
    availableHeight >= FormMetrics.RegularMinHeight -> FormMetrics.Regular
    availableHeight >= FormMetrics.MediumMinHeight -> FormMetrics.Medium
    else -> FormMetrics.Compact
}

/**
 * Perfil de medidas para un contenedor de alto total [maxHeight] (por ejemplo, el de un `BoxWithConstraints` a
 * pantalla completa). Descuenta las barras de estado y de navegación, que ocupan parte de ese alto.
 */
@Composable
fun formMetricsForWindow(maxHeight: Dp): FormMetrics {
    val density = LocalDensity.current
    val systemBars = WindowInsets.systemBars
    val barsHeight = with(density) { (systemBars.getTop(this) + systemBars.getBottom(this)).toDp() }
    return formMetricsFor(maxHeight - barsHeight)
}

/** Medidas vigentes. Por defecto, las de Figma: las pantallas que no lo proveen no cambian. */
val LocalFormMetrics = staticCompositionLocalOf { FormMetrics.Regular }

/** Estilo del título de cada pantalla de formulario, ajustado al perfil vigente. */
val FormMetrics.titleStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.headlineLarge.copy(fontSize = titleFontSize, lineHeight = titleLineHeight)
