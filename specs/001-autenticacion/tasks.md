# Tareas 001: Autenticación

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [x] T001 Punto de control en git y rama `refactor/sdd-auth`.
- [x] T002 Constitución, `spec.md`, `plan.md` y `tasks.md`.

## Fase 1: Estructura base
- [x] T003 Mover el design system a `core/designsystem` (`theme`, `components`, `util`) y `safeRunCatching` a `core/common`.
- [x] T004 [P] Mover la navegación a `core/navigation` y eliminar `Route.OfflineConnection`.
- [x] T005 Catálogo de versiones: agregar Firestore y DataStore, quitar OkHttp y Functions, alinear `hilt-android-testing` con la versión de Hilt.
- [x] T006 Eliminar `BrevoEmailDataSource` y cualquier secreto (RNF-01).

## Fase 2: Dominio (`feature/auth/domain`)
- [x] T007 Modelos `User`, `AuthField`, `AuthError` y `AuthException` (RF-AUTH-08).
- [x] T008 Interfaces `AuthRepository`, `UserRepository`, `PasswordRecoveryRepository` y `SessionPreferences`.
- [x] T009 `ValidateEmailUseCase` y `ValidatePasswordUseCase` (RF-AUTH-04).
- [x] T010 `LoginUseCase` (HU-02, CA-03-01/02).
- [x] T011 `RegisterUseCase` con rollback (HU-01, CA-01-09).
- [x] T012 [P] `ResolveSessionUseCase`, `GetCurrentUserUseCase` y `LogoutUseCase` (HU-03, HU-05).
- [x] T013 [P] `RequestRecoveryCodeUseCase`, `VerifyRecoveryCodeUseCase` y `ResetPasswordUseCase` (HU-04).

## Fase 3: Datos (`feature/auth/data`)
- [x] T014 `FirebaseErrorMapper` (RF-AUTH-08).
- [x] T015 `FirebaseAuthRepository` (RF-AUTH-01, 03, 05).
- [x] T016 [P] `FirestoreUserRepository` y `firestore.rules` (RF-AUTH-02, RNF-02).
- [x] T017 [P] `DataStoreSessionPreferences` (RF-AUTH-06).
- [x] T018 [P] `DemoPasswordRecoveryRepository` y `DemoRecoveryData` (RF-AUTH-07).
- [x] T019 Módulos Hilt `CoreDataModule` y `AuthDataModule`.

## Fase 4: Presentación
- [x] T020 `AuthErrorText` (AuthError → texto) y `AuthScreenScaffold` desacoplado del estado.
- [x] T021 `LoginViewModel`, `LoginContract` (UiState, Event, Effect) y `LoginScreen` (HU-02, HU-06).
- [x] T022 `RegisterViewModel` y `RegisterScreen` (HU-01, HU-06).
- [x] T023 `PasswordRecoveryViewModel` y `PasswordRecoveryScreen` (HU-04).
- [x] T024 `SplashViewModel` y `SplashScreen` (HU-03).
- [x] T025 `HomeViewModel` y `HomeScreen` (HU-05).
- [x] T026 `AppNavHost`, `NavTransitions` y `AuthNavGraph` (navegación de la §7 del plan).

## Fase 5: Pruebas
- [x] T027 Fakes compartidos en `testutil/`.
- [x] T028 Pruebas unitarias de UseCases, mapper y repositorio demo.
- [x] T029 Pruebas unitarias de los 5 ViewModels.
- [x] T030 Actualizar las pruebas instrumentadas de pantallas y navegación (compilan; se ejecutan en un emulador).

## Fase 6: Cierre
- [x] T031 Actualizar `README.md` y `AGENTS.md` (guía para agentes de IA; reemplaza a `CLAUDE.md`).
- [x] T032 Verificación final: 85 pruebas unitarias en verde, `assembleDebug` y `lintDebug` sin errores, `git grep` de secretos limpio.

