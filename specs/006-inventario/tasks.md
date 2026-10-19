# Tareas 006: Inventario de alimentos

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).
> `→ #N` es el Issue de GitHub donde se planea y se cierra la tarea (milestone "Spec 006: Inventario").
> **Cada Issue enlaza en `InventoryDataModule` la implementación que crea**, para que ninguna persona bloquee las
> pantallas (reorganización del 15/10/2026: el #29 se cerró y sus tareas pasaron al #69, #32 y #33).
> **Antes de las fases 3 y 5 (y de T027):** comprobar que el MCP de Figma responde leyendo un frame de la spec §8
> (por ejemplo, "Inventario" `476:3095`, con `get_metadata` o `get_screenshot`). Si falla, no se implementa el diseño:
> se siguen los pasos de `AGENTS.md` → "Si el MCP de Figma no está configurado".

## Fase 0: Preparación
- [x] T001 La spec, el plan y las tareas en `master` (PR #17), la 006 en la lista de specs de `AGENTS.md` y las tareas divididas en Issues del milestone "Spec 006: Inventario". Cada Issue se trabaja en su propia rama `feature/006-<issue>-<tema>`.
- [x] T002 El equipo confirma si se agregan cantidad, unidad o precio (spec §7). Si se agregan, se actualizan primero la spec y el plan. **Decisión (2026-10-14):** cantidad y unidad opcionales (CA-INV-01-08, 02-08, 02-09); el precio queda para la spec de presupuesto. **Cambio (2026-10-16):** el precio opcional entra en esta spec (T052). → #18

## Fase 1: Dominio (Kotlin puro y pruebas JVM)
- [x] T003 `coreLibraryDesugaring` en `app/build.gradle.kts` y `libs.versions.toml` (plan §9), necesario para `java.time` con `minSdk` 24. → #19
- [x] T004 Modelos: `InventoryItem`, `ItemPhoto`, `FoodCategory`, `ExpiryStatus`, `Quantity`, `QuantityUnit`, `InventorySection`, `ItemDraft`, `ValidItem`, `LocalPhoto`, `InventoryError` + `InventoryException` + `toInventoryError()`, `InventoryField`, `SpeechEvent` y `SpeechError` (plan §2). → #20
- [x] T005 Interfaces: `InventoryRepository`, `PhotoRepository`, `SpeechToText`, `ExpiryReminderScheduler` e `InventoryPreferences`. → #20
- [x] T006 [P] Fakes en `testutil/`: `FakeInventoryRepository`, `FakePhotoRepository`, `FakeSpeechToText`, `FakeExpiryReminderScheduler`, `FakeInventoryPreferences` y `FixedClock`. → #21
- [x] T007 `ClassifyExpiryUseCase` y `ObserveInventoryUseCase`, con pruebas (CA-INV-01-01, 01-04, 01-06; RF-INV-02). → #22
- [x] T008 `ValidateItemUseCase`, con pruebas (CA-INV-02-02, 02-03, 02-04, 02-08, 02-09, 03-03; RF-INV-03). → #23
- [x] T009 `AddItemUseCase`, `UpdateItemUseCase` y `DeleteItemUseCase`, con pruebas de orden, rollback de la foto y programación de avisos (CA-INV-02-06, 03-02, 04-02, 05-04 a 05-06, 08-01). → #24
- [x] T010 [P] `SearchInventoryUseCase`, con pruebas (CA-INV-06-02 a 06-05, 07-04; RF-INV-05). → #25
- [x] T011 [P] `ParseSpokenIngredientsUseCase`, con pruebas (CA-INV-07-03; RF-INV-06). → #26
- [x] T012 [P] `GetExpiringItemsUseCase`, con pruebas (CA-INV-08-01 a 08-03). → #27
- [x] T013 `LogoutUseCase` cancela los avisos (`ExpiryReminderScheduler.cancel()`); se actualiza su prueba (CA-INV-08-06). Mientras no existan los avisos reales, `InventoryDataModule` enlaza `NoOpExpiryReminderScheduler` para que Hilt compile. → #28

## Fase 2: Datos e infraestructura
- [x] T014 Dependencias: `firebase-storage`, CameraX, Coil 3, WorkManager y `hilt-work` (plan §9). → #19
- [x] T015 `CoreDataModule.provideFirebaseStorage()` con el emulador en el puerto 9199. `firebase.json` con `storage` y el emulador (hecho en el #34). Prueba: I `InventoryDataModuleTest`, I `FirebaseStoragePhotoRepositoryEmulatorTest` (inyecta desde Hilt). → #69
- [x] T016 `InventoryErrorMapper`, con pruebas (plan §10). Prueba: U `InventoryErrorMapperTest`. → #30
- [x] T017 `FirestoreInventoryRepository` (`Map`, `serverTimestamp`, `snapshots()` → `Flow`; `quantity` y `unit` solo si hay cantidad) (RF-INV-01). Guardar y borrar con transacción (versión anterior de RNF-INV-05; desde el #71 se reemplaza en T047 por la cola de Firestore). Prueba: U `InventoryDocumentTest`; de punta a punta con las reglas en T022. → #30
- [x] T018 [P] `FirebaseStoragePhotoRepository`, con la compresión a 1080 px / JPEG 80 y EXIF (RNF-INV-02). Pruebas: U `PhotoScalingTest`, I `PhotoCompressorTest`, I `FirebaseStoragePhotoRepositoryEmulatorTest` (con `-PuseFirebaseEmulator=true`). → #31
- [x] T019 [P] `AndroidSpeechToText` (`callbackFlow`, es-CO) y en el manifiesto `<queries>` y `RECORD_AUDIO` (plan §5), y su `@Binds` en `InventoryDataModule`. Pruebas: U `AndroidSpeechToTextTest`, I `InventoryDataModuleTest`. → #32
- [x] T020 [P] `InventoryPreferences` sobre el DataStore existente, y su `@Binds` en `InventoryDataModule`. Pruebas: U `DataStoreInventoryPreferencesTest`, I `InventoryDataModuleTest`. → #33
- [x] T021 `InventoryDataModule` (creado en T013 con el aviso vacío) enlaza también los repositorios. Cada implementación se enlazó en su Issue: Firestore y fotos → #69, voz → #32 y preferencias → #33, las tres enlazadas. Prueba: I `InventoryDataModuleTest`.
- [x] T022 Reglas: subcolección `inventory` en `firestore.rules` (con `validQuantity`) y `storage.rules` nuevo (RNF-INV-01, plan §3). Pruebas: R `rules-test/` (Firestore y Storage), I `FirestoreInventoryRepositoryEmulatorTest` (de punta a punta con `-PuseFirebaseEmulator=true`). → #34

## Fase 3: Design system
> Requiere el MCP de Figma comprobado (ver la nota del inicio).

- [x] T023 `SmartFoodTextField` con `leadingContent` y `label: String?`, sin cambios en los usos actuales. Las pruebas de autenticación siguen verdes. Prueba: I `SmartFoodFieldsTest` (la sobrecarga recursiva que cerraba la app se corrigió en #60; nombre accesible, `FieldFrame` y medidas del ícono en #62). → #35
- [x] T024 [P] `SmartFoodSelectorField` (comparte la caja con el campo de texto) (CA-INV-02-04, 02-05). Prueba: I `SmartFoodFieldsTest.t024_*` (etiqueta y valor en un solo nodo desde #62). → #35
- [x] T025 [P] `SmartFoodConfirmDialog` y `SmartFoodResultDialog` (CA-INV-02-06, 03-02, 04-01, 05-04). Ícono, pregunta, título y cuerpo por parámetro; el resultado se anuncia como `liveRegion` (RNF-INV-04). Prueba: I `SmartFoodDialogsTest.t025_*` y I `SmartFoodPrimaryButtonTest.t025_*`. La variante destructiva del botón se cerró en el #80. → #36
- [x] T026 [P] `SmartFoodBottomBar` y la variante de acento de `SmartFoodPrimaryButton` (CA-INV-02-02, 09-02). Pasó del #36 al #38 para no bloquear la navegación. El botón ya era naranja: solo se agregó `leadingIcon`. Prueba: I `SmartFoodNavigationTest`. → #38
- [x] T027 [P] Colores de estado del inventario y documentación de íconos y textos (CA-INV-01-01, RNF-INV-08). El amarillo es `YellowExpiring` (`#F6E938`, Figma `476:3121`); Vencido y Fresco siguen con `ErrorRed` y `GreenCheck`. Los íconos son de Material porque el Figma solo los tiene como PNG (plan §8), y los textos los entrega cada pantalla: el #39 creó `strings_inventory.xml` y su cabecera lo documenta. Prueba: I `ExpiringSoonColorTest`. → #37

## Fase 4: Navegación
- [x] T028 Renombrar `feature/home` → `feature/profile` (`ProfileScreen`, `ProfileViewModel`, `Route.Profile`). Se ajustan sus pruebas (CA-INV-09-03). Prueba: U `ProfileViewModelTest`. El 16/10 la pantalla pasó al Figma `306:369` (solo "Cerrar Sesión" funciona; spec §8). Pruebas: U `ProfileViewModelTest`, I `ProfileScreenTest`. → #38
- [x] T029 Rutas `Inventory`, `InventorySearch`, `ProductEditor(itemId?)` y `ProductCamera`. `MainGraph` arranca en `Inventory`. `MainTabScaffold` con la barra inferior solo en las pestañas (CA-INV-09-01, 09-02, 09-04, 09-05). Destinos del inventario provisionales hasta #39 a #42. → #38
- [x] T030 `MainNavigationTest` y `AppNavigationTest` actualizados (el inicio de sesión llega a "Mi Inventario" y el cierre de sesión pasa por Perfil). `AppNavigationTest` no necesitó cambios (solo cubre la autenticación); `MainNavigationTest` no toca "Cerrar sesión" para no cerrar la sesión real del dispositivo (CA-05-03 lo cubre `ProfileViewModelTest`). → #38

## Fase 5: Pantallas
> Requiere el MCP de Figma comprobado (ver la nota del inicio).

- [x] T031 Lista: `InventoryContract`, `InventoryViewModel` e `InventoryScreen` con `InventoryItemRow` y `ExpirySectionHeader`, y sus pruebas (HU-INV-01). Pruebas: U `InventoryViewModelTest`, I `InventoryScreenTest`. → #39
- [x] T032 Editor: `ProductEditorContract`, `ProductEditorViewModel`, `ProductEditorScreen` y `ProductEditorRoute` con `PhotoSlot`, `OptionSheet` (categoría y unidad), `ExpiryDatePicker` sin días pasados y la fila de cantidad, y sus pruebas (HU-INV-02, 03, 04; CA-INV-02-08, 02-09). La fila de la lista muestra la cantidad (CA-INV-01-08, en T031). El permiso de notificaciones queda declarado en el contrato y lo conecta la T039 (#44). Pruebas: U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`. → #40
- [x] T033 Cámara: `ProductCameraScreen` (CameraX, recuadro de encuadre, resultado por `savedStateHandle`), con el permiso y la explicación (HU-INV-05). Incluye `SmartFoodTopBar`. Pruebas: I `ProductCameraScreenTest`, I `SmartFoodTopBarTest`. → #41
- [x] T034 Búsqueda: `InventorySearchContract`, `InventorySearchViewModel` e `InventorySearchScreen` con `SmartFoodTopBar`, los `CategoryChips` (en `ui/components/`) y "x", y sus pruebas (HU-INV-06). Los resultados reutilizan `InventoryItemRow`. → #42
- [x] T035 Voz: `VoiceSearchCard` y `VoiceSearchOverlay` ("Habla ahora", "…", error con "Reintentar"), el permiso de micrófono y la cancelación, con pruebas del ViewModel y de la pantalla (HU-INV-07). → #43
- [x] T036 `AdaptiveLayoutTest` cubre el editor (alta y edición) en 360×720, 384×857 y 412×917 con el teclado cerrado; con el teclado abierto se comprueba a mano (RNF-INV-06). Prueba: I `AdaptiveLayoutTest.t036_*`. → #40

- [x] T052 Precio opcional en COP (CA-INV-01-09, 02-10, 02-11): `ItemDraft.priceText`, `ValidItem.price` e `InventoryItem.price`, validación en `ValidateItemUseCase` (`InventoryField.PRICE`), `price` en `InventoryDocument`, `validPrice` y `hasOnly` en `firestore.rules` (desplegado), fila "Precio" en el editor y "$ 12.500" en la lista. Pruebas: U `ValidateItemUseCaseTest`, U `InventoryDocumentTest`, U `ProductEditorViewModelTest`, R `firestore.test.js`, I `ProductEditorScreenTest`, I `InventoryScreenTest`. → #40
- [x] T053 Íconos del Figma (PNG teñidos con el tema) en el editor, la lista, la barra superior, la barra inferior y el Perfil (spec §8). Prueba: las instrumentadas de esas pantallas siguen en verde. → #40
- [x] T054 Editor adaptable: botón fijo abajo con el aviso "⚠ Los campos no pueden estar vacíos" (CA-INV-02-02), medidas sin el inset del teclado y pruebas reales con adb en 360×640, 360×804, 412×917 dp y fuente al 130 % (RNF-INV-06). Pruebas: U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, I `AdaptiveLayoutTest.t036_*`, M (capturas de adb). → #40

- [x] T055 Fidelidad al Figma antes de la entrega: barra de 4 pestañas (Recetas y Guardadas deshabilitadas) también en la búsqueda y el editor, campos del editor con línea inferior y botón dentro del formulario, diálogos y voz con fondo desenfocado, "Imagen Eliminada" y "Producto eliminado" del Figma, texto reconocido en el aviso de voz, resultado del editor sobre la lista y foto desde la galería o la cámara con la hoja "Agregar foto", y etiqueta de la lista del tamaño del Figma con la cantidad junto al nombre (CA-INV-01-08, 02-06, 03-02, 04-02, 05-04, 05-07, 07-03, 09-02, 09-04). Pruebas: I `MainNavigationTest`, I `SmartFoodNavigationTest`, I `AdaptiveLayoutTest`, I `InventoryScreenTest`, U `InventorySearchViewModelTest`, U `ProductEditorViewModelTest`, M (capturas frente al Figma). → PR `fix/ui-figma`
- [x] T056 Indicador de carga al guardar, actualizar y eliminar: `SmartFoodLoadingDialog` (fondo desenfocado, indicador circular y texto) y `savingAction` (`ADD`, `UPDATE`, `DELETE`) en el editor con "Guardando producto…", "Actualizando producto…" o "Eliminando producto…" (CA-INV-02-12). Salió de la sustentación del 16/10. Pruebas: U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, I `SmartFoodDialogsTest`. → #96

## Fase 6: Avisos
- [x] T037 `ExpiryNotifier` (canal, plurales y `PendingIntent`) y `ExpiryReminderWorker` (`@HiltWorker`) (CA-INV-08-01 a 08-04). → #44
- [x] T038 `WorkManagerExpiryReminderScheduler` (próxima 9:00, `UPDATE`). `SmartfoodApp` como `Configuration.Provider`, sin el inicializador por defecto (lo hace esta tarea: `ExpiryReminderWorker` es `@HiltWorker`, decisión del 16/10/2026; la T048 ya lo encuentra hecho). `work-testing` en `libs.versions.toml` y `app/build.gradle.kts` para la prueba del worker (autorizado en el #44). `SplashViewModel` reprograma los avisos con sesión activa. Reemplaza el enlace a `NoOpExpiryReminderScheduler` en `InventoryDataModule` y borra esa clase. → #44
- [x] T039 Permiso `POST_NOTIFICATIONS` después del primer guardado, pedido una sola vez (CA-INV-08-05). → #44

## Fase 7: Cierre
- [x] T040 `proguard-rules.pro` revisado y `assembleRelease` compilado (Coil, WorkManager). → #45
- [x] T041 Plantilla de la Política de privacidad (spec 005): fotos, micrófono y avisos (spec §9). → #45
- [x] T042 `README.md` y `AGENTS.md` (arquitectura de `feature/inventory`, emulador de Storage y comandos). → #45
- [x] T043 Verificación local: `testDebugUnitTest`, `assembleDebug`, pruebas instrumentadas con `firebase emulators:start --only auth,firestore,storage` y el checklist manual (puntos 1 a 9). → #45
- [x] T044 Despliegue: activar Cloud Storage en `smartfood-f62a1` (`southamerica-east1`) y `firebase deploy --only firestore:rules,storage`. Checklist en producción (punto 10). → #45

## Fase 8: Sin conexión (HU-INV-10)
> Spec, plan §13 y ADR-INV-09 aprobados en el #71. Las tareas T046 a T051 se dividen en Issues después de fusionar
> el #71. El orden es T046 → T047 y T048 (en paralelo) → T049, T050 y T051.

- [x] T045 Spec, plan y tareas del uso sin conexión: §7, RNF-INV-05, CA-INV-02-07, 04-02, 05-02, 05-04 a 05-06, HU-INV-10, RF-INV-09 y ADR-INV-09. → #71
- [x] T046 Dominio: `WriteOutcome`, `SavedItem`, `isPendingSync` y `localPhotoPath` en `InventoryItem`; `InventoryRepository` con `createItem`, `updateItem`, `setPhoto` y `hasPendingWrites`; la interfaz `PhotoSync`; los casos de uso de agregar, editar, eliminar y observar sin subir fotos, `HasPendingChangesUseCase` y `LogoutUseCase` con `clearAll()`. Fakes: `FakeInventoryRepository` actualizado y `FakePhotoSync`. Pruebas U que citan CA-INV-10-xx (plan §2 y §13).
- [x] T047 [P] `FirestoreInventoryRepository` sin transacciones: `set`/`update`/`delete` con espera de 3 s → `WriteOutcome`, `MetadataChanges.INCLUDE` → `isPendingSync`, `setPhoto` y `hasPendingWrites` (plan §3). Las reglas no cambian. Pruebas: U `InventoryDocumentTest`, I `FirestoreInventoryRepositoryEmulatorTest` con `disableNetwork()` (CA-INV-10-02, 10-05).
- [x] T048 [P] `PendingPhotoStore`, `PhotoUploadWorker`, `PhotoDeleteWorker` y `WorkManagerPhotoSync` (`@HiltWorker`), con su `@Binds` en `InventoryDataModule`. `SmartfoodApp` como `Configuration.Provider` y sin el inicializador del manifiesto (sale de la T038, #44). `work-testing` ya está declarado desde la T038. Pruebas I de los workers (CA-INV-05-05, 05-06, 10-03, 10-04).
- [x] T049 Lista y búsqueda: ícono de pendiente (`PendingSyncIcon`, a la izquierda del lápiz; en la búsqueda, antes de la etiqueta) y foto local en `InventoryItemRow`, con la descripción "Pendiente de sincronizar" (CA-INV-10-02, 10-04, 10-06). Pruebas: I `InventoryScreenTest` (claro y oscuro), I `InventorySearchScreenTest`. → #85
- [x] T050 Editor: resultado "Guardado en tu teléfono" con `WriteOutcome.PENDING` (`EditorResult.SavedOnPhone`) y la foto pendiente en `PhotoSlot` con `PhotoUiState.Pending`, cargada con `GetItemUseCase` (CA-INV-10-02, 10-04). Incluye las vistas previas que faltaban en la UI del inventario. Salió del #40 el 16/10 y tiene su propio Issue. Pruebas: U `GetItemUseCaseTest`, U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, I `EditorResultHostTest`. → #99
- [x] T051 Perfil: aviso al cerrar sesión con cambios pendientes, con `SmartFoodConfirmDialog` (CA-INV-10-07). Pruebas: U `ProfileViewModelTest`, I `ProfileScreenTest`.

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria (`app/src/test`), **I** = prueba instrumentada (`app/src/androidTest`), **R** = prueba de reglas (`rules-test/`, con los emuladores), **M** =
verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-INV-01-01 | U `ObserveInventoryUseCaseTest`, U `InventoryViewModelTest`, I `InventoryScreenTest`, I `ExpiringSoonColorTest` (color de las secciones) |
| CA-INV-01-02 | I `InventoryScreenTest` |
| CA-INV-01-03 | I `InventoryScreenTest` (sin foto), M 2 (con foto) |
| CA-INV-01-04 | U `ObserveInventoryUseCaseTest` |
| CA-INV-01-05 | U `InventoryViewModelTest`, I `InventoryScreenTest` |
| CA-INV-01-06 | U `ClassifyExpiryUseCaseTest` |
| CA-INV-01-07 | U `ObserveInventoryUseCaseTest`, U `InventoryViewModelTest` (el flujo del fake emite), M 1 |
| CA-INV-01-08 | I `InventoryScreenTest`, M 1 |
| CA-INV-01-09 | I `InventoryScreenTest` |
| CA-INV-02-01 | I `ProductEditorScreenTest` |
| CA-INV-02-02 | U `ValidateItemUseCaseTest`, U `ProductEditorViewModelTest` (aviso de campos vacíos), I `ProductEditorScreenTest`, I `SmartFoodNavigationTest` (botón) |
| CA-INV-02-03 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-04 | U `ValidateItemUseCaseTest`, M 1 |
| CA-INV-02-05 | I `ProductEditorScreenTest` |
| CA-INV-02-06 | U `AddItemUseCaseTest`, U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, I `SmartFoodDialogsTest` |
| CA-INV-02-07 | U `ProductEditorViewModelTest` (rechazo del servidor), U `AddItemUseCaseTest` |
| CA-INV-02-08 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-09 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-10 | U `ValidateItemUseCaseTest`, U `InventoryDocumentTest`, U `ProductEditorViewModelTest`, R `firestore.test.js` |
| CA-INV-02-11 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-12 | U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, I `SmartFoodDialogsTest` |
| CA-INV-03-01 | U `ProductEditorViewModelTest` |
| CA-INV-03-02 | U `UpdateItemUseCaseTest`, U `ProductEditorViewModelTest`, I `SmartFoodDialogsTest` |
| CA-INV-03-03 | U `ValidateItemUseCaseTest` |
| CA-INV-03-04 | U `ProductEditorViewModelTest` |
| CA-INV-04-01 | I `ProductEditorScreenTest`, I `SmartFoodDialogsTest`, I `SmartFoodPrimaryButtonTest` (botón destructivo) |
| CA-INV-04-02 | U `DeleteItemUseCaseTest` (encola el borrado de la foto), U `ProductEditorViewModelTest`, I `PhotoSyncWorkersTest`, M 3 |
| CA-INV-04-03 | I `ProductEditorScreenTest`, I `SmartFoodDialogsTest` |
| CA-INV-05-01 | I `ProductCameraScreenTest`, M 2 |
| CA-INV-05-02 | U `ProductEditorViewModelTest`, M 2 |
| CA-INV-05-03 | U `ProductEditorViewModelTest`, I `ProductCameraScreenTest`, M 4 |
| CA-INV-05-04 | U `UpdateItemUseCaseTest`, U `ProductEditorViewModelTest`, I `ProductEditorScreenTest` (papelera), I `SmartFoodDialogsTest` |
| CA-INV-05-05 | U `UpdateItemUseCaseTest`, I `PhotoSyncWorkersTest` (borra la anterior después de subir), M 2 |
| CA-INV-05-06 | U `AddItemUseCaseTest`, U `UpdateItemUseCaseTest`, I `FirebaseStoragePhotoRepositoryEmulatorTest` (conserva el archivo si falla), I `PhotoSyncWorkersTest` (reintenta; archivo ilegible → sin foto) |
| CA-INV-05-07 | U `ProductEditorViewModelTest`, I `ProductEditorScreenTest`, M (galería y cámara en el teléfono) |
| CA-INV-06-01 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-06-02 | U `SearchInventoryUseCaseTest`, U `InventorySearchViewModelTest` |
| CA-INV-06-03 | U `SearchInventoryUseCaseTest`, U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-06-04 | U `SearchInventoryUseCaseTest` |
| CA-INV-06-05 | U `SearchInventoryUseCaseTest` |
| CA-INV-06-06 | I `InventorySearchScreenTest` |
| CA-INV-06-07 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-06-08 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-01 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-02 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-03 | U `ParseSpokenIngredientsUseCaseTest`, U `InventorySearchViewModelTest`, M 5 |
| CA-INV-07-04 | U `SearchInventoryUseCaseTest` |
| CA-INV-07-05 | U `AndroidSpeechToTextTest`, U `InventorySearchViewModelTest`, I `InventorySearchScreenTest`, M 5 |
| CA-INV-07-06 | U `AndroidSpeechToTextTest`, U `InventorySearchViewModelTest`, M 6 |
| CA-INV-07-07 | U `InventorySearchViewModelTest` (cancela la recolección del fake) |
| CA-INV-07-08 | U `AndroidSpeechToTextTest`, U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-09 | U `InventorySearchViewModelTest` |
| CA-INV-08-01 | U `GetExpiringItemsUseCaseTest`, U `AddItemUseCaseTest` (programa el aviso), U `WorkManagerExpiryReminderSchedulerTest` (próxima 9:00), U `SplashViewModelTest` (reprograma el aviso), I `ExpiryReminderWorkerTest` (publica el aviso), M 8 |
| CA-INV-08-02 | U `GetExpiringItemsUseCaseTest`, I `ExpiryReminderWorkerTest` (nombra el más próximo), M 8 |
| CA-INV-08-03 | U `GetExpiringItemsUseCaseTest`, I `ExpiryReminderWorkerTest` (sin productos por vencer no publica) |
| CA-INV-08-04 | I `ExpiryReminderWorkerTest` (la notificación abre la app), M 8 |
| CA-INV-08-05 | U `DataStoreInventoryPreferencesTest`, U `ProductEditorViewModelTest`, I `InventoryDataModuleTest`, M 8 |
| CA-INV-08-06 | U `SessionUseCasesTest` (cerrar sesión cancela los avisos), U `SplashViewModelTest` (sin sesión no reprograma), I `InventoryDataModuleTest` |
| CA-INV-08-07 | U `LoginUseCaseTest` (iniciar sesión programa el aviso; si falla, no) |
| CA-INV-09-01 | I `MainNavigationTest` |
| CA-INV-09-02 | I `MainNavigationTest`, I `SmartFoodNavigationTest` |
| CA-INV-09-03 | I `MainNavigationTest`, U `ProfileViewModelTest`, I `ProfileScreenTest` |
| CA-INV-09-04 | I `MainNavigationTest` |
| CA-INV-09-05 | I `MainNavigationTest` |
| CA-INV-10-01 | M 7 |
| CA-INV-10-02 | U `AddItemUseCaseTest`, U `UpdateItemUseCaseTest`, U `ProductEditorViewModelTest` (`SavedOnPhone`), I `FirestoreInventoryRepositoryEmulatorTest` (sin red → `PENDING`), I `InventoryScreenTest`, I `EditorResultHostTest`, M 7 |
| CA-INV-10-03 | U `DeleteItemUseCaseTest`, I `PhotoSyncWorkersTest`, M 7 |
| CA-INV-10-04 | U `ObserveInventoryUseCaseTest`, U `GetItemUseCaseTest`, U `ProductEditorViewModelTest` (`Pending`), I `PhotoSyncWorkersTest`, I `InventoryScreenTest`, I `ProductEditorScreenTest` |
| CA-INV-10-05 | I `FirestoreInventoryRepositoryEmulatorTest` (al encender la red, `isPendingSync` pasa a falso), M 7 |
| CA-INV-10-06 | I `InventoryScreenTest` (claro y oscuro), I `InventorySearchScreenTest` |
| CA-INV-10-07 | U `HasPendingChangesUseCaseTest`, U `SessionUseCasesTest`, U `ProfileViewModelTest`, I `ProfileScreenTest` |
| RNF-INV-01 | R `firestore.test.js`, R `storage.test.js`, I `FirestoreInventoryRepositoryEmulatorTest`, M 9 |
| RNF-INV-02 | U `PhotoScalingTest`, I `PhotoCompressorTest`, I `FirebaseStoragePhotoRepositoryEmulatorTest`, R `storage.test.js`, M 9 (foto > 2 MB denegada), M 2 (tamaño subido) |
| RNF-INV-04 | I `InventoryScreenTest`, I `InventorySearchScreenTest` (descripciones y estado de los chips) |
| RNF-INV-05 | Las de CA-INV-10-01 a 10-05, M 7 |
| RNF-INV-06 | I `AdaptiveLayoutTest` (editor, T036), M (teclado abierto) |

### Checklist de verificación manual

**Con los emuladores** (`firebase emulators:start --only auth,firestore,storage` y la app con
`-PuseFirebaseEmulator=true`), en un dispositivo real o un emulador con cámara y micrófono:

1. Añadir "Leche Entera" (Lácteos, vence en 2 días, 1,5 L) y "Pasta" (Cereales, vence en 60 días, sin cantidad):
   aparecen en "Próximo a vencer" y en "Fresco" sin recargar; la leche muestra "1,5 L" y la pasta no muestra cantidad.
   El calendario no deja elegir ayer (CA-INV-01-07, 01-08, 02-04).
2. Añadir "Carne" con foto: el visor muestra el recuadro, la miniatura aparece en la lista y en la UI del emulador
   (Storage) el archivo pesa menos de 400 KB. Reemplazar la foto: el archivo anterior desaparece (CA-INV-05-01,
   05-02, 05-05; RNF-INV-02).
3. Eliminar "Carne": el documento y la foto desaparecen del emulador (CA-INV-04-02).
4. Negar el permiso de cámara: aparece la explicación con el acceso a Ajustes y el producto se guarda sin foto
   (CA-INV-05-03).
5. En la búsqueda, dictar "leche, tomate y pasta": la barra muestra "leche, tomate, pasta" y aparecen los
   productos existentes. Quedarse en silencio: "No se entendió. Reinténtalo" (CA-INV-07-03, 07-05).
6. Negar el permiso de micrófono: aparece el mensaje y la búsqueda por texto funciona (CA-INV-07-06).
7. Con el modo avión:
   - abrir "Mi Inventario": se ven los productos y las fotos ya vistas (CA-INV-10-01);
   - añadir "Arroz" con foto y editar "Leche Entera": aparece "Guardado en tu teléfono…", y los dos quedan con el
     ícono de pendiente; la fila de "Arroz" muestra la foto del teléfono (CA-INV-10-02, 10-04, 10-06);
   - eliminar "Pasta": desaparece de la lista (CA-INV-10-03);
   - tocar "Cerrar sesión" en Perfil: aparece el aviso de cambios sin subir, y "Cancelar" deja la sesión abierta
     (CA-INV-10-07).

   Quitar el modo avión: sin recargar, desaparecen los íconos, y en el emulador aparecen "Arroz" con su foto en
   Storage y la edición de la leche, y "Pasta" ya no está (CA-INV-10-05).
8. Con un producto que vence mañana, forzar el worker (`adb shell cmd jobscheduler run -f com.smartfood.app <id>`
   o la App Inspection de WorkManager): llega "«…» vence pronto" y al tocarla se abre "Mi Inventario". En Android
   13+, el permiso se pidió después del primer guardado y no se vuelve a pedir (CA-INV-08-01, 08-02, 08-04, 08-05).
9. Reglas: con la REST del emulador, leer `users/{otroUid}/inventory` → `PERMISSION_DENIED`; subir a Storage un
   JPEG de 3 MB → denegado; crear un producto con categoría "X", con `quantity` sin `unit` o con `quantity: 0` → denegado (RNF-INV-01,
   RNF-INV-02).

**En producción** (después del despliegue):

10. Con una cuenta real: añadir un producto con foto, buscarlo por voz y eliminarlo. En la consola de Firebase, el
    documento y la foto se crean y se borran.
