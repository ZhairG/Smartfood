# Tareas 006: Inventario de alimentos

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).
> `→ #N` es el Issue de GitHub donde se planea y se cierra la tarea (milestone "Spec 006: Inventario").

## Fase 0: Preparación
- [x] T001 La spec, el plan y las tareas en `master` (PR #17), la 006 en la lista de specs de `AGENTS.md` y las tareas divididas en Issues del milestone "Spec 006: Inventario". Cada Issue se trabaja en su propia rama `feature/006-<issue>-<tema>`.
- [x] T002 El equipo confirma si se agregan cantidad, unidad o precio (spec §7). Si se agregan, se actualizan primero la spec y el plan. **Decisión (2026-10-14):** cantidad y unidad opcionales (CA-INV-01-08, 02-08, 02-09); el precio queda para la spec de presupuesto. → #18

## Fase 1: Dominio (Kotlin puro y pruebas JVM)
- [x] T003 `coreLibraryDesugaring` en `app/build.gradle.kts` y `libs.versions.toml` (plan §9), necesario para `java.time` con `minSdk` 24. → #19
- [x] T004 Modelos: `InventoryItem`, `ItemPhoto`, `FoodCategory`, `ExpiryStatus`, `Quantity`, `QuantityUnit`, `InventorySection`, `ItemDraft`, `ValidItem`, `LocalPhoto`, `InventoryError` + `InventoryException` + `toInventoryError()`, `InventoryField`, `SpeechEvent` y `SpeechError` (plan §2). → #20
- [x] T005 Interfaces: `InventoryRepository`, `PhotoRepository`, `SpeechToText`, `ExpiryReminderScheduler` e `InventoryPreferences`. → #20
- [x] T006 [P] Fakes en `testutil/`: `FakeInventoryRepository`, `FakePhotoRepository`, `FakeSpeechToText`, `FakeExpiryReminderScheduler`, `FakeInventoryPreferences` y `FixedClock`. → #21
- [x] T007 `ClassifyExpiryUseCase` y `ObserveInventoryUseCase`, con pruebas (CA-INV-01-01, 01-04, 01-06; RF-INV-02). → #22
- [x] T008 `ValidateItemUseCase`, con pruebas (CA-INV-02-02, 02-03, 02-04, 02-08, 02-09, 03-03; RF-INV-03). → #23
- [ ] T009 `AddItemUseCase`, `UpdateItemUseCase` y `DeleteItemUseCase`, con pruebas de orden, rollback de la foto y programación de avisos (CA-INV-02-06, 03-02, 04-02, 05-04 a 05-06, 08-01). → #24
- [ ] T010 [P] `SearchInventoryUseCase`, con pruebas (CA-INV-06-02 a 06-05, 07-04; RF-INV-05). → #25
- [ ] T011 [P] `ParseSpokenIngredientsUseCase`, con pruebas (CA-INV-07-03; RF-INV-06). → #26
- [ ] T012 [P] `GetExpiringItemsUseCase`, con pruebas (CA-INV-08-01 a 08-03). → #27
- [ ] T013 `LogoutUseCase` cancela los avisos (`ExpiryReminderScheduler.cancel()`); se actualiza su prueba (CA-INV-08-06). → #28

## Fase 2: Datos e infraestructura
- [x] T014 Dependencias: `firebase-storage`, CameraX, Coil 3, WorkManager y `hilt-work` (plan §9). → #19
- [ ] T015 `CoreDataModule.provideFirebaseStorage()` con el emulador en el puerto 9199. `firebase.json` con `storage` y el emulador. → #29
- [ ] T016 `InventoryErrorMapper`, con pruebas (plan §10). → #30
- [ ] T017 `FirestoreInventoryRepository` (`Map`, `serverTimestamp`, `snapshots()` → `Flow`; `quantity` y `unit` solo si hay cantidad) (RF-INV-01). → #30
- [ ] T018 [P] `FirebaseStoragePhotoRepository`, con la compresión a 1080 px / JPEG 80 y EXIF (RNF-INV-02). → #31
- [ ] T019 [P] `AndroidSpeechToText` (`callbackFlow`, es-CO) y en el manifiesto `<queries>` y `RECORD_AUDIO` (plan §5). → #32
- [ ] T020 [P] `InventoryPreferences` sobre el DataStore existente. → #33
- [ ] T021 `InventoryDataModule`, que enlaza las interfaces. → #29
- [ ] T022 Reglas: subcolección `inventory` en `firestore.rules` (con `validQuantity`) y `storage.rules` nuevo (RNF-INV-01, plan §3). → #34

## Fase 3: Design system
- [ ] T023 `SmartFoodTextField` con `leadingContent` y `label: String?`, sin cambios en los usos actuales. Las pruebas de autenticación siguen verdes. → #35
- [ ] T024 [P] `SmartFoodSelectorField` (comparte la caja con el campo de texto) (CA-INV-02-04, 02-05). → #35
- [ ] T025 [P] `SmartFoodConfirmDialog` y `SmartFoodResultDialog` (CA-INV-02-06, 03-02, 04-01, 05-04). → #36
- [ ] T026 [P] `SmartFoodBottomBar` y la variante de acento de `SmartFoodPrimaryButton` (CA-INV-02-02, 09-02). → #36
- [ ] T027 [P] Íconos vectoriales exportados del Figma y `strings_inventory.xml` (RNF-INV-08). Rol de color `ExpiringSoon` si hace falta (plan §8). → #37

## Fase 4: Navegación
- [ ] T028 Renombrar `feature/home` → `feature/profile` (`ProfileScreen`, `ProfileViewModel`, `Route.Profile`). Se ajustan sus pruebas (CA-INV-09-03). → #38
- [ ] T029 Rutas `Inventory`, `InventorySearch`, `ProductEditor(itemId?)` y `ProductCamera`. `MainGraph` arranca en `Inventory`. `MainScaffold` con la barra inferior solo en las pestañas (CA-INV-09-01, 09-02, 09-04, 09-05). → #38
- [ ] T030 `MainNavigationTest` y `AppNavigationTest` actualizados (el inicio de sesión llega a "Mi Inventario" y el cierre de sesión pasa por Perfil). → #38

## Fase 5: Pantallas
- [ ] T031 Lista: `InventoryContract`, `InventoryViewModel` e `InventoryScreen` con `InventoryItemRow` y `ExpirySectionHeader`, y sus pruebas (HU-INV-01). → #39
- [ ] T032 Editor: `ProductEditorContract`, `ProductEditorViewModel` y `ProductEditorScreen` con `PhotoSlot`, `CategorySheet`, `DatePicker` sin días pasados y la fila de cantidad con su hoja de unidad, y sus pruebas (HU-INV-02, 03, 04; CA-INV-02-08, 02-09). La fila de la lista muestra la cantidad (CA-INV-01-08, en T031). → #40
- [ ] T033 Cámara: `ProductCameraScreen` (CameraX, recuadro de encuadre, resultado por `savedStateHandle`), con el permiso y la explicación (HU-INV-05). → #41
- [ ] T034 Búsqueda: `InventorySearchContract`, `InventorySearchViewModel` e `InventorySearchScreen` con `CategoryChips` y "x", y sus pruebas (HU-INV-06). → #42
- [ ] T035 Voz: `VoiceSearchCard` y `VoiceSearchOverlay` ("Habla ahora", "…", error con "Reintentar"), el permiso de micrófono y la cancelación, con pruebas del ViewModel y de la pantalla (HU-INV-07). → #43
- [ ] T036 `AdaptiveLayoutTest` cubre el editor en 360×720, con el teclado abierto y cerrado (RNF-INV-06). → #40

## Fase 6: Avisos
- [ ] T037 `ExpiryNotifier` (canal, plurales y `PendingIntent`) y `ExpiryReminderWorker` (`@HiltWorker`) (CA-INV-08-01 a 08-04). → #44
- [ ] T038 `WorkManagerExpiryReminderScheduler` (próxima 9:00, `UPDATE`). `SmartfoodApp` como `Configuration.Provider`, sin el inicializador por defecto. `SplashViewModel` reprograma los avisos con sesión activa. → #44
- [ ] T039 Permiso `POST_NOTIFICATIONS` después del primer guardado, pedido una sola vez (CA-INV-08-05). → #44

## Fase 7: Cierre
- [ ] T040 `proguard-rules.pro` revisado y `assembleRelease` compilado (Coil, WorkManager). → #45
- [ ] T041 Plantilla de la Política de privacidad (spec 005): fotos, micrófono y avisos (spec §9). → #45
- [ ] T042 `README.md` y `AGENTS.md` (arquitectura de `feature/inventory`, emulador de Storage y comandos). → #45
- [ ] T043 Verificación local: `testDebugUnitTest`, `assembleDebug`, pruebas instrumentadas con `firebase emulators:start --only auth,firestore,storage` y el checklist manual (puntos 1 a 9). → #45
- [ ] T044 Despliegue: activar Cloud Storage en `smartfood-f62a1` (`southamerica-east1`) y `firebase deploy --only firestore:rules,storage`. Checklist en producción (punto 10). → #45

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria (`app/src/test`), **I** = prueba instrumentada (`app/src/androidTest`), **M** =
verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-INV-01-01 | U `ObserveInventoryUseCaseTest`, I `InventoryScreenTest` |
| CA-INV-01-02 | I `InventoryScreenTest` |
| CA-INV-01-03 | I `InventoryScreenTest` (sin foto), M 2 (con foto) |
| CA-INV-01-04 | U `ObserveInventoryUseCaseTest` |
| CA-INV-01-05 | U `InventoryViewModelTest`, I `InventoryScreenTest` |
| CA-INV-01-06 | U `ClassifyExpiryUseCaseTest` |
| CA-INV-01-07 | U `ObserveInventoryUseCaseTest`, U `InventoryViewModelTest` (el flujo del fake emite), M 1 |
| CA-INV-01-08 | I `InventoryScreenTest`, M 1 |
| CA-INV-02-01 | I `ProductEditorScreenTest` |
| CA-INV-02-02 | U `ValidateItemUseCaseTest`, U `ProductEditorViewModelTest`, I `ProductEditorScreenTest` |
| CA-INV-02-03 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-04 | U `ValidateItemUseCaseTest`, M 1 |
| CA-INV-02-05 | I `ProductEditorScreenTest` |
| CA-INV-02-06 | U `AddItemUseCaseTest`, U `ProductEditorViewModelTest`, I `ProductEditorScreenTest` |
| CA-INV-02-07 | U `ProductEditorViewModelTest`, M 7 |
| CA-INV-02-08 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-02-09 | U `ValidateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-03-01 | U `ProductEditorViewModelTest` |
| CA-INV-03-02 | U `UpdateItemUseCaseTest`, U `ProductEditorViewModelTest` |
| CA-INV-03-03 | U `ValidateItemUseCaseTest` |
| CA-INV-03-04 | U `ProductEditorViewModelTest` |
| CA-INV-04-01 | I `ProductEditorScreenTest` |
| CA-INV-04-02 | U `DeleteItemUseCaseTest`, M 3 |
| CA-INV-04-03 | I `ProductEditorScreenTest` |
| CA-INV-05-01 | M 2 |
| CA-INV-05-02 | U `ProductEditorViewModelTest`, M 2 |
| CA-INV-05-03 | U `ProductEditorViewModelTest`, M 4 |
| CA-INV-05-04 | U `UpdateItemUseCaseTest`, I `ProductEditorScreenTest` |
| CA-INV-05-05 | U `UpdateItemUseCaseTest`, M 2 |
| CA-INV-05-06 | U `AddItemUseCaseTest`, U `UpdateItemUseCaseTest` |
| CA-INV-06-01 | I `InventorySearchScreenTest` |
| CA-INV-06-02 | U `SearchInventoryUseCaseTest` |
| CA-INV-06-03 | U `SearchInventoryUseCaseTest`, I `InventorySearchScreenTest` |
| CA-INV-06-04 | U `SearchInventoryUseCaseTest` |
| CA-INV-06-05 | U `SearchInventoryUseCaseTest` |
| CA-INV-06-06 | I `InventorySearchScreenTest` |
| CA-INV-06-07 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-06-08 | U `InventorySearchViewModelTest` |
| CA-INV-07-01 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-02 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-03 | U `ParseSpokenIngredientsUseCaseTest`, U `InventorySearchViewModelTest`, M 5 |
| CA-INV-07-04 | U `SearchInventoryUseCaseTest` |
| CA-INV-07-05 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest`, M 5 |
| CA-INV-07-06 | U `InventorySearchViewModelTest`, M 6 |
| CA-INV-07-07 | U `InventorySearchViewModelTest` (cancela la recolección del fake) |
| CA-INV-07-08 | U `InventorySearchViewModelTest`, I `InventorySearchScreenTest` |
| CA-INV-07-09 | U `InventorySearchViewModelTest` |
| CA-INV-08-01 | U `GetExpiringItemsUseCaseTest`, M 8 |
| CA-INV-08-02 | U `GetExpiringItemsUseCaseTest`, M 8 |
| CA-INV-08-03 | U `GetExpiringItemsUseCaseTest` |
| CA-INV-08-04 | M 8 |
| CA-INV-08-05 | U `ProductEditorViewModelTest`, M 8 |
| CA-INV-08-06 | U `LogoutUseCaseTest` |
| CA-INV-09-01 | I `AppNavigationTest` |
| CA-INV-09-02 | I `MainNavigationTest` |
| CA-INV-09-03 | I `ProfileScreenTest` (antes `HomeScreenTest`) |
| CA-INV-09-04 | I `MainNavigationTest` |
| CA-INV-09-05 | I `MainNavigationTest` |
| RNF-INV-01 | M 9 |
| RNF-INV-02 | M 9 (foto > 2 MB denegada), M 2 (tamaño subido) |
| RNF-INV-04 | I `InventoryScreenTest`, I `InventorySearchScreenTest` (descripciones y estado de los chips) |
| RNF-INV-06 | I `AdaptiveLayoutTest` |

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
7. Con el modo avión, guardar un producto: aparece el error de red y el formulario conserva los datos
   (CA-INV-02-07).
8. Con un producto que vence mañana, forzar el worker (`adb shell cmd jobscheduler run -f com.smartfood.app <id>`
   o la App Inspection de WorkManager): llega "«…» vence pronto" y al tocarla se abre "Mi Inventario". En Android
   13+, el permiso se pidió después del primer guardado y no se vuelve a pedir (CA-INV-08-01, 08-02, 08-04, 08-05).
9. Reglas: con la REST del emulador, leer `users/{otroUid}/inventory` → `PERMISSION_DENIED`; subir a Storage un
   JPEG de 3 MB → denegado; crear un producto con categoría "X", con `quantity` sin `unit` o con `quantity: 0` → denegado (RNF-INV-01,
   RNF-INV-02).

**En producción** (después del despliegue):

10. Con una cuenta real: añadir un producto con foto, buscarlo por voz y eliminarlo. En la consola de Firebase, el
    documento y la foto se crean y se borran.