## Fase 7: Autocompletado (HU-07, #67)
- [x] T033 Spec: HU-07 (CA-07-01 a 07-04), RF-AUTH-09, "Fuera de alcance", ADR-07 y §8.1 del plan.
- [x] T034 `contentType` en `SmartFoodTextField`, `SmartFoodPasswordField` y `PasswordFieldWithChecklist`, y los tipos de cada campo en Login, Registro y Recuperación (CA-07-01, 07-03, 07-04). El tipo del correo es la constante `EmailAccountContentType` (`AuthContentTypes.kt`): cada `+` crea un objeto nuevo y las pruebas comparan por instancia.
- [x] T035 `AuthNavGraph` llama a `AutofillManager.commit()` al terminar bien el login, el registro o el cambio de contraseña (CA-07-02).
- [x] T036 Pruebas instrumentadas de los tipos y verificación manual en el teléfono (checklist 7).

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria (`app/src/test`), **I** = prueba instrumentada (`app/src/androidTest`),
**M** = verificación manual (checklist al final).

| Criterio | Pruebas |
|---|---|
| CA-01-01 | U `RegisterUseCaseTest`, U `RegisterViewModelTest` |
| CA-01-02 | U `RegisterUseCaseTest` |
| CA-01-03 | U `RegisterViewModelTest`, I `RegisterScreenTest.ca0103_*` |
| CA-01-04 | U `RegisterUseCaseTest`, U `RegisterViewModelTest` |
| CA-01-05 | U `RegisterUseCaseTest`, U `RegisterViewModelTest`, I `RegisterScreenTest.ca0105_*` |
| CA-01-06 | U `RegisterUseCaseTest`, U `RegisterViewModelTest`, U `FirebaseErrorMapperTest`, I `RegisterScreenTest.ca0106_*` |
| CA-01-07 | U `RegisterUseCaseTest` (2), U `RegisterViewModelTest`, M |
| CA-01-08 | M |
| CA-01-09 | U `RegisterUseCaseTest` |
| CA-02-01 | U `LoginUseCaseTest` (2), U `LoginViewModelTest`, I `LoginScreenTest.ca0201_*` |
| CA-02-02 | U `LoginUseCaseTest`, U `LoginViewModelTest` |
| CA-02-03 | U `LoginUseCaseTest`, U `LoginViewModelTest`, U `FirebaseErrorMapperTest`, I `LoginScreenTest.ca0203_*` |
| CA-02-04 | U `LoginViewModelTest`, U `FirebaseErrorMapperTest` |
| CA-02-05 | U `LoginUseCaseTest`, U `LoginViewModelTest`, M |
| CA-02-06 | U `LoginViewModelTest` |
| CA-02-07 | U `LoginViewModelTest` |
| CA-03-01 | U `SessionUseCasesTest`, U `SplashViewModelTest`, M |
| CA-03-02 | U `SessionUseCasesTest`, U `SplashViewModelTest`, U `LoginUseCaseTest`, M |
| CA-03-03 | U `SessionUseCasesTest`, U `SplashViewModelTest`, I `AppNavigationTest` |
| CA-03-04 | I `AppNavigationTest.transicionSplashALogin_*` |
| CA-04-01 | U `PasswordRecoveryUseCasesTest`, U `PasswordRecoveryViewModelTest`, I `PasswordRecoveryScreenTest.ca0401_*` |
| CA-04-02 | U `PasswordRecoveryUseCasesTest` |
| ~~CA-04-03~~ | Reemplazado por CA-REC-01-02 ([tasks 003](../003-recuperacion-real/tasks.md#matriz-de-trazabilidad)) |
| CA-04-04 | U `PasswordRecoveryUseCasesTest`, U `DemoPasswordRecoveryRepositoryTest`, U `PasswordRecoveryViewModelTest`, I `PasswordRecoveryScreenTest.ca0404_*` |
| CA-04-05 | U `PasswordRecoveryUseCasesTest` |
| CA-04-06 | U `PasswordRecoveryUseCasesTest`, U `DemoPasswordRecoveryRepositoryTest`, U `PasswordRecoveryViewModelTest`, I `PasswordRecoveryScreenTest.ca0406_*` |
| CA-04-07 | U `PasswordRecoveryUseCasesTest`, U `DemoPasswordRecoveryRepositoryTest`, U `PasswordRecoveryViewModelTest` |
| CA-04-08 | U `PasswordRecoveryViewModelTest` |
| CA-04-09 | U `PasswordRecoveryViewModelTest` |
| CA-04-10 | U `PasswordRecoveryUseCasesTest` (2), U `PasswordRecoveryViewModelTest`, I `PasswordRecoveryScreenTest.ca0410_*` (2) |
| CA-04-11 | U `PasswordRecoveryUseCasesTest`, U `DemoPasswordRecoveryRepositoryTest`, U `PasswordRecoveryViewModelTest`, M |
| CA-04-12 | I `AppNavigationTest.transicionRecuperacionALogin_*`, M |
| CA-05-01 | U `SessionUseCasesTest`, U `HomeViewModelTest`, M |
| CA-05-02 | U `HomeViewModelTest` |
| CA-05-03 | U `SessionUseCasesTest`, U `HomeViewModelTest`, M |
| CA-06-01 | U `FirebaseErrorMapperTest`, U `LoginViewModelTest`, U `RegisterViewModelTest`, I `LoginScreenTest.ca0601_*` |
| CA-06-02 | U `LoginViewModelTest`, U `RegisterViewModelTest` |
| RF-AUTH-04 | U `ValidateEmailUseCaseTest`, U `ValidatePasswordUseCaseTest` |
| CA-07-01 | I `LoginScreenTest.ca0701_*`, M 7 |
| CA-07-02 | M 7 |
| CA-07-03 | I `RegisterScreenTest.ca0703_*`, I `PasswordRecoveryScreenTest.ca0703_*`, M 7 |
| CA-07-04 | I `SmartFoodFieldsTest.ca_07_04_*` |

### Checklist de verificación manual (emulador con Firebase real)

1. Registro con datos válidos → aparece `users/{uid}` en la consola de Firestore → pantalla de éxito → "Continuar" lleva al login y "atrás" no vuelve al formulario (CA-01-07, CA-01-08).
2. Login con esa cuenta → Home dice "Hola, <nombre>" y "atrás" cierra la app (CA-02-05, CA-05-01).
3. Cerrar y volver a abrir la app → Home (CA-03-01). Repetir el login sin "Recuérdame" → al reabrir, Login (CA-03-02).
4. Recuperación en modo demo (`-PrecoveryDemo=true`) con `demo@smartfood.com` + `1234` → nueva contraseña → éxito → login (CA-04-11). La recuperación real se verifica con el checklist de la [spec 003](../003-recuperacion-real/tasks.md#checklist-de-verificación-manual). Volver desde el paso 2 y reentrar → empieza en el paso 1 (CA-04-12).
5. "Cerrar Sesión" en Home → Login y "atrás" no vuelve a Home (CA-05-03).
6. Modo avión → intentar login → diálogo "Sin conexión" → desactivar modo avión → "Reconectar" (CA-06-01, CA-06-02).
7. Con el autocompletado de Google activo en el teléfono (*Ajustes → Contraseñas y cuentas*): tocar el correo del
   login → aparecen las cuentas guardadas y al elegir una se llenan los dos campos (CA-07-01); entrar con una cuenta
   no guardada → el teléfono ofrece guardarla (CA-07-02); en el registro, tocar la contraseña → se ofrece una
   contraseña segura (CA-07-03). En esa hoja, Google pide escribir el "Nombre de usuario" (el correo): la arma con los
   datos del inicio de la sesión de autocompletado, cuando el correo aún estaba vacío, y ni la app ni Compose pueden
   llenarla. Se probó marcar el correo solo como `EmailAddress` y cancelar la sesión al salir del correo: ninguno lo
   llena (el segundo incluso quita la sugerencia). Al terminar el registro, el ofrecimiento de guardar usa el correo
   del formulario. Las cuentas guardadas con el APK release no aparecen en
   el debug. Si Google no ofrece guardar, revisar en el Gestor de contraseñas que "Ofrecer guardar contraseñas" esté
   activo y que SmartFood no esté en "Sitios y apps rechazados" (sin eso, su respuesta no trae `SaveInfo`).
