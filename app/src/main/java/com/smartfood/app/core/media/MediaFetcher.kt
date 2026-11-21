package com.smartfood.app.core.media

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

/**
 * Nombre: MediaFetcher.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Carga en Coil las fotos con URL `smartfood-media://…` (spec 012, RF-SPK-06): las lee de Firestore con
 * [FirestoreMediaStore] y las deja en la caché de la app, así no se vuelven a leer cada vez. Las URLs de Storage y los
 * archivos locales siguen pasando por los cargadores de siempre.
 * @param url URL completa, que da el nombre del archivo en caché.
 * @param ref foto que se lee.
 * @param store almacén de fotos en Firestore.
 * @param cacheDir carpeta de la caché.
 */
class MediaFetcher(
    private val url: String,
    private val ref: MediaRef,
    private val store: FirestoreMediaStore,
    private val cacheDir: File,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val file = File(cacheDir, sha256(url))
        if (!file.exists()) {
            // Si la foto no existe, Coil muestra el marcador sin foto y la app sigue (CA-SPK-05-05).
            val bytes = store.read(ref) ?: throw FileNotFoundException(url)
            withContext(Dispatchers.IO) {
                cacheDir.mkdirs()
                val partial = File(cacheDir, "${file.name}.tmp")
                partial.writeBytes(bytes)
                partial.renameTo(file)
            }
        }
        return SourceFetchResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = FirestoreMediaStore.JPEG,
            dataSource = DataSource.DISK,
        )
    }

    /** Crea un [MediaFetcher] solo para las URLs del esquema `smartfood-media`. */
    class Factory(private val store: FirestoreMediaStore, private val cacheDir: File) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != MediaRef.SCHEME) return null
            val url = data.toString()
            val ref = MediaRef.fromUrl(url) ?: return null
            return MediaFetcher(url, ref, store, cacheDir)
        }
    }

    private companion object {
        fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Nombre: SmartFoodImageLoaderFactory
 * Descripción: El `ImageLoader` de toda la app: el de siempre más [MediaFetcher] (spec 012). Lo registra
 * `SmartfoodApp`.
 * @param appContext contexto de la aplicación, para la carpeta de caché.
 * @param store almacén de fotos en Firestore.
 */
class SmartFoodImageLoaderFactory(
    private val appContext: Context,
    private val store: FirestoreMediaStore,
) : SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(MediaFetcher.Factory(store, File(appContext.cacheDir, CACHE_DIR))) }
            .build()

    private companion object {
        const val CACHE_DIR = "smartfood-media"
    }
}
