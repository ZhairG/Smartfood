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
│   │   ├── SpeechToText.kt
│   │   └── ExpiryReminderScheduler.kt
│   └── usecase/
│       ├── ClassifyExpiryUseCase.kt          umbral de 3 días; recibe un Clock
│       ├── ObserveInventoryUseCase.kt        agrupa por estado y ordena (CA-INV-01-01, 01-04)
│       ├── ValidateItemUseCase.kt            reglas del formulario (único lugar)
│       ├── SaveItemUseCases.kt               AddItemUseCase, UpdateItemUseCase (foto + documento, con rollback)
│       ├── DeleteItemUseCase.kt
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
                                              SmartFoodResultDialog; SmartFoodTextField ampliado
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
    // El precio se agregará aquí como campo opcional en la spec de presupuesto (spec §7).
)
data class InventorySection(val status: ExpiryStatus, val items: List<InventoryItem>)

/** Borrador del formulario: lo que valida ValidateItemUseCase. La cantidad llega como texto, tal como se escribió. */
data class ItemDraft(
    val name: String,
    val expiresOn: LocalDate?,
    val category: FoodCategory?,
    val quantityText: String = "",
    val unit: QuantityUnit? = null,
)

/** Resultado de ValidateItemUseCase: nombre recortado, campos obligatorios presentes y cantidad interpretada. */
data class ValidItem(val name: String, val expiresOn: LocalDate, val category: FoodCategory, val quantity: Quantity?)

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
| `InventoryRepository` | `observeItems(): Flow<List<InventoryItem>>`, `getItem(id): Result<InventoryItem>`, `newItemId(): String`, `saveItem(item): Result<Unit>` (crea o reemplaza), `deleteItem(id): Result<Unit>` |
| `PhotoRepository` | `upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto>`, `delete(path: String): Result<Unit>` |
| `SpeechToText` | `isAvailable(): Boolean`, `listen(): Flow<SpeechEvent>` (terminar de recolectar cancela la escucha) |
| `ExpiryReminderScheduler` | `schedule()`, `cancel()` |

| UseCase | Responsabilidad |
|---|---|
| `ClassifyExpiryUseCase(clock)` | `invoke(expiresOn): ExpiryStatus`. Días = `ChronoUnit.DAYS.between(hoy, expiresOn)`; < 0 vencido, 0..3 próximo, > 3 fresco (CA-INV-01-06). |
| `ObserveInventoryUseCase` | `Flow<List<InventorySection>>` en orden EXPIRED, EXPIRING_SOON, FRESH, sin secciones vacías, ordenado por fecha y nombre. |
| `ValidateItemUseCase(clock)` | `invoke(draft, originalExpiresOn: LocalDate?): Result<ValidItem>`. El nombre recortado debe tener entre 1 y 50 caracteres; la fecha y la categoría son obligatorias; la fecha no puede ser anterior a hoy, salvo si es igual a `originalExpiresOn` (CA-INV-03-03). Cantidad: el texto recortado vacío da `quantity = null` (la unidad se descarta); si no, se cambia la coma por punto y debe ser un número > 0, ≤ 9999 y con 2 decimales como máximo, o falla con `Validation(QUANTITY, INVALID)` (CA-INV-02-09). Sin unidad se usa `UNITS` (CA-INV-02-08). Además expone `isComplete(draft)`, que solo mira nombre, fecha y categoría, para habilitar el botón (CA-INV-02-02). |
| `AddItemUseCase` | Valida → `newItemId()` → sube la foto si hay → `saveItem`. Si `saveItem` falla, borra la foto subida (CA-INV-05-06). La primera vez que guarda, programa los avisos. |
| `UpdateItemUseCase` | Valida → sube la foto nueva si cambió → `saveItem` → borra la foto anterior si se reemplazó o se quitó. Si `saveItem` falla, borra la foto nueva. Que falle el borrado de la foto anterior no falla la operación (queda en el log). |
| `DeleteItemUseCase` | `deleteItem` → borra la foto si había (CA-INV-04-02). |
| `SearchInventoryUseCase` | `invoke(items, query, category?): List<InventoryItem>`. Normaliza (minúsculas y sin tildes con `java.text.Normalizer`), divide por comas, descarta los términos vacíos y aplica OR por "contiene". Cada término coincide también sin la `s` o `es` final (CA-INV-07-04). Orden por fecha. |
| `ParseSpokenIngredientsUseCase` | `invoke(alternatives): List<String>`. Toma la primera alternativa no vacía, la divide por comas y por las palabras " y " y " e ", recorta, quita los artículos iniciales ("el", "la", "los", "las", "un", "una", "unos", "unas") y los duplicados. "carne pollo y leche" → `[carne pollo, leche]`, porque sin pausa ni conector no se puede separar con seguridad. El reconocedor suele insertar las comas en las pausas. |
| `GetExpiringItemsUseCase` | Los productos en estado `EXPIRING_SOON`, ordenados por fecha (CA-INV-08-01, 02). |

