package com.smartfood.app.feature.inventory.domain.model

/**
 * Nombre: InventoryError.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Errores de negocio tipados del inventario (spec 006, plan §10). La capa de datos traduce cada falla
 * técnica a un [InventoryError] y la UI decide qué texto mostrar.
 */

/** Clasificación semántica de las fallas del inventario. */
sealed interface InventoryError {
    /** No hay conexión a internet (RNF-INV-05). */
    data object Network : InventoryError

    /** Las reglas de Firestore o de Storage rechazaron la operación. */
    data object PermissionDenied : InventoryError

    /** El producto ya no existe. */
    data object NotFound : InventoryError

    /** La foto no se pudo subir (CA-INV-05-06). */
    data object PhotoUploadFailed : InventoryError

    /** No hay sesión activa. */
    data object NotAuthenticated : InventoryError

    /**
     * Un campo del formulario no es válido (RF-INV-03).
     *
     * @property field Campo que se marca en rojo.
     * @property reason Motivo del error.
     */
    data class Validation(val field: InventoryField, val reason: Reason) : InventoryError {
        /** Motivo de un error de validación. */
        enum class Reason {
            /** El campo obligatorio está vacío. */
            EMPTY,

            /** El nombre tiene más de 50 caracteres. */
            TOO_LONG,

            /** La fecha nueva es anterior a hoy. */
            PAST_DATE,

            /** La cantidad no es un número, está fuera de rango o tiene más de 2 decimales (CA-INV-02-09). */
            INVALID,
        }
    }

    /** Error no clasificado. */
    data class Unknown(val cause: String?) : InventoryError
}

/**
 * Excepción portadora de un [InventoryError], usada como causa de un `Result.failure`.
 *
 * @property error Error de negocio que describe la falla.
 */
class InventoryException(val error: InventoryError) : Exception(error.toString())

/** Extrae el [InventoryError] de una excepción; si no es un [InventoryException], lo clasifica como [InventoryError.Unknown]. */
fun Throwable.toInventoryError(): InventoryError =
    (this as? InventoryException)?.error ?: InventoryError.Unknown(message)

/** Atajo para construir un `Result` fallido a partir de un [InventoryError]. */
fun <T> inventoryFailure(error: InventoryError): Result<T> = Result.failure(InventoryException(error))
