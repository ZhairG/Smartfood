# Plan técnico 006: Inventario de alimentos

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md).

## 1. Arquitectura

```
feature/inventory/
├── domain/                                   Kotlin puro (java.time permitido)
│   ├── model/
│   │   ├── InventoryItem.kt                  InventoryItem, ItemPhoto, FoodCategory, ExpiryStatus, Quantity,
│   │   │                                     QuantityUnit, InventorySection, ItemDraft, ValidItem, LocalPhoto
│   │   ├── InventoryError.kt                 InventoryError (sealed), InventoryException, toInventoryError()
│   │   ├── InventoryField.kt                 NAME, EXPIRY, CATEGORY, QUANTITY
│   │   └── Speech.kt                         SpeechEvent, SpeechError
│   ├── repository/
│   │   ├── InventoryRepository.kt
│   │   ├── PhotoRepository.kt                LocalPhoto (ruta de archivo local) y operaciones en la nube
│   │   ├── PhotoSync.kt                      subida y borrado de fotos en segundo plano (§13)
│   │   ├── SpeechToText.kt
│   │   └── ExpiryReminderScheduler.kt
│   └── usecase/
│       ├── ClassifyExpiryUseCase.kt          umbral de 3 días; recibe un Clock
│       ├── ObserveInventoryUseCase.kt        agrupa por estado y ordena (CA-INV-01-01, 01-04)
│       ├── ValidateItemUseCase.kt            reglas del formulario (único lugar)
│       ├── SaveItemUseCases.kt               AddItemUseCase, UpdateItemUseCase (documento + foto encolada, §13)
│       ├── DeleteItemUseCase.kt
│       ├── HasPendingChangesUseCase.kt       cambios o fotos sin subir, para el aviso al cerrar sesión (§13)
│       ├── SearchInventoryUseCase.kt         normaliza, divide por comas, OR + categoría, plural simple
│       ├── ParseSpokenIngredientsUseCase.kt  dictado → lista de ingredientes
│       └── GetExpiringItemsUseCase.kt        para el aviso diario
├── data/
│   ├── repository/FirestoreInventoryRepository.kt
│   ├── repository/FirebaseStoragePhotoRepository.kt
│   ├── speech/AndroidSpeechToText.kt
│   ├── reminder/WorkManagerExpiryReminderScheduler.kt
│   ├── reminder/ExpiryReminderWorker.kt
│   ├── reminder/ExpiryNotifier.kt            canal y construcción de la notificación
│   ├── sync/PendingPhotoStore.kt             fotos que esperan subirse, en filesDir/pending/{itemId}/{uuid}.jpg (§13)
│   ├── sync/WorkManagerPhotoSync.kt          implementación de PhotoSync
│   ├── sync/PhotoUploadWorker.kt             sube la foto y actualiza el producto (@HiltWorker)
│   ├── sync/PhotoDeleteWorker.kt             borra una foto de Storage cuando hay red (@HiltWorker)
│   ├── mapper/InventoryErrorMapper.kt        único lugar que conoce excepciones de Firebase en esta feature
│   └── di/InventoryDataModule.kt
└── ui/
    ├── InventoryNavGraph.kt                  inventoryDestinations(navController)
    ├── InventoryErrorText.kt                 único mapeo InventoryError / SpeechError → strings
    ├── list/        InventoryContract.kt, InventoryViewModel.kt, InventoryScreen.kt
    ├── search/      InventorySearchContract.kt, InventorySearchViewModel.kt, InventorySearchScreen.kt
    ├── editor/      ProductEditorContract.kt, ProductEditorViewModel.kt, ProductEditorScreen.kt
    ├── camera/      ProductCameraScreen.kt (visor CameraX; sin ViewModel), CameraCapture.kt
    └── components/  InventoryItemRow, ExpirySectionHeader, CategoryChips, CategorySheet, PhotoSlot,
                     VoiceSearchCard, VoiceSearchOverlay, PermissionRationale

feature/profile/                              hoy feature/home: HomeScreen → ProfileScreen (sin cambios de lógica)
core/navigation/                              rutas nuevas, MainGraph con barra inferior
core/designsystem/components/                 SmartFoodSelectorField, SmartFoodBottomBar, SmartFoodConfirmDialog,
                                              SmartFoodResultDialog, SmartFoodTopBar (barra de las pantallas internas,
                                              Figma `476:5960`); SmartFoodTextField ampliado
res/values/strings_inventory.xml              todos los textos de la feature
```

- **`java.time` en el dominio.** `LocalDate` y `Clock` son Kotlin/JVM puro, no Android. Como `minSdk` es 24, se
  activa `coreLibraryDesugaring` (ver §9).
- **Una sola pantalla para añadir y editar** (`ProductEditorScreen`): el modo sale de `itemId == null`. La barra
  superior cambia el título y la acción ("Guardar" solo en la edición), y el botón inferior es "Añadir al Inventario"
  o "Eliminar".
- **La cámara no tiene ViewModel.** Solo captura un archivo y lo devuelve; la decisión de guardarlo es del editor.
- **Lista y búsqueda comparten datos:** las dos observan `ObserveInventoryUseCase`. La búsqueda deriva su lista con
  `combine(items, query, category)` y `SearchInventoryUseCase`.

## 2. Contratos

### Dominio