### UI (contratos de pantalla)

| Pantalla | UiState (resumen) | Eventos principales | Efectos |
|---|---|---|---|
| `InventoryScreen` | `isLoading`, `sections`, `error` | `AddClicked`, `SearchClicked`, `EditClicked(id)`, `RetryClicked` | `NavigateToEditor(id?)`, `NavigateToSearch` |
| `InventorySearchScreen` | `query`, `selectedCategory`, `results`, `isVoiceAvailable`, `voice: VoiceUiState {Hidden, Listening, Processing, Failed(error)}` | `QueryChanged`, `ClearQuery`, `CategorySelected(c?)`, `VoiceClicked`, `VoicePermissionResult(granted)`, `VoiceRetry`, `VoiceDismissed`, `ResultClicked(id)` | `RequestMicPermission`, `NavigateToEditor(id)`, `ShowPermissionRationale` |
| `ProductEditorScreen` | `mode {Add, Edit}`, `name`, `expiresOn`, `category`, `quantityText`, `unit`, `photo: PhotoUiState {None, Local(path), Remote(url)}`, `isSaveEnabled`, `isSaving`, `error`, `dialog: EditorDialog?` | `NameChanged`, `ExpiryPicked`, `CategoryPicked`, `QuantityChanged`, `UnitPicked`, `CameraClicked`, `PhotoCaptured(path)`, `RemovePhotoClicked`, `RemovePhotoConfirmed`, `SaveClicked`, `DeleteClicked`, `DeleteConfirmed`, `DialogDismissed` | `RequestCameraPermission`, `NavigateToCamera`, `RequestNotificationPermission`, `NavigateBack` |

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
  ├── photoPath       : string?    ruta en Storage
  ├── photoUrl        : string?    URL de descarga
  ├── createdAt       : timestamp  serverTimestamp (solo al crear)
  └── updatedAt       : timestamp  serverTimestamp
