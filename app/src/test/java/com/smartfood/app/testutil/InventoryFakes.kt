package com.smartfood.app.testutil

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryPreferences
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.repository.SpeechToText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/**
 * Nombre: InventoryFakes.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementaciones fake de los contratos del dominio del inventario (spec 006, T006), compartidas
 * por las pruebas unitarias. Registran las llamadas recibidas y devuelven resultados configurables.
 */

/**
 * Registro ordenado de llamadas que pueden compartir varios fakes, para comprobar el orden entre repositorios
 * (por ejemplo, subir la foto antes de guardar el producto).
 */
class CallLog {
    val entries = mutableListOf<String>()

    fun record(entry: String) {
        entries += entry
    }
}

/** Construye un producto de prueba con valores por defecto. */
fun inventoryItem(
    id: String = "item-1",
    name: String = "Leche Entera",
    category: FoodCategory = FoodCategory.DAIRY,
    expiresOn: LocalDate = LocalDate.of(2026, 9, 10),
    quantity: Quantity? = null,
    photo: ItemPhoto? = null,
) = InventoryItem(id, name, category, expiresOn, quantity, photo)

/**
 * Fake de [InventoryRepository] que guarda los productos en un `MutableStateFlow`, así cada cambio se emite como
 * en Firestore. Por defecto todas las operaciones tienen éxito y el servidor las confirma ([WriteOutcome.SYNCED]).
 */
class FakeInventoryRepository(private val log: CallLog = CallLog()) : InventoryRepository {
    val items = MutableStateFlow<List<InventoryItem>>(emptyList())

    /** Si no es `null`, [createItem] y [updateItem] devuelven este fallo sin guardar nada. */
    var saveResult: Result<Unit>? = null
    /** Si no es `null`, [deleteItem] devuelve este fallo sin borrar nada. */
    var deleteResult: Result<Unit>? = null
    /** Si no es `null`, [setPhoto] devuelve este resultado sin cambiar nada. */
    var setPhotoResult: Result<Unit>? = null
    /** Cómo terminan las escrituras: `PENDING` simula que no hay conexión (CA-INV-10-02). */
    var outcome: WriteOutcome = WriteOutcome.SYNCED
    /** Lo que devuelve [hasPendingWrites] (CA-INV-10-07). */
    var pendingWrites: Boolean = false
    /** Si no es `null`, las escrituras esperan a que se complete, para ver el estado mientras guardan. */
    var gate: CompletableDeferred<Unit>? = null

    val savedItems = mutableListOf<InventoryItem>()
    /** `photoChanged` de cada [updateItem], en orden. */
    val photoChangedFlags = mutableListOf<Boolean>()
    val deletedIds = mutableListOf<String>()
    val photoSets = mutableListOf<Pair<String, ItemPhoto?>>()
    private var nextId = 0

    override fun observeItems(): Flow<List<InventoryItem>> = items

    override suspend fun getItem(id: String): Result<InventoryItem> =
        items.value.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: inventoryFailure(InventoryError.NotFound)

    override fun newItemId(): String {
        nextId++
        log.record("newItemId")
        return "item-new-$nextId"
    }

    override suspend fun createItem(item: InventoryItem): Result<WriteOutcome> {
        log.record("createItem:${item.id}")
        return write(item)
    }

    override suspend fun updateItem(item: InventoryItem, photoChanged: Boolean): Result<WriteOutcome> {
        log.record("updateItem:${item.id}")
        photoChangedFlags += photoChanged
        // Como en Firestore, sin photoChanged se conserva la foto que ya tenía el documento.
        val stored = items.value.firstOrNull { it.id == item.id }
        return write(if (photoChanged) item else item.copy(photo = stored?.photo ?: item.photo))
    }

    private suspend fun write(item: InventoryItem): Result<WriteOutcome> {
        gate?.await()
        saveResult?.let { result -> return result.map { outcome } }
        savedItems += item
        items.value = items.value.filterNot { it.id == item.id } + item
        return Result.success(outcome)
    }

    override suspend fun deleteItem(id: String): Result<WriteOutcome> {
        log.record("deleteItem:$id")
        gate?.await()
        deleteResult?.let { result -> return result.map { outcome } }
        deletedIds += id
        items.value = items.value.filterNot { it.id == id }
        return Result.success(outcome)
    }

    override suspend fun setPhoto(id: String, photo: ItemPhoto?): Result<Unit> {
        log.record("setPhoto:$id")
        setPhotoResult?.let { return it }
        val stored = items.value.firstOrNull { it.id == id } ?: return inventoryFailure(InventoryError.NotFound)
        photoSets += id to photo
        items.value = items.value.map { if (it.id == id) stored.copy(photo = photo) else it }
        return Result.success(Unit)
    }

