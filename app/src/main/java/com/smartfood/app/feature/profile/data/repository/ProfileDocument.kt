package com.smartfood.app.feature.profile.data.repository

import com.smartfood.app.feature.profile.domain.model.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Nombre: ProfileDocument.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Conversión entre [UserProfile] y el documento `users/{uid}` de Firestore (spec 008, plan §2.1). Se usa
 * `Map` para no depender de reflexión con R8, como en el registro y el inventario. Las funciones son puras para
 * probarlas sin Firebase.
 */
internal object ProfileDocument {
    const val FULL_NAME = "fullName"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val BIRTH_DATE = "birthDate"
    const val PHOTO_PATH = "photoPath"
    const val PHOTO_URL = "photoUrl"
    const val UPDATED_AT = "updatedAt"
}

/** Lee el perfil de un documento. Los campos opcionales mal formados se ignoran en lugar de romper la pantalla. */
internal fun profileFromDocument(uid: String, data: Map<String, Any?>): UserProfile = UserProfile(
    uid = uid,
    fullName = (data[ProfileDocument.FULL_NAME] as? String).orEmpty(),
    email = (data[ProfileDocument.EMAIL] as? String).orEmpty(),
    phone = (data[ProfileDocument.PHONE] as? String)?.takeIf { it.isNotBlank() },
    birthDate = (data[ProfileDocument.BIRTH_DATE] as? String)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    },
    // La ruta y la URL van juntas: con una sola, no hay foto.
    photoUrl = photoPair(data)?.second,
    photoPath = photoPair(data)?.first,
)

private fun photoPair(data: Map<String, Any?>): Pair<String, String>? {
    val path = data[ProfileDocument.PHOTO_PATH] as? String
    val url = data[ProfileDocument.PHOTO_URL] as? String
    return if (path.isNullOrBlank() || url.isNullOrBlank()) null else path to url
}

/**
 * Campos para `update` del perfil editado. Lo que se dejó vacío se borra con [delete] (`FieldValue.delete()`), y
 * `updatedAt` lleva [serverTime] (`FieldValue.serverTimestamp()`), como exigen las reglas. Nunca incluye el correo ni la
 * constancia de términos (RNF-PER-01).
 */
internal fun UserProfile.toUpdateFields(delete: Any, serverTime: Any): Map<String, Any> = buildMap {
    put(ProfileDocument.FULL_NAME, fullName)
    put(ProfileDocument.PHONE, phone ?: delete)
    put(ProfileDocument.BIRTH_DATE, birthDate?.toString() ?: delete)
    if (photoPath != null && photoUrl != null) {
        put(ProfileDocument.PHOTO_PATH, photoPath)
        put(ProfileDocument.PHOTO_URL, photoUrl)
    }
    put(ProfileDocument.UPDATED_AT, serverTime)
}
