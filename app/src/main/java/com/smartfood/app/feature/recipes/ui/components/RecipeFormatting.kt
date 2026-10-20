package com.smartfood.app.feature.recipes.ui.components

import java.text.NumberFormat
import java.util.Locale

/**
 * Nombre: RecipeFormatting.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Formatos de las pantallas de recetas (spec 009, T007 y T008): pesos con puntos de miles y cantidades
 * con coma decimal, en español de Colombia.
 */

private val Colombian = Locale.forLanguageTag("es-CO")

/** Precio en pesos, como "8.400". */
fun formatCop(value: Long): String =
    NumberFormat.getIntegerInstance(Colombian).format(value)

/** Cantidad con hasta un decimal, como "300" o "2,5". */
fun formatQuantity(value: Double): String =
    NumberFormat.getNumberInstance(Colombian).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }.format(value)