```kotlin
enum class FoodCategory { FRUITS, DAIRY, PROTEINS, VEGETABLES, GRAINS, OTHER }   // código estable en la nube
enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, FRESH }
enum class QuantityUnit { UNITS, GRAMS, KILOGRAMS, POUNDS, MILLILITERS, LITERS }   // código estable en la nube

/** Cantidad con su unidad: al ser un solo valor opcional, "van juntas" queda en el tipo (T002). */
data class Quantity(val amount: Double, val unit: QuantityUnit)

data class ItemPhoto(val path: String, val url: String)
data class InventoryItem(
    val id: String,
    val name: String,
    val category: FoodCategory,
    val expiresOn: LocalDate,
    val quantity: Quantity? = null,
    val photo: ItemPhoto? = null,
    val localPhotoPath: String? = null,   // foto que espera subirse (CA-INV-10-04, T046)
    val isPendingSync: Boolean = false,   // cambios o foto sin subir (CA-INV-10-02, T046)
    val price: Long? = null,              // pesos colombianos enteros, opcional (CA-INV-02-10, T052)
)

/** Cómo terminó una escritura: confirmada por el servidor o guardada en el teléfono (§13, T046). */
enum class WriteOutcome { SYNCED, PENDING }
data class SavedItem(val item: InventoryItem, val outcome: WriteOutcome)
data class InventorySection(val status: ExpiryStatus, val items: List<InventoryItem>)

/** Borrador del formulario: lo que valida ValidateItemUseCase. La cantidad llega como texto, tal como se escribió. */
data class ItemDraft(
    val name: String,
    val expiresOn: LocalDate?,
    val category: FoodCategory?,
    val quantityText: String = "",
    val unit: QuantityUnit? = null,
    val priceText: String = "",          // "12500" o "12.500"; vacío = sin precio (T052)
)

/** Resultado de ValidateItemUseCase: nombre recortado, campos obligatorios presentes y cantidad interpretada. */
data class ValidItem(
    val name: String, val expiresOn: LocalDate, val category: FoodCategory, val quantity: Quantity?, val price: Long? = null,
)

@JvmInline value class LocalPhoto(val filePath: String)

sealed interface InventoryError {
    data object Network : InventoryError
    data object PermissionDenied : InventoryError       // reglas de Firestore o Storage
    data object NotFound : InventoryError
    data object PhotoUploadFailed : InventoryError
    data object NotAuthenticated : InventoryError
    data class Validation(val field: InventoryField, val reason: Reason) : InventoryError {
        enum class Reason { EMPTY, TOO_LONG, PAST_DATE, INVALID }   // INVALID: cantidad fuera de rango o no numérica
    }
    data class Unknown(val cause: String?) : InventoryError
}

sealed interface SpeechEvent {
    data object Ready : SpeechEvent                       // "Habla ahora"
    data object Processing : SpeechEvent                  // "…"
    data class Result(val alternatives: List<String>) : SpeechEvent
    data class Failed(val error: SpeechError) : SpeechEvent
}
enum class SpeechError { NO_MATCH, NOT_AVAILABLE, PERMISSION_DENIED, NETWORK }
```

| Interfaz | Firma |
|---|---|
| `InventoryRepository` | `observeItems(): Flow<List<InventoryItem>>` (con `isPendingSync`), `getItem(id): Result<InventoryItem>`, `newItemId(): String`, `createItem(item): Result<WriteOutcome>`, `updateItem(item, photoChanged: Boolean): Result<WriteOutcome>`, `deleteItem(id): Result<WriteOutcome>`, `setPhoto(id, photo: ItemPhoto?): Result<Unit>` (lo usa el worker) y `hasPendingWrites(): Boolean`. Reemplazan a `saveItem(item): Result<Unit>` (§13, T046 y T047). |
| `PhotoRepository` | `upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto>`, `delete(path: String): Result<Unit>`. Desde el #71 solo la usan los workers. |
| `PhotoSync` (§13) | `enqueueUpload(itemId, photo: LocalPhoto, replacedPath: String?)`, `enqueueDelete(path)`, `cancelUpload(itemId)`, `pendingPhotos(): Flow<Map<String, String>>` (id → ruta local), `hasPending(): Boolean` y `clearAll()` |
| `SpeechToText` | `isAvailable(): Boolean`, `listen(): Flow<SpeechEvent>` (terminar de recolectar cancela la escucha) |
| `ExpiryReminderScheduler` | `schedule()`, `cancel()` |

| UseCase | Responsabilidad |
|---|---|
| `ClassifyExpiryUseCase(clock)` | `invoke(expiresOn): ExpiryStatus`. Días = `ChronoUnit.DAYS.between(hoy, expiresOn)`; < 0 vencido, 0..3 próximo, > 3 fresco (CA-INV-01-06). |
| `ObserveInventoryUseCase` | `Flow<List<InventorySection>>` en orden EXPIRED, EXPIRING_SOON, FRESH, sin secciones vacías, ordenado por fecha y nombre. Combina `observeItems()` con `PhotoSync.pendingPhotos()`: un producto con foto pendiente lleva `localPhotoPath` e `isPendingSync = true` (CA-INV-10-04, T046). |
| `ValidateItemUseCase(clock)` | `invoke(draft, originalExpiresOn: LocalDate?): Result<ValidItem>`. El nombre recortado debe tener entre 1 y 50 caracteres; la fecha y la categoría son obligatorias; la fecha no puede ser anterior a hoy, salvo si es igual a `originalExpiresOn` (CA-INV-03-03). Cantidad: el texto recortado vacío da `quantity = null` (la unidad se descarta); si no, se cambia la coma por punto y debe ser un número > 0, ≤ 9999 y con 2 decimales como máximo, o falla con `Validation(QUANTITY, INVALID)` (CA-INV-02-09). Sin unidad se usa `UNITS` (CA-INV-02-08). Además expone `isComplete(draft)`, que solo mira nombre, fecha y categoría, para habilitar el botón (CA-INV-02-02). |
| `AddItemUseCase` | `invoke(draft, photo: LocalPhoto?): Result<SavedItem>`, con `SavedItem(item, outcome: WriteOutcome)`. Valida → `newItemId()` → `createItem` → si hay foto, `PhotoSync.enqueueUpload(id, photo, replacedPath = null)`. La foto ya no se sube aquí (§13, CA-INV-05-06). Con éxito llama a `schedule()`, que es idempotente. |
| `UpdateItemUseCase` | `invoke(original, draft, photoEdit: PhotoEdit): Result<SavedItem>`, con `PhotoEdit` = `Keep`, `Remove` o `Replace(LocalPhoto)`. Valida → `updateItem(item, photoChanged = photoEdit == Remove)`. Con `Keep` y `Replace`, el documento conserva los campos de la foto. Con `Replace`, `enqueueUpload(id, foto, replacedPath = original.photo?.path)`: el worker pone la nueva con `setPhoto` y después borra la anterior (CA-INV-05-05); mientras tanto el producto muestra la anterior. Con `Remove`, `cancelUpload(id)` y `enqueueDelete(original.photo.path)` si había (CA-INV-05-04). |
| `DeleteItemUseCase` | `invoke(item): Result<WriteOutcome>`: `deleteItem` → `cancelUpload(item.id)` → `enqueueDelete(path)` si tenía foto en la nube (CA-INV-04-02, CA-INV-10-03). |
| `HasPendingChangesUseCase` | `invoke(): Boolean` = `InventoryRepository.hasPendingWrites() \|\| PhotoSync.hasPending()` (CA-INV-10-07, T046). `LogoutUseCase` llama además a `PhotoSync.clearAll()`. |
| `SearchInventoryUseCase` | `invoke(items, query, category?): List<InventoryItem>`. Normaliza (minúsculas y sin tildes con `java.text.Normalizer`), divide por comas, descarta los términos vacíos y aplica OR por "contiene". Cada término coincide también sin la `s` o `es` final (CA-INV-07-04). Orden por fecha. |
| `ParseSpokenIngredientsUseCase` | `invoke(alternatives): List<String>`. Toma la primera alternativa no vacía, la divide por comas y por las palabras " y " y " e ", recorta, quita los artículos iniciales ("el", "la", "los", "las", "un", "una", "unos", "unas") y los duplicados. "carne pollo y leche" → `[carne pollo, leche]`, porque sin pausa ni conector no se puede separar con seguridad. El reconocedor suele insertar las comas en las pausas. |
| `GetExpiringItemsUseCase` | `invoke(items): List<InventoryItem>`. Los productos en estado `EXPIRING_SOON`, ordenados por fecha y nombre (CA-INV-08-01, 02). El worker se lo aplica a la primera emisión del repositorio. |

