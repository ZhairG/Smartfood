package com.smartfood.app.feature.inventory.data.mapper

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException

/**
 * Nombre: InventoryErrorMapper.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Traduce las excepciones de Firestore y Storage a errores de negocio [InventoryError] (spec 006,
 * plan §10). Es el único lugar de la feature de inventario que conoce los tipos de excepción de Firebase.
 */
object InventoryErrorMapper {

    /** Clasifica [throwable] en un [InventoryError]. */
    fun map(throwable: Throwable): InventoryError = when (throwable) {
        is InventoryException -> throwable.error
        is FirebaseNetworkException -> InventoryError.Network
        is FirebaseFirestoreException -> when (throwable.code) {
            // Sin conexión, las transacciones fallan con UNAVAILABLE (RNF-INV-05).
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> InventoryError.Network
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> InventoryError.PermissionDenied
            FirebaseFirestoreException.Code.NOT_FOUND -> InventoryError.NotFound
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> InventoryError.NotAuthenticated
            else -> InventoryError.Unknown(throwable.code.name)
        }
        is StorageException -> mapStorageError(throwable.errorCode)
        else -> InventoryError.Unknown(throwable.message)
    }

    /**
     * Clasifica un error de Cloud Storage por su código. Se separa de [map] para probarlo en la JVM sin construir
     * la excepción.
     *
     * @param errorCode Valor de [StorageException.getErrorCode].
     */
    internal fun mapStorageError(errorCode: Int): InventoryError = when (errorCode) {
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> InventoryError.Network
        StorageException.ERROR_NOT_AUTHORIZED -> InventoryError.PermissionDenied
        StorageException.ERROR_OBJECT_NOT_FOUND -> InventoryError.NotFound
        else -> InventoryError.PhotoUploadFailed
    }

    /** Envuelve [throwable] en un [InventoryException] con el error ya clasificado. */
    fun toException(throwable: Throwable): InventoryException =
        throwable as? InventoryException ?: InventoryException(map(throwable))
}

/** Convierte cualquier fallo de este [Result] en un [InventoryException] clasificado. */
fun <T> Result<T>.mapInventoryError(): Result<T> =
    exceptionOrNull()?.let { Result.failure(InventoryErrorMapper.toException(it)) } ?: this
