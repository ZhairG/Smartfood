package com.smartfood.app.core.media

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: FirestoreMediaStore.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fotos guardadas como bytes en Firestore para el plan Spark, donde Storage no responde (spec 012,
 * RF-SPK-06). Cada foto es un documento `users/{uid}/media/{mediaId}` con `data` (JPEG de [MAX_BYTES] como máximo),
 * `contentType` y `createdAt`, por debajo del límite de 1 MiB por documento. Las reglas no permiten editar: una foto
 * nueva siempre tiene otro `mediaId` (RNF-SPK-03).
 * @param firestore instancia de Firestore.
 */
@Singleton
class FirestoreMediaStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun document(ref: MediaRef) = firestore.document(ref.path)

    /** Guarda [bytes] como JPEG. Falla si superan [MAX_BYTES]. */
    suspend fun write(ref: MediaRef, bytes: ByteArray) {
        require(bytes.size <= MAX_BYTES) { "La foto pesa ${bytes.size} bytes; el máximo es $MAX_BYTES" }
        document(ref).set(
            mapOf(
                DATA to Blob.fromBytes(bytes),
                CONTENT_TYPE to JPEG,
                CREATED_AT to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    /** Los bytes de la foto, o `null` si no existe. Sin red, Firestore responde desde su caché si la tiene. */
    suspend fun read(ref: MediaRef): ByteArray? =
        document(ref).get().await().getBlob(DATA)?.toBytes()

    /** Borra la foto. Si no existía, no hace nada (idempotente, como en Storage). */
    suspend fun delete(ref: MediaRef) {
        document(ref).delete().await()
    }

    companion object {
        /** Margen bajo el límite de 1 MiB por documento de Firestore; las reglas exigen lo mismo. */
        const val MAX_BYTES = 900 * 1024

        const val JPEG = "image/jpeg"
        private const val DATA = "data"
        private const val CONTENT_TYPE = "contentType"
        private const val CREATED_AT = "createdAt"
    }
}