    override suspend fun hasPendingWrites(): Boolean = pendingWrites
}

/**
 * Fake de [PhotoSync] que registra lo que se encola y publica las fotos pendientes en un `MutableStateFlow`
 * (spec 006 §13).
 */
class FakePhotoSync(private val log: CallLog = CallLog()) : PhotoSync {
    /** Subidas encoladas: producto, foto y la foto de la nube que reemplaza. */
    val uploads = mutableListOf<Triple<String, LocalPhoto, String?>>()
    val deletes = mutableListOf<String>()
    val cancelled = mutableListOf<String>()
    var clearAllCalls = 0
    val pending = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?) {
        log.record("enqueueUpload:$itemId")
        uploads += Triple(itemId, photo, replacedPath)
        pending.value = pending.value + (itemId to photo.filePath)
    }

    override fun enqueueDelete(path: String) {
        log.record("enqueueDelete:$path")
        deletes += path
    }

    override fun cancelUpload(itemId: String) {
        log.record("cancelUpload:$itemId")
        cancelled += itemId
        pending.value = pending.value - itemId
    }

    override fun pendingPhotos(): Flow<Map<String, String>> = pending

    override fun hasPending(): Boolean = pending.value.isNotEmpty()

    override fun clearAll() {
        log.record("clearAll")
        clearAllCalls++
        pending.value = emptyMap()
    }
}

/** Fake de [PhotoRepository]. Cada subida exitosa devuelve una ruta única dentro del producto. */
class FakePhotoRepository(private val log: CallLog = CallLog()) : PhotoRepository {
    /** Si no es `null`, [upload] devuelve este resultado. */
    var uploadResult: Result<ItemPhoto>? = null
    /** Si no es `null`, [delete] devuelve este resultado (el borrado se registra igual). */
    var deleteResult: Result<Unit>? = null

    val uploads = mutableListOf<Pair<String, LocalPhoto>>()
    val deletedPaths = mutableListOf<String>()
    private var uploadCount = 0

    override suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto> {
        log.record("upload:$itemId")
        uploadResult?.let { return it }
        uploadCount++
        uploads += itemId to photo
        val path = "users/$DEFAULT_UID/inventory/$itemId/foto-$uploadCount.jpg"
        return Result.success(ItemPhoto(path, "https://fotos.test/$path"))
    }

    override suspend fun delete(path: String): Result<Unit> {
        log.record("deletePhoto:$path")
        deletedPaths += path
        return deleteResult ?: Result.success(Unit)
    }

    private companion object {
        const val DEFAULT_UID = FakeAuthRepository.DEFAULT_UID
    }
}

/**
 * Fake de [SpeechToText]. Las pruebas envían los eventos con [send]; la escucha termina con un `Result` o un
 * `Failed`, o cuando se cancela la recolección.
 */
class FakeSpeechToText(var available: Boolean = true) : SpeechToText {
    private val events = Channel<SpeechEvent>(Channel.UNLIMITED)

    var listenCalls = 0
    /** Veces que se dejó de recolectar una escucha antes de su resultado (CA-INV-07-07). */
    var cancellations = 0
    var isListening = false
        private set

    override fun isAvailable(): Boolean = available

    override fun listen(): Flow<SpeechEvent> = flow {
        listenCalls++
        isListening = true
        try {
            for (event in events) {
                emit(event)
                if (event is SpeechEvent.Result || event is SpeechEvent.Failed) break
            }
        } catch (e: CancellationException) {
            cancellations++
            throw e
        } finally {
            isListening = false
        }
    }

    /** Entrega un evento a la escucha activa o a la siguiente. */
    fun send(event: SpeechEvent) {
        events.trySend(event)
    }
}

/** Fake de [ExpiryReminderScheduler] que cuenta las llamadas. */
class FakeExpiryReminderScheduler(private val log: CallLog = CallLog()) : ExpiryReminderScheduler {
    var scheduleCalls = 0
    var cancelCalls = 0

    override fun schedule() {
        scheduleCalls++
        log.record("schedule")
    }

    override fun cancel() {
        cancelCalls++
        log.record("cancel")
    }
}

/** Fake de [InventoryPreferences] en memoria. */
class FakeInventoryPreferences(var notificationPermissionRequested: Boolean = false) : InventoryPreferences {
    override suspend fun wasNotificationPermissionRequested(): Boolean = notificationPermissionRequested

    override suspend fun markNotificationPermissionRequested() {
        notificationPermissionRequested = true
    }
}
