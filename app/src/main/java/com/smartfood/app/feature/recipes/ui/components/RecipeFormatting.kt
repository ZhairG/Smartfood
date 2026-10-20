package com.smartfood.app.feature.recipes.ui.components

import com.smartfood.app.R
import com.smartfood.app.feature.recipes.domain.model.Store
import java.text.NumberFormat
import java.util.Locale

/**
 * Nombre: RecipeFormatting.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Formatos de las pantallas de recetas (spec 009, T007, T008 y T033): pesos con puntos de miles,
 * cantidades con coma decimal, en español de Colombia, y el nombre de cada supermercado.
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

/** Nombre del supermercado para mostrar (CA-RIA-04-07). */
val Store.titleRes: Int
    get() = when (this) {
        Store.EXITO -> R.string.recipes_store_exito
        Store.CARULLA -> R.string.recipes_store_carulla
        Store.OLIMPICA -> R.string.recipes_store_olimpica
        Store.D1 -> R.string.recipes_store_d1
    }