```

Se escribe y se lee con `Map` (sin reflexión, por R8), igual que `FirestoreUserRepository`. Sin cantidad, las claves
`quantity` y `unit` no se escriben. Agregar `price` más adelante es agregar otra clave opcional: los documentos viejos
siguen siendo válidos.

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
- El bucket se crea en `southamerica-east1`, como Firestore. El plan Blaze ya está activo.

## 4. Foto: captura, compresión y subida

1. `ProductCameraScreen` usa `LifecycleCameraController` + `PreviewView` (`AndroidView`), con el recuadro de
   esquinas del Figma dibujado en Compose encima.
2. La captura se guarda con `takePicture(OutputFileOptions)` en `cacheDir/photos/{uuid}.jpg`.
3. La ruta vuelve al editor con `navController.previousBackStackEntry?.savedStateHandle["captured_photo"]`, y el
   editor la recibe como `PhotoCaptured(path)`.
4. `FirebaseStoragePhotoRepository.upload`:
   - decodifica con `inSampleSize`;
   - respeta la orientación EXIF;
   - escala a 1080 px en el lado mayor;
   - comprime en JPEG con calidad 80 (RNF-INV-02);
   - sube con `putBytes` y `contentType = image/jpeg`;
   - devuelve `ItemPhoto(path, downloadUrl)`.
5. El archivo temporal se borra después de subirlo o al descartar el formulario.
6. Las miniaturas y la zona de foto cargan `photoUrl` con Coil 3 (`AsyncImage`), con el color del estado como
   marcador (CA-INV-01-03).

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
- `MainScaffold` (en `core/navigation`) envuelve el `NavHost` y muestra `SmartFoodBottomBar` solo si el destino
  actual es `Inventory` o `Profile` (CA-INV-09-02, 04). Las pestañas navegan con
  `popUpTo(Inventory) { saveState = true }`, `launchSingleTop = true` y `restoreState = true`.
- Las rutas internas usan `NavTransitions` (empuje de 600 ms). Las pestañas cambian sin transición de empuje.
- `ScreenRoute` gana las entradas correspondientes y pierde `HOME`. `AppNavigationTest` se actualiza: después del
  inicio de sesión se espera "Mi Inventario" y el cierre de sesión pasa por la pestaña Perfil.

## 8. Design system

| Componente | Cambio |
|---|---|
| `SmartFoodTextField` | Nuevos `leadingContent: (@Composable () -> Unit)? = null` y `label: String?`. Sin etiqueta visible, la etiqueta accesible va en `semantics { contentDescription }`. Los usos actuales no cambian. |
| `SmartFoodSelectorField` (nuevo, `Inputs.kt`) | La misma caja que `SmartFoodTextField` (`inputBorderAndBackground`, `LocalFormMetrics`), sin teclado. Se toca con `Role.Button` y admite `leadingContent`, `trailingContent` (chevron) e `isError`. La etiqueta y la fila con borde se extraen a un `private @Composable` compartido para no duplicar código. |
| `SmartFoodBottomBar` (nuevo) | `NavigationBar` con los ítems Inventario y Perfil, ícono y texto. El seleccionado va en `primary`. |
| `SmartFoodConfirmDialog` (nuevo, `Dialogs.kt`) | Ícono en círculo, pregunta, botón destructivo ("Eliminar", `error`) y "Cancelar" (`secondary`). Usa la forma de `SmartFoodOfflineDialog`. |
| `SmartFoodResultDialog` (nuevo, `Dialogs.kt`) | Ícono de check, título y cuerpo opcional ("Producto Guardado" / "Te avisaremos…"). Se anuncia como `liveRegion`. |
| `SmartFoodPrimaryButton` | Variante de color "acento" (naranja `secondary`) para "Añadir al Inventario" habilitado. El deshabilitado conserva el gris actual. |

- **Cantidad (T002, desviación de la spec §8):** en el editor, debajo de la categoría, una fila con un
  `SmartFoodTextField` numérico (`KeyboardType.Decimal`) y un `SmartFoodSelectorField` de unidad que abre una hoja
  como la de categoría. En `InventoryItemRow`, el texto "1,5 kg" junto a la etiqueta de categoría, formateado con la
  configuración regional del teléfono (sin ceros decimales sobrantes).
- El editor provee `LocalFormMetrics` con `formMetricsForWindow(maxHeight)`. No hacen falta `smaller()` ni medidas
  fijas nuevas (RNF-INV-06).
- Los colores de estado se toman de los tokens existentes (rojo `error`, amarillo y verde de la paleta). Si falta el
  amarillo, se agrega un rol `ExpiringSoon` en `Theme.kt`, no un valor suelto.
- Íconos nuevos en `res/drawable` (carrito, calendario, etiqueta, cámara, papelera, micrófono, ondas), exportados del
  Figma como vectores.

## 9. Dependencias y configuración

| Cambio | Detalle |
|---|---|
| `libs.versions.toml` | `firebase-storage` (BOM), `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle` y `camera-view` (misma versión), `io.coil-kt.coil3:coil-compose` y `coil-network-okhttp`, `androidx.work:work-runtime-ktx`, `androidx.hilt:hilt-work` (+ `androidx.hilt:hilt-compiler` en KSP), `com.android.tools:desugar_jdk_libs`. Se usa la última versión estable de cada una al implementar. |
| `app/build.gradle.kts` | `compileOptions.isCoreLibraryDesugaringEnabled = true` y `coreLibraryDesugaring(libs.desugar.jdk.libs)`. |
| `AndroidManifest.xml` | Permisos `CAMERA`, `RECORD_AUDIO` y `POST_NOTIFICATIONS`; `<uses-feature android:name="android.hardware.camera" android:required="false"/>`; `<queries>` del servicio de voz; quitar el inicializador por defecto de WorkManager. |
| `CoreDataModule` | `provideFirebaseStorage()`, no nulo, con `useEmulator(host, 9199)` en modo emulador. |
| `firebase.json` | `"storage": { "rules": "storage.rules" }` y `"emulators.storage": { "port": 9199 }`. |
| `proguard-rules.pro` | Revisar Coil 3 y WorkManager con Hilt (los workers se instancian por reflexión de la fábrica). Compilar `assembleRelease` para comprobarlo. |

## 10. Errores

- `InventoryErrorMapper.mapFirebaseError()`:
  - `FirebaseFirestoreException` → `UNAVAILABLE` → `Network`, `PERMISSION_DENIED` → `PermissionDenied`,
    `NOT_FOUND` → `NotFound`;
  - `StorageException` → `ERROR_RETRY_LIMIT_EXCEEDED` → `Network`, `ERROR_NOT_AUTHORIZED` → `PermissionDenied` y
    el resto → `PhotoUploadFailed`;
  - `FirebaseNetworkException` → `Network`.
- Todo en `data` usa `safeRunCatching { … }.mapFirebaseError()`.
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
  - `InventorySearchScreenTest`: chips, sin resultados, aviso de voz y "x".
  - `MainNavigationTest`: barra inferior, pestañas y "atrás".
  - `AppNavigationTest` actualizado y `AdaptiveLayoutTest` para el editor en 360×720.
- **Manual** (checklist en `tasks.md`): cámara real, voz real en es-CO, permisos negados, notificación a la hora
  (forzada con `adb shell cmd jobscheduler run`), reglas con los emuladores (otro uid → denegado; foto > 2 MB →
  denegado) y producción.
