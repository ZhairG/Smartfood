package com.smartfood.app.feature.inventory.domain.model

import java.time.LocalDate

/**
 * Nombre: InventoryItem.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Modelos del inventario de alimentos (spec 006, plan §2): el producto, su categoría, su cantidad
 * opcional, su foto y el estado de vencimiento con el que se agrupa la lista.
 */

/** Categoría de un producto. El nombre de cada valor es el código estable que se guarda en la nube. */
enum class FoodCategory { FRUITS, DAIRY, PROTEINS, VEGETABLES, GRAINS, OTHER }

/** Estado de un producto según los días que faltan para su vencimiento (RF-INV-02). */
enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, FRESH }

/** Unidad de la cantidad de un producto (T002). El nombre de cada valor es el código estable en la nube. */
enum class QuantityUnit { UNITS, GRAMS, KILOGRAMS, POUNDS, MILLILITERS, LITERS }

/**
 * Cantidad de un producto con su unidad. Al ser un solo valor opcional, la cantidad y la unidad siempre van juntas.
 *
 * @property amount Valor mayor que 0 y hasta 9999, con 2 decimales como máximo (CA-INV-02-09).
 * @property unit Unidad de la cantidad.
 */
data class Quantity(val amount: Double, val unit: QuantityUnit)

/**
 * Foto de un producto guardada en la nube.
 *
 * @property path Ruta del archivo en Cloud Storage, necesaria para borrarlo.
 * @property url URL de descarga para mostrar la imagen.
 */
data class ItemPhoto(val path: String, val url: String)

/**
 * Producto del inventario.
 *
 * @property id Identificador del documento.
 * @property name Nombre recortado, de 1 a 50 caracteres.
 * @property category Categoría del producto.
 * @property expiresOn Día de vencimiento, sin hora (ADR-INV-02).
 * @property quantity Cantidad con su unidad, si se indicó (T002).
 * @property photo Foto del producto, si tiene.
 */
data class InventoryItem(
    val id: String,
    val name: String,
    val category: FoodCategory,
    val expiresOn: LocalDate,
    val quantity: Quantity? = null,
    val photo: ItemPhoto? = null,
    /** Precio en pesos colombianos enteros, opcional (CA-INV-02-10, T052). */
    val price: Long? = null,
    // El precio se agregará aquí como campo opcional en la spec de presupuesto (spec §7).
)

/** Sección de la lista: los productos que comparten un [ExpiryStatus] (CA-INV-01-01). */
data class InventorySection(val status: ExpiryStatus, val items: List<InventoryItem>)

/**
 * Borrador del formulario del producto, tal como lo escribió el usuario. Lo valida `ValidateItemUseCase`.
 *
 * @property quantityText Cantidad como texto; vacía si el usuario no indicó cantidad.
 * @property unit Unidad elegida; `null` si no se eligió.
 */
data class ItemDraft(
    val name: String,
    val expiresOn: LocalDate?,
    val category: FoodCategory?,
    val quantityText: String = "",
    val unit: QuantityUnit? = null,
    /** Precio tal como se escribió ("12500" o "12.500"); vacío es "sin precio" (T052). */
    val priceText: String = "",
)

/** Datos de un producto ya validados: nombre recortado, campos obligatorios presentes y cantidad interpretada. */
data class ValidItem(
    val name: String,
    val expiresOn: LocalDate,
    val category: FoodCategory,
    val quantity: Quantity?,
    val price: Long? = null,
)

/** Foto tomada con la cámara que todavía está en el teléfono, antes de subirla. */
@JvmInline
value class LocalPhoto(val filePath: String)
