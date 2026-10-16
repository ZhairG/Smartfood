package com.smartfood.app.feature.inventory.data.repository

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Nombre: InventoryDocument.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Conversión entre [InventoryItem] y el documento `users/{uid}/inventory/{itemId}` de Firestore
 * (spec 006, plan §3). Trabaja con mapas para no depender de reflexión con R8 y no conoce Firebase, así que se
 * prueba en la JVM. Las marcas de tiempo (`createdAt`, `updatedAt`) las agrega el repositorio.
 */
internal object InventoryDocument {
    const val NAME = "name"
    const val NAME_NORMALIZED = "nameNormalized"
    const val CATEGORY = "category"
    const val EXPIRES_ON = "expiresOn"
    const val QUANTITY = "quantity"
    const val UNIT = "unit"
    const val PRICE = "price"
    const val PHOTO_PATH = "photoPath"
    const val PHOTO_URL = "photoUrl"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}

/**
 * Campos del documento de este producto. Sin cantidad no se escriben `quantity` ni `unit`, y sin foto no se
 * escriben `photoPath` ni `photoUrl` (T002).
 */
internal fun InventoryItem.toDocumentFields(): Map<String, Any> = buildMap {
    put(InventoryDocument.NAME, name)
    put(InventoryDocument.NAME_NORMALIZED, SearchInventoryUseCase.normalize(name))
    put(InventoryDocument.CATEGORY, category.name)
    // ISO "yyyy-MM-dd": un día sin hora (ADR-INV-02).
    put(InventoryDocument.EXPIRES_ON, expiresOn.toString())
    quantity?.let {
        put(InventoryDocument.QUANTITY, it.amount)
        put(InventoryDocument.UNIT, it.unit.name)
    }
    // Sin precio no se escribe la clave (T052): los documentos viejos siguen siendo válidos.
    price?.let { put(InventoryDocument.PRICE, it) }
    photo?.let {
        put(InventoryDocument.PHOTO_PATH, it.path)
        put(InventoryDocument.PHOTO_URL, it.url)
    }
}

/**
 * Lee un documento de forma tolerante: una categoría desconocida pasa a [FoodCategory.OTHER], y una cantidad o
 * unidad inválida se descarta. Devuelve `null` si falta el nombre o la fecha no es válida, para omitir el documento
 * sin romper la lista.
 *
 * @param id Identificador del documento.
 * @param data Campos del documento.
 */
internal fun inventoryItemFromDocument(id: String, data: Map<String, Any?>): InventoryItem? {
    val name = (data[InventoryDocument.NAME] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val expiresOn = (data[InventoryDocument.EXPIRES_ON] as? String)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    } ?: return null
    val category = enumOrNull<FoodCategory>(data[InventoryDocument.CATEGORY]) ?: FoodCategory.OTHER
    val amount = (data[InventoryDocument.QUANTITY] as? Number)?.toDouble()?.takeIf { it > 0 }
    val unit = enumOrNull<QuantityUnit>(data[InventoryDocument.UNIT])
    val price = (data[InventoryDocument.PRICE] as? Number)?.toLong()?.takeIf { it in 1..ValidateItemUseCase.MAX_PRICE }
    val photoPath = data[InventoryDocument.PHOTO_PATH] as? String
    val photoUrl = data[InventoryDocument.PHOTO_URL] as? String
    return InventoryItem(
        id = id,
        name = name,
        category = category,
        expiresOn = expiresOn,
        quantity = if (amount != null && unit != null) Quantity(amount, unit) else null,
        photo = if (photoPath != null && photoUrl != null) ItemPhoto(photoPath, photoUrl) else null,
        price = price,
    )
}

private inline fun <reified E : Enum<E>> enumOrNull(value: Any?): E? =
    (value as? String)?.let { code -> enumValues<E>().firstOrNull { it.name == code } }