### UI (contratos de pantalla)

| Pantalla | UiState (resumen) | Eventos principales | Efectos |
|---|---|---|---|
| `InventoryScreen` | `isLoading`, `sections`, `error` | `AddClicked`, `SearchClicked`, `EditClicked(id)`, `RetryClicked` | `NavigateToEditor(id?)`, `NavigateToSearch` |
| `InventorySearchScreen` | `query`, `selectedCategory`, `results`, `isVoiceAvailable`, `voice: VoiceUiState {Hidden, Listening, Processing, Failed(error)}` | `QueryChanged`, `ClearQuery`, `CategorySelected(c?)`, `VoiceClicked`, `VoicePermissionResult(granted)`, `VoiceRetry`, `VoiceDismissed`, `ResultClicked(id)` | `RequestMicPermission`, `NavigateToEditor(id)`, `ShowPermissionRationale` |
| `ProductEditorScreen` | `mode {Add, Edit}`, `name`, `expiresOn`, `category`, `quantityText`, `unit`, `photo: PhotoUiState {None, Local(path), Remote(url)}`, `isSaveEnabled`, `isSaving`, `error`, `dialog: EditorDialog?` | `NameChanged`, `ExpiryPicked`, `CategoryPicked`, `QuantityChanged`, `UnitPicked`, `CameraClicked`, `PhotoCaptured(path)`, `RemovePhotoClicked`, `RemovePhotoConfirmed`, `SaveClicked`, `DeleteClicked`, `DeleteConfirmed`, `DialogDismissed` | `RequestCameraPermission`, `NavigateToCamera`, `RequestNotificationPermission`, `NavigateBack` |
| `ProfileScreen` (§13) | lo actual + `dialog: ProfileDialog?` (`ConfirmLogoutWithPending`) | `LogoutClicked`, `LogoutConfirmed`, `DialogDismissed` | `NavigateToLogin` |

- **Sin conexión (§13):** con `WriteOutcome.PENDING`, el editor muestra el diálogo de resultado "Guardado en tu
  teléfono" en lugar de "Producto Guardado" o "Guardado" (CA-INV-10-02). La fila de la lista muestra el ícono de
  pendiente si `isPendingSync`, y la miniatura y la zona de foto usan `localPhotoPath` antes que `photo.url`
  (CA-INV-10-04).

- Los campos en rojo se **derivan** de `error` (`InventoryError.Validation.field`), como en autenticación.
- `isSaveEnabled` viene de `ValidateItemUseCase.isComplete`; el ViewModel no repite reglas.
- Los diálogos de resultado ("Producto Guardado", "Guardado", "Producto eliminado", "Imagen eliminada") son parte
  del estado. Al cerrarse, o tras 2 s, emiten `NavigateBack` cuando corresponde.
- **Permisos:** la pantalla lanza `rememberLauncherForActivityResult(RequestPermission)` al recibir el efecto y
  devuelve el resultado como evento. Comprobar si ya está concedido (`ContextCompat.checkSelfPermission`) es un
  detalle de la UI.

## 3. Modelo de datos y reglas

### Firestore

```
users/{uid}/inventory/{itemId}
  ├── name            : string     1..50 caracteres, recortado
  ├── nameNormalized  : string     minúsculas sin tildes (para futuras consultas en el servidor)
  ├── category        : string     FRUITS | DAIRY | PROTEINS | VEGETABLES | GRAINS | OTHER
  ├── expiresOn       : string     "yyyy-MM-dd" (día sin hora; ADR-INV-02)
  ├── quantity        : number?    > 0 y ≤ 9999; presente solo junto con unit (T002)
  ├── unit            : string?    UNITS | GRAMS | KILOGRAMS | POUNDS | MILLILITERS | LITERS
  ├── price           : int?       pesos colombianos, > 0 y ≤ 99999999 (T052)
  ├── photoPath       : string?    ruta en Storage
  ├── photoUrl        : string?    URL de descarga
  ├── createdAt       : timestamp  serverTimestamp (solo al crear)
  └── updatedAt       : timestamp  serverTimestamp
```

Se escribe y se lee con `Map` (sin reflexión, por R8), igual que `FirestoreUserRepository`. Sin cantidad, las claves
`quantity` y `unit` no se escriben; sin precio, `price` tampoco. El precio (T052, 16/10) fue la primera clave opcional
que se agregó después: los documentos viejos siguen siendo válidos.

