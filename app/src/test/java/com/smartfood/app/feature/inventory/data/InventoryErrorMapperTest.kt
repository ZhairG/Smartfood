package com.smartfood.app.feature.inventory.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.storage.StorageException
import com.smartfood.app.feature.inventory.data.mapper.InventoryErrorMapper
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.testutil.inventoryError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: InventoryErrorMapperTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [InventoryErrorMapper] (spec 006, T016, plan §10).
 */
class InventoryErrorMapperTest {

    private fun firestore(code: Code) = FirebaseFirestoreException("falla $code", code)

    @Test
    fun `RNF-INV-05 sin red el error es Network`() {
        assertEquals(InventoryError.Network, InventoryErrorMapper.map(FirebaseNetworkException("sin red")))
    }

    @Test
    fun `CA-INV-02-07 Firestore no disponible o sin respuesta es Network`() {
        assertEquals(InventoryError.Network, InventoryErrorMapper.map(firestore(Code.UNAVAILABLE)))
        assertEquals(InventoryError.Network, InventoryErrorMapper.map(firestore(Code.DEADLINE_EXCEEDED)))
    }

    @Test
    fun `reglas de Firestore que niegan el acceso dan PermissionDenied`() {
        assertEquals(InventoryError.PermissionDenied, InventoryErrorMapper.map(firestore(Code.PERMISSION_DENIED)))
    }

    @Test
    fun `documento inexistente da NotFound y sin sesion da NotAuthenticated`() {
        assertEquals(InventoryError.NotFound, InventoryErrorMapper.map(firestore(Code.NOT_FOUND)))
        assertEquals(InventoryError.NotAuthenticated, InventoryErrorMapper.map(firestore(Code.UNAUTHENTICATED)))
    }

    @Test
    fun `otro codigo de Firestore da Unknown con el nombre del codigo`() {
        assertEquals(InventoryError.Unknown("ABORTED"), InventoryErrorMapper.map(firestore(Code.ABORTED)))
    }

    @Test
    fun `errores de Storage se clasifican por su codigo`() {
        assertEquals(InventoryError.Network, InventoryErrorMapper.mapStorageError(StorageException.ERROR_RETRY_LIMIT_EXCEEDED))
        assertEquals(InventoryError.PermissionDenied, InventoryErrorMapper.mapStorageError(StorageException.ERROR_NOT_AUTHORIZED))
        assertEquals(InventoryError.NotFound, InventoryErrorMapper.mapStorageError(StorageException.ERROR_OBJECT_NOT_FOUND))
    }

    @Test
    fun `CA-INV-05-06 cualquier otro error de Storage es PhotoUploadFailed`() {
        assertEquals(InventoryError.PhotoUploadFailed, InventoryErrorMapper.mapStorageError(StorageException.ERROR_UNKNOWN))
        assertEquals(InventoryError.PhotoUploadFailed, InventoryErrorMapper.mapStorageError(StorageException.ERROR_QUOTA_EXCEEDED))
    }

    @Test
    fun `un InventoryException conserva su error y lo desconocido es Unknown`() {
        val validation = InventoryError.Validation(InventoryField.NAME, InventoryError.Validation.Reason.EMPTY)
        assertEquals(validation, InventoryErrorMapper.map(InventoryException(validation)))
        assertEquals(InventoryError.Unknown("rara"), InventoryErrorMapper.map(IllegalStateException("rara")))
    }

    @Test
    fun `mapInventoryError clasifica el fallo y deja intacto el exito`() {
        val failure = Result.failure<Unit>(FirebaseNetworkException("sin red")).mapInventoryError()
        assertTrue(failure.exceptionOrNull() is InventoryException)
        assertEquals(InventoryError.Network, failure.inventoryError)

        assertEquals(Result.success(7), Result.success(7).mapInventoryError())
    }
}
