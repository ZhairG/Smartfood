package com.smartfood.app.feature.inventory.data.sync

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Nombre: PhotoSyncWorkersTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [PhotoUploadWorker], [PhotoDeleteWorker] y [PendingPhotoStore] (spec 006
 * §13, T048). Los workers se construyen con dobles de los repositorios para no tocar Firebase; las fotos son archivos
 * reales en `filesDir/pending`. Cubren la subida con su foto anterior (CA-INV-05-05), el reintento sin red y la foto
 * ilegible (CA-INV-05-06), el producto borrado mientras la foto esperaba y el borrado idempotente (CA-INV-10-03).
 */
@RunWith(AndroidJUnit4::class)
class PhotoSyncWorkersTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    // Carpeta aparte: no toca las fotos pendientes reales de la app en el teléfono.
    private val store = PendingPhotoStore(File(context.cacheDir, "pending-test"))
    private val auth = FakeAuth("uid")
    private val inventory = FakeInventory()
    private val photos = FakePhotos()
    private val photoSync = RecordingPhotoSync()

    @Before
    fun limpiarAntes() = store.clear()

    @After
    fun limpiarDespues() = store.clear()

    @Test
    fun caInv0505_subeLaFotoLaPoneEnElProductoYBorraLaAnterior() = runBlocking {
        val file = store.put("leche", jpeg())

        val result = upload(file, replacedPath = "users/uid/inventory/leche/vieja.jpg").doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("leche", inventory.photoSets.single().first)
        assertFalse("La foto del teléfono se borra al quedar en el producto", file.exists())
        assertTrue(store.pending.value.isEmpty())
        assertEquals(listOf("users/uid/inventory/leche/vieja.jpg"), photos.deleted)
    }

    @Test
    fun caInv0506_sinRedReintentaYConservaLaFoto() = runBlocking {
        val file = store.put("leche", jpeg())
        photos.uploadError = InventoryError.Network

        assertEquals(ListenableWorker.Result.retry(), upload(file).doWork())
        assertTrue(file.exists())
        assertEquals(file.path, store.pending.value["leche"])
    }

    @Test
    fun caInv0506_unaFotoIlegibleDejaElProductoSinFoto() = runBlocking {
        val roto = File(context.cacheDir, "rota.jpg").apply { writeText("no es una imagen") }
        val file = store.put("leche", roto)

        assertEquals(ListenableWorker.Result.success(), upload(file).doWork())
        assertEquals(0, photos.uploads)
        assertFalse(file.exists())
        assertTrue(store.pending.value.isEmpty())
    }

    @Test
    fun t048_siElProductoSeBorroLaFotoSubidaSeBorraDeLaNube() = runBlocking {
        val file = store.put("leche", jpeg())
        inventory.setPhotoError = InventoryError.NotFound

        assertEquals(ListenableWorker.Result.success(), upload(file).doWork())
        assertEquals(listOf(photos.lastUploaded!!.path), photos.deleted)
        assertFalse(file.exists())
    }

    @Test
    fun t048_siLasReglasNoDejanPonerLaFotoNoSeReintentaParaSiempre() = runBlocking {
        val file = store.put("leche", jpeg())
        inventory.setPhotoError = InventoryError.PermissionDenied

        assertEquals(ListenableWorker.Result.success(), upload(file).doWork())
        assertEquals(listOf(photos.lastUploaded!!.path), photos.deleted)
        assertFalse(file.exists())
    }

    @Test
    fun t048_siPonerLaFotoFallaPorRedSeReintentaSinDejarLaFotoHuerfana() = runBlocking {
        val file = store.put("leche", jpeg())
        inventory.setPhotoError = InventoryError.Network

        assertEquals(ListenableWorker.Result.retry(), upload(file).doWork())
        assertEquals(listOf(photos.lastUploaded!!.path), photos.deleted)
        assertTrue(file.exists())
    }

    @Test
    fun caInv0505_siLaAnteriorNoSeBorraSeEncolaSuBorrado() = runBlocking {
        val file = store.put("leche", jpeg())
        photos.deleteError = InventoryError.Network

        upload(file, replacedPath = "vieja.jpg").doWork()

        assertEquals(listOf("vieja.jpg"), photoSync.deletes)
    }

    @Test
    fun caInv1007_sinSesionNoSubeNada() = runBlocking {
        val file = store.put("leche", jpeg())
        auth.userId = null

        assertEquals(ListenableWorker.Result.success(), upload(file).doWork())
        assertEquals(0, photos.uploads)
    }

    @Test
    fun caInv1003_borrarLaFotoEnLaNubeReintentaSinRedYTerminaAlBorrarla() = runBlocking {
        photos.deleteError = InventoryError.Network
        assertEquals(ListenableWorker.Result.retry(), delete("users/uid/inventory/leche/a.jpg").doWork())

        photos.deleteError = null
        assertEquals(ListenableWorker.Result.success(), delete("users/uid/inventory/leche/a.jpg").doWork())
    }

    @Test
    fun t048_unaFotoNuevaReemplazaALaQueEsperabaDelMismoProducto() {
        val primera = store.put("leche", jpeg())
        val segunda = store.put("leche", jpeg())

        assertFalse(primera.exists())
        assertEquals(mapOf("leche" to segunda.path), store.pending.value)
        store.removeAll("leche")
        assertTrue(store.pending.value.isEmpty())
    }

    private fun upload(file: File, replacedPath: String? = null): PhotoUploadWorker =
        TestListenableWorkerBuilder<PhotoUploadWorker>(context)
            .setInputData(
                workDataOf(
                    PhotoUploadWorker.KEY_ITEM_ID to "leche",
                    PhotoUploadWorker.KEY_FILE_PATH to file.path,
                    PhotoUploadWorker.KEY_REPLACED_PATH to replacedPath
                )
            )
            .setWorkerFactory(factory)
            .build()

    private fun delete(path: String): PhotoDeleteWorker =
        TestListenableWorkerBuilder<PhotoDeleteWorker>(context)
            .setInputData(workDataOf(PhotoDeleteWorker.KEY_PATH to path))
            .setWorkerFactory(factory)
            .build()

    private val factory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? = when (workerClassName) {
            PhotoUploadWorker::class.java.name -> PhotoUploadWorker(
                appContext, workerParameters, auth, inventory, photos, photoSync, store, PhotoCompressor()
            )
            PhotoDeleteWorker::class.java.name -> PhotoDeleteWorker(appContext, workerParameters, auth, photos)
            else -> null
        }
    }

    /** Una foto JPEG real y pequeña, como la que deja la cámara en `filesDir/photos`. */
    private fun jpeg(): File {
        val file = File.createTempFile("foto", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        bitmap.recycle()
        return file
    }
}