**Escritura (desde el #71, T047).** Hasta el #71, guardar y borrar usaban transacciones (`runTransaction`) para
fallar con `Network` sin conexión, como pedía la versión anterior de RNF-INV-05. Ahora se escribe en la cola de
Firestore, que funciona sin red (§13):
- **Crear:** `set` con todos los campos, y `createdAt` y `updatedAt` con `FieldValue.serverTimestamp()`.
- **Editar:** `update` con los campos del producto y `updatedAt`. La cantidad que se quitó se borra con
  `FieldValue.delete()`, y `createdAt` no se toca, como piden las reglas. Los campos de la foto solo se escriben si
  `photoChanged`, es decir, con `Remove`, que los borra; así no se pisa lo que escribió `PhotoUploadWorker`.
- **Poner la foto (`setPhoto`, lo usa el worker con conexión):** primero comprueba en el servidor que el producto
  exista. Las reglas rechazan un `update` sobre un documento que no existe con `PERMISSION_DENIED`, no con
  `NOT_FOUND` (lo mostró la prueba con el emulador); sin esa comprobación, el worker reintentaría para siempre.
- **Borrar:** `delete`.
- **Espera:** cada escritura espera la confirmación del servidor como mucho 3 s
  (`withTimeoutOrNull { task.await() }`). Si llega, es `SYNCED`; si no, es `PENDING`, y la escritura sigue en la cola
  aunque la corrutina deje de esperar. Un error que llega antes (por ejemplo, `PERMISSION_DENIED`) se devuelve como
  fallo (CA-INV-02-07).
- Las reglas no cambian: `serverTimestamp` se resuelve cuando la escritura llega al servidor, así que
  `createdAt == request.time` y `updatedAt == request.time` se siguen cumpliendo al sincronizar.

La lectura es tolerante: una categoría desconocida pasa a `OTHER`,
una cantidad o unidad inválida se descarta y un documento sin nombre o sin fecha válida se omite de la lista.

Reglas (`firestore.rules`):

```
match /users/{uid}/inventory/{itemId} {
  function validItem(d) {
    return d.name is string && d.name.size() > 0 && d.name.size() <= 50
      && d.category in ['FRUITS','DAIRY','PROTEINS','VEGETABLES','GRAINS','OTHER']
      && d.expiresOn is string && d.expiresOn.matches('^\\d{4}-\\d{2}-\\d{2}$')
      && validQuantity(d)
      && d.updatedAt == request.time;
  }
  // Cantidad y unidad van juntas: las dos ausentes o las dos válidas (T002).
  function validQuantity(d) {
    return (!('quantity' in d) && !('unit' in d))
      || (d.quantity is number && d.quantity > 0 && d.quantity <= 9999
          && d.unit in ['UNITS','GRAMS','KILOGRAMS','POUNDS','MILLILITERS','LITERS']);
  }
  allow read, delete: if isOwner(uid);
  allow create: if isOwner(uid) && validItem(request.resource.data) && request.resource.data.createdAt == request.time;
  allow update: if isOwner(uid) && validItem(request.resource.data)
    && request.resource.data.createdAt == resource.data.createdAt;
}
```

La regla actual de `users/{uid}` no cubre las subcolecciones, así que esta regla es necesaria.

La versión vigente (`firestore.rules`, T022 y T052) endurece este esquema: `keys().hasOnly([...])` con los 11 campos del
documento (con `price`, validado por `validPrice`: ausente, o `int` > 0 y ≤ 99999999) (no se cuelan claves extra), `nameNormalized` debe ser texto y `validPhoto` exige que `photoPath` y
`photoUrl` vayan juntas. Las pruebas automáticas viven en `rules-test/` (`@firebase/rules-unit-testing`) y se corren
con `firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"`.

### Cloud Storage

```
users/{uid}/inventory/{itemId}/{uuid}.jpg
```

Hay un archivo nuevo `storage.rules`, registrado en `firebase.json`:

```
match /b/{bucket}/o/users/{uid}/inventory/{itemId}/{file} {
  allow read, delete: if request.auth != null && request.auth.uid == uid;
  allow create: if request.auth != null && request.auth.uid == uid
    && request.resource.size < 2 * 1024 * 1024
    && request.resource.contentType == 'image/jpeg';
}
```

- Un nombre de archivo único (`uuid`) evita que la caché de imágenes muestre la foto vieja al reemplazarla.
- En Storage, sobrescribir un archivo cuenta como `create`, así que la regla vigente exige además `resource == null`:
  una foto subida no se puede reemplazar, solo borrar (lo comprobó `rules-test`).
- El bucket se crea en `southamerica-east1`, como Firestore. El plan Blaze ya está activo.

## 4. Foto: captura, compresión y subida

1. `ProductCameraScreen` usa `LifecycleCameraController` + `PreviewView` (`AndroidView`), con el recuadro de
   esquinas del Figma dibujado en Compose encima.
2. La captura se guarda con `takePicture(OutputFileOptions)` en `filesDir/photos/{uuid}.jpg` (`takePhoto`, #41). No
   va en `cacheDir` porque Android puede borrarla sola mientras espera a subirse (#71); el repositorio la borra al
   subirla. El permiso se pide al abrir el visor y, si se niega, la misma pantalla explica para qué se usa y ofrece
   abrir los ajustes (CA-INV-05-03).
3. La ruta vuelve al editor con `navController.previousBackStackEntry?.savedStateHandle[CAPTURED_PHOTO_KEY]` (`"captured_photo"`), y el
   editor la recibe como `PhotoCaptured(path)`.
4. `FirebaseStoragePhotoRepository.upload`:
   - decodifica con `inSampleSize`;
   - respeta la orientación EXIF;
   - escala a 1080 px en el lado mayor;
   - comprime en JPEG con calidad 80 (RNF-INV-02);
   - sube con `putBytes` y `contentType = image/jpeg`;
   - devuelve `ItemPhoto(path, downloadUrl)`.
5. **Desde el #71, `upload` solo lo llama `PhotoUploadWorker`** (§13): el producto se guarda primero y la foto se
   sube en segundo plano. El archivo temporal se borra después de subirlo o al descartar el formulario. El repositorio lo borra **solo si la
   subida sale bien**: si falla, el archivo se conserva y el worker reintenta (CA-INV-05-06). Una foto ilegible da
   `PhotoUploadFailed`. `delete(path)` es idempotente: una foto que ya no existe cuenta como borrada. Los cálculos
   (muestreo, tamaño final y orientación EXIF) viven en `data/photo/PhotoScaling.kt`, sin Android, y se prueban en la
   JVM; `PhotoCompressor` los aplica con `BitmapFactory`, `ExifInterface` y una sola `Matrix`.
6. Las miniaturas y la zona de foto cargan `photoUrl` con Coil 3 (`AsyncImage`), con el color del estado como
   marcador (CA-INV-01-03). Si hay `localPhotoPath`, cargan ese archivo (CA-INV-10-04).

## 5. Voz

- `AndroidSpeechToText` envuelve `SpeechRecognizer` en un `callbackFlow`, que se crea y se usa en el hilo principal:
  - `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, `EXTRA_LANGUAGE = "es-CO"`, `EXTRA_PARTIAL_RESULTS = true` y
    `EXTRA_MAX_RESULTS = 3`;
  - `onReadyForSpeech` → `Ready`; `onEndOfSpeech` → `Processing`;
  - `onResults` → `Result`;
  - `ERROR_NO_MATCH` y `ERROR_SPEECH_TIMEOUT` → `NO_MATCH`; `ERROR_INSUFFICIENT_PERMISSIONS` →
    `PERMISSION_DENIED`; los errores de red → `NETWORK`.
  - `awaitClose` llama a `cancel()` y `destroy()` (CA-INV-07-07).
- `isAvailable()` = `SpeechRecognizer.isRecognitionAvailable(context)`. En el manifiesto:
  - `<queries><intent><action android:name="android.speech.RecognitionService"/></intent></queries>`, sin el cual
    falla en Android 11+;
  - el permiso `RECORD_AUDIO`.
- Se usa UI propia (`VoiceSearchOverlay`, "Habla ahora" / "…" / "No se entendió. Reinténtalo") y no el diálogo del
  sistema, por fidelidad a "Grabadora" (ADR-INV-04).
- `InventorySearchViewModel` recolecta `listen()` en un `Job`. Cuando llega `Result`:
  1. `ParseSpokenIngredientsUseCase` convierte el dictado en ingredientes.
  2. `query = ingredientes.joinToString(", ")`.
  3. Se cierra el aviso.

  Si la lista queda vacía, se trata como `NO_MATCH`. `VoiceDismissed` cancela el `Job`.

## 6. Avisos de vencimiento

- **`WorkManagerExpiryReminderScheduler.schedule()`:**
  - `PeriodicWorkRequest` de 24 h, nombre único `expiry-reminder`, `ExistingPeriodicWorkPolicy.UPDATE`;
  - retardo inicial hasta la próxima 9:00 local.
- **`cancel()`:** `cancelUniqueWork`.
- **`ExpiryReminderWorker` (`@HiltWorker`, `CoroutineWorker`):**
  - sin sesión, termina con `success`;
  - si no, toma la primera emisión del repositorio, aplica `GetExpiringItemsUseCase` y, si hay productos, publica la
    notificación (CA-INV-08-01 a 03).
- **Programación:** `AddItemUseCase` llama a `schedule()` (idempotente), y también `SplashViewModel` al resolver una
  sesión activa, para recuperar el trabajo tras reinstalar. `LogoutUseCase` llama a `cancel()` y descarta la
  notificación (CA-INV-08-06).
- **`ExpiryNotifier`:**
  - canal "Vencimientos" (importancia por defecto) y textos de `strings_inventory.xml` con plurales
    (`<plurals>`);
  - `PendingIntent` a `MainActivity`; la Splash decide si va al inventario o al inicio de sesión (CA-INV-08-04).
  - Si el permiso no está concedido, no publica nada.
- **Permiso `POST_NOTIFICATIONS` (API 33+):** el editor emite `RequestNotificationPermission` después del primer
  guardado exitoso. "Ya se pidió" se guarda con una interfaz nueva `InventoryPreferences` (dominio), implementada
  con el DataStore existente, para no volver a pedirlo (CA-INV-08-05).
- **`SmartfoodApp`:**
  - implementa `Configuration.Provider` con `HiltWorkerFactory`;
  - en el manifiesto se quita `androidx.work.WorkManagerInitializer` del `InitializationProvider`.
  - Lo hace la **T038 (#44)**, no la T048: `ExpiryReminderWorker` es `@HiltWorker` y sin el `HiltWorkerFactory`
    WorkManager no puede instanciarlo. El aviso del #71 que lo movía a la T048 se corrige porque la T048 llega
    después de los avisos, no antes (decisión del 16/10/2026).

## 7. Navegación

```kotlin
@Serializable data object Inventory : Route
@Serializable data object InventorySearch : Route
@Serializable data class ProductEditor(val itemId: String? = null) : Route
@Serializable data object ProductCamera : Route
@Serializable data object Profile : Route          // reemplaza a Home
```

- `MainGraph` arranca en `Inventory`. `navigateToMainGraph()` no cambia, así que CA-INV-09-01 se cumple sin tocar
  la Splash ni el inicio de sesión.
- `MainTabScaffold` (en `core/navigation`) dibuja `SmartFoodBottomBar` y lo usan **solo** las pantallas de pestaña
  (`Inventory` y `Profile`). Las internas y las de autenticación no lo usan, así que no muestran la barra
  (CA-INV-09-02, 04) y no cambian sus márgenes de teclado ni de barras del sistema (spec 002). Un `Scaffold` global
  alrededor del `NavHost` los habría cambiado (decisión del #38). Las pestañas navegan con `navigateToTab`:
  `popUpTo(Inventory) { saveState = true }`, `launchSingleTop = true` y `restoreState = true`.
- Las rutas internas usan `NavTransitions` (empuje de 600 ms). Entre pestañas no hay empuje
  (`NavTransitions.tabEnter`, `tabExit`, `tabPopEnter` y `tabPopExit`).
- `mainGraph()` vive en `core/navigation/MainNavGraph.kt` y reúne `inventoryDestinations` (`feature/inventory/ui`) y
  `profileDestination` (`feature/profile`). Mientras no existan, los destinos del inventario son pantallas
  provisionales que reemplazan sus Issues (#39 a #42).
- `ScreenRoute` gana las entradas correspondientes y pierde `HOME`. `AppNavigationTest` se actualiza: después del
  inicio de sesión se espera "Mi Inventario" y el cierre de sesión pasa por la pestaña Perfil.

## 8. Design system

| Componente | Cambio |
|---|---|
| `SmartFoodTextField` | Nuevos `leadingContent: (@Composable () -> Unit)? = null`, `label: String?` y `accessibilityLabel: String?`. Sin etiqueta visible, el campo se anuncia con `accessibilityLabel` o, si falta, con el placeholder (`semantics { contentDescription }`). Los usos actuales no cambian. |
| `SmartFoodSelectorField` (nuevo, `Inputs.kt`) | La misma caja que `SmartFoodTextField`: las dos usan el `private @Composable FieldFrame` (etiqueta, caja con `inputBorderAndBackground`, ícono y contenido final). Sin teclado. La etiqueta y la caja forman **un solo nodo** que se toca (`Role.Button`), así que tocar la etiqueta abre el selector y TalkBack anuncia "Categoría, Lácteos, botón". Admite `leadingContent`, `trailingContent` (chevron), `isError` y `accessibilityLabel`. |
| `SmartFoodTopBar` (nuevo, `TopBar.kt`) | Barra de las pantallas internas (Figma `476:5960`): fondo `primary`, flecha atrás con descripción "Volver", título centrado como encabezado y acción opcional a la derecha (el "Guardar" del editor). La usan la cámara, el editor y la búsqueda. |
| `FormMetrics` | `fieldIconSize` (Regular 30, Medium 26, Compact 24 dp) y `fieldIconSpacing` (23, 18 y 14 dp): el ícono inicial va en una caja de ese tamaño, en gris `onSurfaceVariant` (el `#9AA39D` del Figma), separado del texto por esa distancia (#62). |
| `SmartFoodBottomBar` (nuevo, `Navigation.kt`) | Barra de 2 pestañas del Figma (`476:5570`): fondo `surfaceVariant` (blanco), línea superior `outline`, pestañas con ícono y texto (Poppins SemiBold de 12 sp) y rol `Tab`. La seleccionada va en `GreenCheck` (el `#16A34A` del Figma) y las demás en `onSurfaceVariant`. Recibe las pestañas por parámetro (`SmartFoodTab`). Los íconos son de Material (`Icons.Outlined.Inventory2` y `Person`, T027). |
| `SmartFoodConfirmDialog` (nuevo, `Dialogs.kt`) | Ícono en círculo, pregunta, botón destructivo ("Eliminar", `error`) y "Cancelar" (`secondary`). Usa la forma de `SmartFoodOfflineDialog`. |
| `SmartFoodResultDialog` (nuevo, `Dialogs.kt`) | Ícono de check, título y cuerpo opcional ("Producto Guardado" / "Te avisaremos…"). Se anuncia como `liveRegion`. |
| `SmartFoodPrimaryButton` | Ya es naranja (`secondary`) habilitado y gris deshabilitado, como "Añadir al Inventario" (`476:5726`), así que no hace falta una variante de acento. Suma `leadingIcon` opcional para el "+" y `destructive` (Boolean) para el botón rojo de confirmación (`error`/`onError`, Figma `476:3815`, CA-INV-04-01), con los colores cerrados: nadie pasa colores sueltos (#80). |

- **Editor adaptable (T054, RNF-INV-06):** barra superior; debajo, un `BoxWithConstraints` que calcula
  `formMetricsFor(maxHeight)` con el alto **sin** el teclado (así los campos no cambian de tamaño al abrirlo); dentro,
  una columna con `imePadding` que lleva los campos (`weight(1f)` y `verticalScroll`), el aviso de error y el botón
  fijo abajo. Al tocar el botón gris, `showMissingFields` marca en rojo los campos que faltan y muestra
  "⚠ Los campos no pueden estar vacíos" (CA-INV-02-02). Se prueba con `AdaptiveLayoutTest` y con capturas reales de
  adb en 360×640, 360×804 y 412×917 dp y con la fuente al 130 %.
- **Precio (T052):** fila "Precio" con teclado numérico debajo de la cantidad; en `InventoryItemRow`, "$ 12.500" junto
  a la fecha con el formato es-CO.
- **Cantidad (T002, desviación de la spec §8):** en el editor, debajo de la categoría, una fila con un
  `SmartFoodTextField` numérico (`KeyboardType.Decimal`) y un `SmartFoodSelectorField` de unidad que abre una hoja
  como la de categoría. En `InventoryItemRow`, el texto "1,5 kg" junto a la etiqueta de categoría, formateado con la
  configuración regional del teléfono (sin ceros decimales sobrantes).
- El editor provee `LocalFormMetrics` con `formMetricsForWindow(maxHeight)`. No hacen falta `smaller()` ni medidas
  fijas nuevas (RNF-INV-06).
- **Colores de estado (T027, #37):** los expone `ExpiryStatus.color`
  (`feature/inventory/ui/components/ExpirySectionHeader.kt`) y los usan el punto del encabezado y el bloque de la
  miniatura sin foto. Decidido con el Figma `476:3114`:
  | Estado | Color | Origen |
  |---|---|---|
  | Vencido | `ErrorRed` `#D92E2E` | token existente (el Figma usa `#DC2626`) |
  | Próximo a vencer | `YellowExpiring` `#F6E938` | token nuevo en `Color.kt`, del nodo `476:3121` |
  | Fresco | `GreenCheck` `#16A34A` | token existente (el Figma usa `#84CC16`) |
- **Íconos (T027, #37):** no hay vectores que exportar. Comprobado con el MCP: la lupa (`476:3201`) sí es vector,
  pero cámara (`476:3680`), micrófono (`476:3211`), el ícono de la pestaña Inventario (`I476:5570;344:2387`), los dos
  íconos de los campos (`CampoEditable`, 30×30) y las ondas de la Grabadora (`476:4508`) son PNG en el archivo. Se usa
  `material-icons-extended`, que ya estaba en el proyecto:
  | Dónde | Ícono |
  |---|---|
  | Campo de nombre | `Icons.Outlined.ShoppingCart` |
  | Campo de categoría | `Icons.Outlined.LocalOffer` |
  | Foto del producto en el editor | `Icons.Outlined.CameraAlt` y `Icons.Outlined.Delete` |
  | Editar de la fila | `Icons.Default.Edit` (#39) |
  | Lupa de la lista | `Icons.Outlined.Search` |
  | Micrófono de "Usa tu voz" | `Icons.Outlined.Mic` |
  | Pestañas | `Icons.Outlined.Inventory2` y `Icons.Outlined.Person` (#38) |
  | Pendiente de subir | `Icons.Outlined.CloudUpload` (T049, #85) |
  Las ondas de la Grabadora no son un asset: se dibujan con Compose en el #43.
- **Textos (T027, #37):** `strings_inventory.xml` lo creó la lista (#39) y cada pantalla agrega los suyos: la búsqueda
  (#42), la voz (#43), el editor (#40) y los avisos (#44). T027 solo documenta la convención; RNF-INV-08 se cumple por
  pantalla.

## 9. Dependencias y configuración

| Cambio | Detalle |
|---|---|
| `libs.versions.toml` | `firebase-storage` (BOM), `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle` y `camera-view` (misma versión), `io.coil-kt.coil3:coil-compose` y `coil-network-okhttp`, `androidx.work:work-runtime-ktx`, `androidx.hilt:hilt-work` (+ `androidx.hilt:hilt-compiler` en KSP), `com.android.tools:desugar_jdk_libs`, `androidx.exifinterface:exifinterface` (1.4.2, la que ya traían CameraX y Coil; se declara porque la usa la compresión de fotos, T018). Se usa la última versión estable de cada una al implementar, salvo dos excepciones: Coil queda en 3.4.0 (desde la 3.5 exige `kotlin-stdlib` 2.4 y el proyecto compila con Kotlin 2.2) y `androidx.hilt` en 1.2.0, alineado con `hilt-navigation-compose`. |
| `app/build.gradle.kts` | `compileOptions.isCoreLibraryDesugaringEnabled = true` y `coreLibraryDesugaring(libs.desugar.jdk.libs)`. Desde el #71, `androidTestImplementation(libs.androidx.work.testing)` para probar los workers (T048). |
| `libs.versions.toml` (#71) | `androidx.work:work-testing`, con la misma versión que `work-runtime-ktx` (T048). |
| `AndroidManifest.xml` | Permisos `CAMERA`, `RECORD_AUDIO` y `POST_NOTIFICATIONS`; `<uses-feature android:name="android.hardware.camera" android:required="false"/>`; `<queries>` del servicio de voz; quitar el inicializador por defecto de WorkManager. |
| `CoreDataModule` | `provideFirebaseStorage()`, no nulo, con `useEmulator(host, 9199)` en modo emulador. Cada SDK se conecta al emulador **una sola vez por proceso** (`connectEmulatorOnce`): Firebase lanza `IllegalStateException` si `useEmulator` se repite, y Hilt arma el grafo en cada `@HiltAndroidTest` (#69). |
| `firebase.json` | `"storage": { "rules": "storage.rules" }` y `"emulators.storage": { "port": 9199 }`. |
| `proguard-rules.pro` | Revisar Coil 3 y WorkManager con Hilt (los workers se instancian por reflexión de la fábrica). Compilar `assembleRelease` para comprobarlo. |

## 10. Errores

- `InventoryErrorMapper.map()`:
  - `FirebaseFirestoreException` → `UNAVAILABLE` y `DEADLINE_EXCEEDED` → `Network`, `PERMISSION_DENIED` →
    `PermissionDenied`, `NOT_FOUND` → `NotFound`, `UNAUTHENTICATED` → `NotAuthenticated`;
  - `StorageException` → `ERROR_RETRY_LIMIT_EXCEEDED` → `Network`, `ERROR_NOT_AUTHORIZED` → `PermissionDenied`,
    `ERROR_OBJECT_NOT_FOUND` → `NotFound` y el resto → `PhotoUploadFailed`;
  - `FirebaseNetworkException` → `Network`.
- Todo en `data` usa `safeRunCatching { … }.mapInventoryError()`. El nombre es distinto del `mapFirebaseError()` de
  autenticación para no confundir los imports.
- `InventoryErrorText.kt` traduce `InventoryError` y `SpeechError` a `strings_inventory.xml`. Para agregar un error:
  `InventoryError` → mapper → `InventoryErrorText` → strings.

## 11. Decisiones (ADR)

| # | Decisión | Alternativas descartadas | Motivo |
|---|---|---|---|
| ADR-INV-01 | Una pantalla de editor para añadir y editar | Dos pantallas | Mismo formulario y mismas validaciones; el Figma solo cambia el título, la acción superior y el botón inferior. |
| ADR-INV-02 | `expiresOn` como `"yyyy-MM-dd"` | `Timestamp` | Un vencimiento es un día, no un instante; un `Timestamp` cambia de día según la zona horaria. |
| ADR-INV-03 | Fotos en Cloud Storage por usuario | Base64 en Firestore; solo en el teléfono | Los documentos de Firestore tienen un límite de 1 MB y se cobran por lectura; la spec pide fotos en la nube. |
| ADR-INV-04 | `SpeechRecognizer` con UI propia | `RecognizerIntent` con el diálogo del sistema; un servicio en la nube de pago | Fidelidad al Figma ("Grabadora") y sin claves (RNF-INV-03). |
| ADR-INV-05 | OR solo entre términos separados por comas | Dividir siempre por espacios | Respeta el Figma ("Carne Pollo" no coincide) y no rompe los nombres compuestos ("Leche Entera"). |
| ADR-INV-06 | Avisos locales con WorkManager | FCM desde Cloud Functions | No necesita servidor ni tokens; el cálculo es el mismo del dominio. |
| ADR-INV-07 | Campos con borde del design system | Línea inferior del Figma | Consistencia con autenticación y adaptabilidad ya probada (spec 002). Aprobado por el equipo. |
| ADR-INV-08 | Filtrado de la búsqueda en el cliente | Consultas de Firestore por término | El inventario de un hogar es pequeño (cientos de productos como mucho); Firestore no permite "contiene" ni OR entre subcadenas. |
| ADR-INV-09 | Uso sin conexión con la cola de Firestore y WorkManager para las fotos (#71) | Base local propia (Room) sincronizada a mano; error de red al guardar sin conexión | Firestore ya trae caché persistente y cola de escrituras; Room duplicaría el modelo y la sincronización. WorkManager sobrevive a cerrar la app y reintenta solo. Las fotos pendientes viven solo en el teléfono, así que las reglas no cambian. |

## 12. Estrategia de pruebas

- **Unitarias (JVM)**, con fakes nuevos en `testutil/`: `FakeInventoryRepository`, `FakePhotoRepository`,
  `FakeSpeechToText`, `FakeExpiryReminderScheduler` y `FixedClock`.
  - `ClassifyExpiryUseCaseTest` (bordes de -1, 0, 3 y 4 días), `ObserveInventoryUseCaseTest`,
    `ValidateItemUseCaseTest` (incluye la cantidad: vacía, "1,5", sin unidad, 0, 10000, "1,234" y "abc"), `AddItemUseCaseTest` y `UpdateItemUseCaseTest` (rollback de la foto),
    `DeleteItemUseCaseTest`.
  - `SearchInventoryUseCaseTest` (tildes, comas, plural, "Carne Pollo") y `ParseSpokenIngredientsUseCaseTest`.
  - `InventoryErrorMapperTest`.
  - Los tres ViewModels: estados, efectos y permisos negados.
- **Instrumentadas (Compose):**
  - `InventoryScreenTest`: secciones, orden, vacío y descripciones accesibles.
  - `ProductEditorScreenTest`: botón deshabilitado/habilitado, errores, hoja de categoría y diálogos.
  - `InventorySearchScreenTest`: `CategoryChips`, sin resultados, aviso de voz con el micrófono desactivado y "x";
    los resultados reutilizan `InventoryItemRow` y la pantalla usa `SmartFoodTopBar`.
  - `MainNavigationTest`: barra inferior, pestañas y "atrás".
  - `AppNavigationTest` actualizado y `AdaptiveLayoutTest` para el editor en 360×720.
- **Manual** (checklist en `tasks.md`): cámara real, voz real en es-CO, permisos negados, notificación a la hora
  (forzada con `adb shell cmd jobscheduler run`), reglas con los emuladores (otro uid → denegado; foto > 2 MB →
  denegado) y producción.
- **Sin conexión (§13):** `FakePhotoSync` en `testutil/` y pruebas U de los casos de uso (`WriteOutcome`, fotos
  encoladas, `HasPendingChangesUseCase`, `LogoutUseCase` con `clearAll`). Pruebas I de los workers con
  `work-testing` (`TestListenableWorkerBuilder`) y de `FirestoreInventoryRepository` con el emulador apagando la red
  (`firestore.disableNetwork()`): guardar devuelve `PENDING`, `isPendingSync` es verdadero y, al encender la red,
  pasa a falso. Manual: modo avión (checklist, punto 7).

## 13. Uso sin conexión (HU-INV-10, #71)

**Principio.** El teléfono es la fuente inmediata y la nube se pone al día sola. Los documentos van por la cola de
Firestore (§3) y las fotos por WorkManager. Ninguna escritura muestra un error por falta de red.

**Indicador de pendiente.**
- `FirestoreInventoryRepository.observeItems()` usa `snapshots(MetadataChanges.INCLUDE)`, y cada documento con
  `metadata.hasPendingWrites()` sale con `isPendingSync = true`.
- `ObserveInventoryUseCase` también marca pendientes los productos con foto en `PhotoSync.pendingPhotos()`.
- `hasPendingWrites()` espera `waitForPendingWrites()` como mucho 500 ms: si no termina, hay escrituras sin confirmar.
  A diferencia de consultar la caché, cubre también los borrados, que ya no aparecen en ella.

**Fotos (`data/sync/`).**
1. `WorkManagerPhotoSync.enqueueUpload` mueve la captura a `filesDir/pending/{itemId}/{uuid}.jpg`
   (`PendingPhotoStore`) y borra las que esperaban para ese producto. No va en `cacheDir`, porque Android puede
   borrarla. El nombre único evita que una subida vieja borre la foto nueva. Después encola `PhotoUploadWorker` como
   trabajo único `photo-upload-{itemId}` (`ExistingWorkPolicy.REPLACE`), con `NetworkType.CONNECTED` y reintento
   exponencial. Los datos del trabajo son `itemId`, `filePath` y `replacedPath`.
2. `PhotoUploadWorker` (`@HiltWorker`, `CoroutineWorker`):
   1. si el archivo no se puede leer (`PhotoCompressor.isReadable`), lo borra y deja el producto sin foto
      (CA-INV-05-06);
   2. sube con `PhotoRepository.upload`, que comprime y ya no borra el archivo: lo borra el worker al final;
   3. si el error es de red, `Result.retry()`; si es de la foto, reintenta hasta 5 veces y después la descarta;
   4. con la foto subida, llama a `setPhoto(itemId, foto)`. Si el producto ya no existe (`NotFound`) o las reglas no
      lo dejan, borra la foto recién subida y termina; con otro error, la borra también y reintenta;
   5. si había `replacedPath`, borra esa foto (CA-INV-05-05);
   6. sin sesión, termina sin hacer nada.
3. `enqueueDelete(path)` encola `PhotoDeleteWorker` (`photo-delete-{path}`, con red y reintentos), que llama a
   `PhotoRepository.delete`, que es idempotente (CA-INV-04-02, CA-INV-10-03).
4. `cancelUpload(itemId)` cancela el trabajo y borra el archivo pendiente.
5. `pendingPhotos()` emite el mapa id → ruta de los archivos de `filesDir/pending`, y se actualiza al encolar,
   subir o cancelar.
6. `clearAll()` cancela todos los trabajos de fotos (etiqueta común `photo-sync`) y vacía `filesDir/pending`.

**Cerrar sesión (CA-INV-10-07).**
- El ViewModel de Perfil llama a `HasPendingChangesUseCase` al tocar "Cerrar sesión".
- Si hay pendientes, muestra `SmartFoodConfirmDialog`: "Tienes cambios sin subir. Si cierras sesión ahora, podrían
  perderse", con "Cerrar sesión" y "Cancelar".
- `LogoutUseCase` cancela los avisos y llama a `PhotoSync.clearAll()` antes de `logout()`.
- Las escrituras de Firestore que siguen en la cola quedan guardadas para esa cuenta: se suben si la misma persona
  vuelve a iniciar sesión, y por eso el aviso dice "podrían perderse".

**Diseño.** Ninguno de estos elementos está en el Figma (spec §8):
- el ícono `Icons.Outlined.CloudUpload` en `onSurfaceVariant`, del tamaño del lápiz (20 dp), con la descripción
  "Pendiente de sincronizar" (`PendingSyncIcon`). En la lista va en la segunda línea, a la izquierda del lápiz: junto a
  la fecha no cabe con el precio sin recortar la etiqueta (T049, #85). En la búsqueda va antes de la etiqueta;
- el diálogo de guardado en el teléfono, con el ícono de nube y "Guardado en tu teléfono: se subirá cuando vuelva la
  conexión";
- los textos van en `strings_inventory.xml`.

**Fuera de alcance.** Resolver conflictos entre dos teléfonos (gana la última escritura, como hoy) y mostrar en otro
teléfono una foto que aún no se sube (spec §7).
