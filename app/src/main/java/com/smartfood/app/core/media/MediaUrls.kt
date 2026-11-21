package com.smartfood.app.core.media

/**
 * Nombre: MediaUrls.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Rutas y URLs de las fotos guardadas en Firestore en el modo gratuito (spec 012, RF-SPK-06). La ruta
 * `users/{uid}/media/{mediaId}` va en `photoPath` y la URL `smartfood-media://users/{uid}/media/{mediaId}` en
 * `photoUrl`, en lugar de la ruta y la URL de Storage. Así los modelos, los casos de uso y las pantallas no cambian, y
 * la URL dice de dónde leer la foto en cualquier modo (CA-SPK-05-06). Es Kotlin puro y se prueba en la JVM.
 * @param uid usuario dueño de la foto.
 * @param mediaId identificador del documento de la foto.
 */
data class MediaRef(val uid: String, val mediaId: String) {
    /** Ruta del documento en Firestore; es el `photoPath` de la foto. */
    val path: String get() = "$USERS/$uid/$MEDIA/$mediaId"

    /** URL que lee `MediaFetcher`; es el `photoUrl` de la foto. */
    val url: String get() = "$SCHEME://$path"

    companion object {
        const val SCHEME = "smartfood-media"
        private const val USERS = "users"
        private const val MEDIA = "media"
        private const val PREFIX = "$SCHEME://"

        private val PathPattern = Regex("^$USERS/([^/]+)/$MEDIA/([^/]+)$")

        /** La foto de [path], o `null` si no es una ruta de media (por ejemplo, una ruta de Storage). */
        fun fromPath(path: String): MediaRef? =
            PathPattern.matchEntire(path)?.let { MediaRef(it.groupValues[1], it.groupValues[2]) }

        /** La foto de [url], o `null` si no usa el esquema `smartfood-media`. */
        fun fromUrl(url: String): MediaRef? =
            if (url.startsWith(PREFIX)) fromPath(url.removePrefix(PREFIX)) else null

        /** `true` si [path] es de una foto guardada en Firestore. */
        fun isMediaPath(path: String): Boolean = fromPath(path) != null

        /**
         * Foto nueva de un producto. El `uuid` hace que nunca se sobrescriba otra: el flujo sube la nueva y después
         * borra la anterior por su ruta, igual que en Storage (CA-SPK-05-03).
         */
        fun inventoryPhoto(uid: String, itemId: String, uuid: String) = MediaRef(uid, "inv-$itemId-$uuid")

        /** Foto nueva del perfil (CA-SPK-05-02). */
        fun profilePhoto(uid: String, uuid: String) = MediaRef(uid, "profile-$uuid")
    }
}