private class FakeAuth(var userId: String?) : AuthRepository {
    override fun currentUserId(): String? = userId
    override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun createAccount(email: String, password: String): Result<String> = Result.success("uid")
    override suspend fun deleteCurrentAccount(): Result<Unit> = Result.success(Unit)
    override fun logout() = Unit
}

private class FakeInventory : InventoryRepository {
    var setPhotoError: InventoryError? = null
    val photoSets = mutableListOf<Pair<String, ItemPhoto?>>()

    override fun observeItems(): Flow<List<InventoryItem>> = flowOf(emptyList())
    override suspend fun getItem(id: String): Result<InventoryItem> =
        Result.success(InventoryItem(id, "Leche", FoodCategory.DAIRY, LocalDate.now()))
    override fun newItemId(): String = "nuevo"
    override suspend fun createItem(item: InventoryItem) = Result.success(WriteOutcome.SYNCED)
    override suspend fun updateItem(item: InventoryItem, photoChanged: Boolean) = Result.success(WriteOutcome.SYNCED)
    override suspend fun deleteItem(id: String) = Result.success(WriteOutcome.SYNCED)
    override suspend fun setPhoto(id: String, photo: ItemPhoto?): Result<Unit> {
        setPhotoError?.let { return Result.failure(InventoryException(it)) }
        photoSets += id to photo
        return Result.success(Unit)
    }
    override suspend fun hasPendingWrites(): Boolean = false
}

private class FakePhotos : PhotoRepository {
    var uploadError: InventoryError? = null
    var deleteError: InventoryError? = null
    var uploads = 0
    var lastUploaded: ItemPhoto? = null
    val deleted = mutableListOf<String>()

    override suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto> {
        uploadError?.let { return Result.failure(InventoryException(it)) }
        uploads++
        return Result.success(ItemPhoto("users/uid/inventory/$itemId/$uploads.jpg", "https://foto/$uploads"))
            .also { lastUploaded = it.getOrNull() }
    }

    override suspend fun delete(path: String): Result<Unit> {
        deleted += path
        return deleteError?.let { Result.failure(InventoryException(it)) } ?: Result.success(Unit)
    }
}

private class RecordingPhotoSync : PhotoSync {
    val deletes = mutableListOf<String>()
    override fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?) = Unit
    override fun enqueueDelete(path: String) {
        deletes += path
    }
    override fun cancelUpload(itemId: String) = Unit
    override fun pendingPhotos(): Flow<Map<String, String>> = emptyFlow()
    override fun hasPending(): Boolean = false
    override fun clearAll() = Unit
}
