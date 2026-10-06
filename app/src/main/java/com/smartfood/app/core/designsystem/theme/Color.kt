package com.smartfood.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * <build>
 * <b>Nombre:</b> Color.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 19/09/2026<br>
 * <b>Descripción:</b> Paleta de colores centralizada para la aplicación Smartfood. Define la identidad visual del<br>
 * Design System en base a las especificaciones de Figma.<br>
 * </build>
 */

// === Colores Base ===
/** Blanco absoluto. */
val White = Color(0xFFFFFFFF)

/** Negro absoluto. */
val Black = Color(0xFF000000)

/** Color transparente. */
val Transparent = Color(0x00FFFFFF)

// === Verdes (Tono Principal) ===
/** Verde principal de la marca. */
val GreenMain = Color(0xFF4ADE80)

/** Verde de símbolo de verificación en pantallas de éxito. */
val GreenSymbol = Color(0xFF22C55E)

/** Verde oscuro usado en fondos y contenedores. */
val GreenDark = Color(0xFF14532D)

/** Verde más oscuro usado en fondos y contenedores del modo oscuro. */
val GreenDark2 = Color(0xFF10271C)

/** Verde de la casilla de verificación y del check de requisitos cumplidos (base de [GreenMain50] y [GreenMain20]). */
val GreenCheck = Color(0xFF16A34A)

/** Verde claro suave para fondos de tarjetas y contenedores claros. */
val GreenLight = Color(0xFFF0FDF4)

/** Verde principal al 50% de opacidad. */
val GreenMain50 = Color(0x8016A34A)

/** Verde principal al 20% de opacidad. */
val GreenMain20 = Color(0x3316A34A)

// === Naranjas (Tono Secundario) ===
/** Naranja principal para acentos e interacciones secundarias. */
val OrangeMain = Color(0xFFFB923C)

/** Naranja oscuro para texto e íconos sobre fondos de contenedor secundario. */
val OrangeDark = Color(0xFFDE7D1C)

/** Naranja claro para contenedores secundarios. */
val OrangeLight = Color(0xFFFFE8CD)

// === Grises (Neutrales, Textos y Superficies) ===
/** Gris oscuro para texto primario neutral. */
val GreyDark = Color(0xFF555555)

/** Gris neutral para texto secundario e íconos deshabilitados. */
val GreyNeutral = Color(0xFF6F7C74)

/** Gris neutral al 35% de opacidad. */
val GreyNeutral35 = Color(0x596F7C74)

/** Gris claro para elementos deshabilitados. */
val GreyLight = Color(0xFF9AA39D)

/** Gris para divisores y bordes estándar. */
val GreyDivider = Color(0xFFD9D9D9)

/** Gris transparente al 60% de opacidad. */
val GreyTransparent60 = Color(0x99CCCCCC)

// === Estados de error ===
/** Rojo semántico para estados de error y validaciones. */
val ErrorRed = Color(0xFFD92E2E)

// === Superficies y Paneles Dark / Light ===
/** Fondo del panel en modo oscuro. */
val SurfaceDark = Color(0xFF0E2118)

/** Fondo de los campos de texto en modo oscuro. */
val InputDark = Color(0xFF172B21)

/** Borde de los campos de texto en modo oscuro. */
val BorderDark = Color(0xFF34423A)

/** Texto atenuado para descripciones y placeholders en modo oscuro. */
val TextMuted = Color(0xFFA8B7AE)

/** Fondo claro para el panel en modo claro. */
val SurfaceLight = Color(0xFFF3F7F4)

// === Sombras ===
/** Color de sombra estándar de botones (30% opacidad). */
val ShadowColorStandard = Color(0x4D000000)

/** Color de sombra de diálogos modales (50% opacidad). */
val ShadowColorModal = Color(0x80000000)
