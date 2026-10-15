package com.smartfood.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartfood.app.R

/**
 * Nombre: Type.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Sistema tipográfico de la aplicación Smartfood.
 * Mapea la jerarquía visual de Figma a la estructura de [Typography] de Material 3.
 */

/**
 * Poppins, la familia del diseño de Figma (spec 002, CA-UI-07-01). Va empaquetada en `res/font` (licencia OFL en
 * `docs/licenses/OFL-Poppins.txt`), así que se ve igual en cualquier teléfono y sin conexión.
 */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * Configuración de la tipografía para la aplicación.
 */
val Typography = Typography(
    // Wordmark principal (Ej. "SmartFood")
    displayLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 60.sp
    ),
    // Título de bienvenida
    headlineLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 35.sp,
        lineHeight = 52.5.sp
    ),
    // Texto de cuerpo principal y etiquetas de campo
    bodyLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.5.sp
    ),
    // Enlaces e hipervínculos
    labelLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.5.sp
    ),
    // Texto de botones principales
    titleLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 33.sp
    )
)
