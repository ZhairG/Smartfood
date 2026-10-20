# Tareas 008: Perfil funcional

> Derivadas de [`plan.md`](./plan.md). Cada tarea cita los criterios que cubre y se marca `[x]` cuando cumple la
> Definition of Done de la [constitución](../constitution.md#7-definition-of-done). Los Issues agrupan tareas por
> feature vertical (spec §10) y se trabajan en orden en la rama `feature/008-perfil`, con **un commit por tarea** (`Tnnn`).

## Fase 0: Preparación
- [ ] T001 La spec, el plan, las tareas y las capturas de `disenos-figma/Modulo 3 - Perfil`.

## Issue 1: Editar información (avanzada)
- [x] T002 `UserProfile`, `ProfileRepository` y `FirestoreProfileRepository` (`observeProfile`, `updateProfile`, `syncEmail`), con `ProfileDataModule` (RF-PER-01).
- [x] T003 `firestore.rules` (update de `users/{uid}`) y `storage.rules` (`users/{uid}/profile/`), con sus casos en `rules-test/` (RNF-PER-01).
- [x] T004 `AuthRepository`/`FirebaseAuthRepository`/`FakeAuthRepository`: `currentEmail`, `reauthenticate`, `requestEmailChange`; errores `InvalidName`, `InvalidPhone`, `InvalidBirthDate` en `AuthError` y `AuthErrorText` (RF-PER-03).
- [x] T005 Casos de uso `ValidateProfile`, `UpdateProfile`, `RequestEmailChange`, `SyncProfileEmail` (y llamada después de iniciar sesión) con sus pruebas (CA-PER-02-05 a 02-07, 02-10 a 02-12).
- [x] T006 Foto: `FileProvider` y `file_paths.xml`, cámara del sistema con `TakePicture`, galería, `ProfilePhotoRepository` sobre Storage con `PhotoCompressor` (CA-PER-02-08, 02-09).
- [x] T007 Pantalla `EditProfile` (contrato, ViewModel, pantalla, ruta) con aviso, carga y "Guardado" (CA-PER-02-01 a 02-04, 02-13).
- [x] T008 Perfil: foto o iniciales desde `observeProfile`, filas que navegan y tarjetas 0 / inventario / $0 (CA-PER-01-01 a 01-04).
- [x] T009 Pruebas: `EditProfileViewModelTest`, `EditProfileScreenTest`, `ProfileScreenTest` y navegación en `MainNavigationTest`.
- [x] T009a Servidor del cambio de correo con código: `emailChangeService.ts`, adaptadores, plantilla, callable `requestEmailChangeCode` y `confirmEmailChange`, `emailChanges` cerrado en `firestore.rules`, con pruebas (CA-PER-02-10 a 02-12, RF-PER-03).
- [x] T009b App del cambio de correo con código: `EmailChangeRepository`, `refreshSession`, casos de uso, aviso `EmailCodeDialog` y pruebas (CA-PER-02-10 a 02-12).

## Issue 2: Preferencias alimentarias (intermedia)
- [x] T010 `FoodPreferences` y enums; `getPreferences`/`savePreferences` en el repositorio; validación del mapa `preferences` en `firestore.rules` y `rules-test/` (RF-PER-04).
- [ ] T011 Casos de uso `GetFoodPreferences` y `SaveFoodPreferences` con pruebas.
- [x] T012 Pantalla `FoodPreferences` con `PreferenceChip`, selección múltiple o única, aviso, carga y "Guardado" (CA-PER-03-01 a 03-04).
- [x] T013 Pruebas: `FoodPreferencesViewModelTest` y `FoodPreferencesScreenTest`.

## Issue 3: Notificaciones y tema (intermedia)
- [x] T014 `NotificationSettings`, `NotificationSettingsRepository` y `DataStoreNotificationSettings` (`notification_prefs`) (RF-PER-05).
- [x] T015 `SetNotificationSettingUseCase`: programa o cancela el trabajo diario (CA-PER-04-03).
- [x] T016 `ExpiryReminderWorker`: respeta "Productos por vencer" y publica "Inventario vacío" (CA-PER-04-03, 04-04); `ExpiryReminderWorkerTest` actualizado.
- [x] T017 Pantalla `NotificationSettings` con interruptores y permiso de Android 13+ (CA-PER-04-01, 04-02, 04-05, 04-06).
- [x] T018 `ThemePreferences` y `DataStoreThemePreferences` (`theme_prefs`) en `core/preferences`; `MainActivity` aplica el tema (RF-PER-07, CA-PER-06-03, 06-04).
- [x] T019 Fila "Tema de la app" y hoja de tema; revisión del tema oscuro en todas las pantallas (CA-PER-06-01, 06-02, 06-05). Pruebas de las dos pantallas.
- [x] T019a `LoginUseCase` vuelve a programar el aviso diario al iniciar sesión, porque `LogoutUseCase` lo cancela (spec 006, CA-INV-08-07); `LoginUseCaseTest` actualizado.

## Issue 4: Cambiar contraseña (intermedia)
- [x] T020 `AuthRepository.updatePassword` (+ fake) y `ChangePasswordUseCase` (reautentica y cambia) con pruebas (RF-PER-06).
- [x] T021 Pantalla `ChangePassword` con checklist, botón habilitado según reglas, carga, "Guardado" y vuelta al Perfil (CA-PER-05-01 a 05-06).
- [x] T022 Pruebas: `ChangePasswordViewModelTest` y `ChangePasswordScreenTest`.

## Cierre (en el mismo PR)
- [x] T023 Suite instrumentada completa y `rules-test` en verde. 34 clases y 146 pruebas en el teléfono (Android 16) el 2026-10-19; `rules-test` 34/34 y `functions` 48/48.
- [ ] T024 Despliegue de `firestore.rules` y `storage.rules` (con confirmación de Zhair).
- [ ] T025 Checklist manual en el teléfono (abajo) y capturas Figma vs app en el PR.
- [ ] T026 Matriz al día, `README.md` y `AGENTS.md` (arquitectura del Perfil).

## Matriz de trazabilidad

Leyenda: **U** = unitaria, **I** = instrumentada, **R** = `rules-test`, **F** = `functions` (`npm --prefix functions test`), **M** = manual.

| Criterio | Pruebas |
|---|---|
| CA-PER-01-01 | U `ProfileViewModelTest`, I `ProfileScreenTest`, M |
| CA-PER-01-02 | U `ProfileViewModelTest`, I `ProfileScreenTest` |
| CA-PER-01-03 | U `ProfileViewModelTest`, I `ProfileScreenTest`, I `MainNavigationTest` |
| CA-PER-01-04 | I `MainNavigationTest` |
| CA-PER-02-01 | I `EditProfileScreenTest` |
| CA-PER-02-02 | U `EditProfileViewModelTest`, I `EditProfileScreenTest` |
| CA-PER-02-03 | U `EditProfileViewModelTest`, I `EditProfileScreenTest` |
| CA-PER-02-04 | U `EditProfileViewModelTest`, U `ProfileUseCasesTest`, I `EditProfileScreenTest` |
| CA-PER-02-05 | U `ProfileUseCasesTest`, U `EditProfileViewModelTest`, R |
| CA-PER-02-06 | U `ProfileUseCasesTest`, R |
| CA-PER-02-07 | U `ProfileUseCasesTest` |
| CA-PER-02-08 | U `EditProfileViewModelTest`, I `EditProfileScreenTest`, M |
| CA-PER-02-09 | U `ProfileUseCasesTest`, U `EditProfileViewModelTest`, R, M |
| CA-PER-02-10 | F `emailChangeService.test`, R, U `ProfileUseCasesTest`, U `EditProfileViewModelTest`, I `EditProfileScreenTest`, M |
| CA-PER-02-11 | F `emailChangeService.test`, U `ProfileUseCasesTest`, U `EditProfileViewModelTest`, I `EditProfileScreenTest` |
| CA-PER-02-12 | F `emailChangeService.test`, U `ProfileUseCasesTest`, U `ProfileViewModelTest`, M |
| CA-PER-02-13 | U `EditProfileViewModelTest` |
| CA-PER-03-01 | I `FoodPreferencesScreenTest` |
| CA-PER-03-02 | U `FoodPreferencesViewModelTest` |
| CA-PER-03-03 | U `FoodPreferencesViewModelTest`, I `FoodPreferencesScreenTest` |
| CA-PER-03-04 | U `PreferencesDocumentTest`, U `FoodPreferencesUseCasesTest`, U `FoodPreferencesViewModelTest`, R, M |
| CA-PER-04-01 | U `DataStoreNotificationSettingsTest`, I `NotificationSettingsScreenTest` |
| CA-PER-04-02 | U `DataStoreNotificationSettingsTest`, U `NotificationSettingsViewModelTest` |
| CA-PER-04-03 | U `SetNotificationSettingUseCaseTest`, U `NotificationSettingsViewModelTest`, I `ExpiryReminderWorkerTest` |
| CA-PER-04-04 | I `ExpiryReminderWorkerTest`, M |
| CA-PER-04-05 | U `NotificationSettingsViewModelTest`, I `NotificationSettingsScreenTest` |
| CA-PER-04-06 | I `NotificationSettingsScreenTest`, M |
| CA-PER-05-01 | I `ChangePasswordScreenTest`, I `ProfileScreenTest` (la fila abre la pantalla) |
| CA-PER-05-02 | I `ChangePasswordScreenTest` |
| CA-PER-05-03 | U `ChangePasswordViewModelTest` |
| CA-PER-05-04 | U `ChangePasswordUseCaseTest`, U `ChangePasswordViewModelTest`, M |
| CA-PER-05-05 | U `ChangePasswordUseCaseTest`, I `ChangePasswordScreenTest` |
| CA-PER-05-06 | U `ChangePasswordViewModelTest` |
| CA-PER-06-01 | U `ProfileViewModelTest`, I `ProfileScreenTest` |
| CA-PER-06-02 | U `ProfileViewModelTest`, I `ProfileScreenTest` |
| CA-PER-06-03 | U `ThemeViewModelTest`, U `DataStoreThemePreferencesTest`, M |
| CA-PER-06-04 | U `DataStoreThemePreferencesTest`, U `ThemeViewModelTest`, M |
| CA-PER-06-05 | M |
| RNF-PER-01 | R |
| RNF-PER-03 | I (semántica de chips, interruptores y foto) |
| RNF-PER-04 | M (capturas Figma vs app) |

### Checklist de verificación manual
1. Perfil: foto o iniciales, nombre, correo, tarjetas (0 / inventario / $0) y las 5 filas abren su pantalla.
2. Editar información: cambiar nombre, teléfono y fecha; ver la pregunta, la carga y "Guardado"; el Perfil se actualiza.
3. Foto con la cámara del sistema y con la galería; sin red, la foto anterior se conserva.
4. Cambiar el correo: pedir contraseña, recibir el enlace, confirmarlo e iniciar sesión con el correo nuevo.
5. Preferencias: elegir varias restricciones y cocinas, un picante y un presupuesto; guardar; reabrir y en otro teléfono.
6. Notificaciones: apagar "Productos por vencer" y comprobar que no llega el aviso; dejar el inventario vacío y recibir el aviso.
7. Cambiar contraseña: actual incorrecta (error), luego correcta; iniciar sesión con la nueva.
8. Tema: Claro, Oscuro y Según el teléfono; recorrer todas las pantallas en oscuro.
