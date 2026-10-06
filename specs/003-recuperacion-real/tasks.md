# Tareas 003: Recuperación de contraseña real

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [x] T001 Rama `feature/003-recuperacion-real` y la spec, el plan y las tareas. Actualizar la spec 001 (CA-04-03, RF-AUTH-07 y ADR-01).

## Fase 1: Servidor (`functions/`)
- [x] T002 Proyecto TypeScript (Node 22, firebase-functions v7, firebase-admin v14 y nodemailer) y su `.gitignore`.
- [x] T003 Puertos `ResetStore`, `UserDirectory`, `Mailer` y `Clock` (plan §2).
- [x] T004 `passwordPolicy` igual a `ValidatePasswordUseCase` (CA-REC-02-04).
- [x] T005 `RecoveryService`: código con hash y sal, vencimiento, intentos, límite de envíos y respuesta neutra (HU-REC-01 a 03).
- [x] T006 [P] Adaptadores de Firestore (transacciones), Admin Auth, Gmail y el logger del emulador (RNF-REC-04).
- [x] T007 Funciones callable `requestPasswordResetCode`, `verifyPasswordResetCode` y `resetPasswordWithCode` con `details.reason` (plan §3, RNF-REC-01 y 03).
- [x] T008 Pruebas de `RecoveryService` con fakes en memoria (`npm test`).
- [x] T009 `firebase.json` (functions y emulators) y regla explícita para `passwordResets` (RNF-REC-02).

## Fase 2: App Android
- [x] T010 Dependencia `firebase-functions` y `BuildConfig` (`USE_DEMO_RECOVERY`, `USE_FIREBASE_EMULATOR` y `FIREBASE_EMULATOR_HOST`).
- [x] T011 `CoreDataModule`: `FirebaseFunctions` en `southamerica-east1` y conexión opcional a los emuladores. `network_security_config` solo en debug.
- [x] T012 `AuthError`: `OtpCodeExpired`, `TooManyAttempts` y `TooManyRequests`. `FirebaseErrorMapper` para `FirebaseFunctionsException`.
- [x] T013 `RemotePasswordRecoveryRepository` y selección real/demo en `AuthDataModule` (RF-REC-06).
- [x] T014 [P] `AuthErrorText` y `strings.xml`: textos nuevos y subtítulo neutro (CA-REC-01-03). Se elimina el texto de "cuenta no encontrada" de la recuperación.
- [x] T015 `PasswordRecoveryContract` (casillas en rojo, CA-REC-03-06) y `PasswordRecoveryViewModel` (regreso al paso 2, CA-REC-02-03).
- [x] T016 [P] `DemoPasswordRecoveryRepository` con respuesta neutra (CA-REC-01-02).
- [x] T017 Pruebas unitarias e instrumentadas actualizadas.

## Fase 3: Cierre
- [x] T018 `README.md` y `AGENTS.md`: configuración, despliegue, emuladores y modo demo.
- [x] T019 Verificación local:
  - `npm test` (21 pruebas) y `testDebugUnitTest` (113 pruebas) en verde.
  - `assembleDebug`, `assembleRelease` (R8) y `lintDebug` sin errores.
  - Recorrido completo contra los emuladores, llamando a las funciones por HTTP con el protocolo callable (equivale a los puntos 1 a 6 del checklist, sin la UI).
- [ ] T020 Despliegue en `smartfood-f62a1` (secretos, `firebase deploy` y política de Artifact Registry) y checklist con un correo real. Lo ejecuta el equipo.

## Matriz de trazabilidad

Leyenda: **S** = prueba del servidor (`functions/src/test/recoveryService.test.ts`), **U** = prueba unitaria de la app
(`app/src/test`), **I** = prueba instrumentada (`app/src/androidTest`), **M** = verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-REC-01-01 | S (2), M |
| CA-REC-01-02 | S (2), U `DemoPasswordRecoveryRepositoryTest`, M |
| CA-REC-01-03 | I `PasswordRecoveryScreenTest.ca0404_caRec0103_*` |
| CA-REC-01-04 | S (2), U `FirebaseErrorMapperTest`, U `DemoPasswordRecoveryRepositoryTest`, M |
| CA-REC-01-05 | S |
| CA-REC-02-01 | S, M |
| CA-REC-02-02 | S, M |
| CA-REC-02-03 | S (2), U `PasswordRecoveryViewModelTest` |
| CA-REC-02-04 | S (2), U `FirebaseErrorMapperTest`, U `PasswordRecoveryViewModelTest`, M |
| CA-REC-03-01 | S, U `FirebaseErrorMapperTest`, I `PasswordRecoveryScreenTest.caRec0301_*` |
| CA-REC-03-02 | S (2), U `FirebaseErrorMapperTest`, I `PasswordRecoveryScreenTest.caRec0302_*` |
| CA-REC-03-03 | S, U `FirebaseErrorMapperTest`, U `PasswordRecoveryViewModelTest`, I `PasswordRecoveryScreenTest.caRec0303_*`, M |
| CA-REC-03-04 | S, U `PasswordRecoveryViewModelTest` |
| CA-REC-03-05 | S |
| CA-REC-03-06 | U `PasswordRecoveryViewModelTest` |
| RNF-REC-02 | M |

### Checklist de verificación manual

**Con los emuladores** (`firebase emulators:start --only auth,firestore,functions` y la app con
`-PuseFirebaseEmulator=true`):

1. Registrar una cuenta → "¿Olvidaste tu contraseña?" → paso 2. El código aparece en el log de Functions como `[EMULADOR] Código de recuperación…` (CA-REC-01-01).
2. Escribir un código incorrecto: las casillas se ponen rojas. Con el correcto se pasa al paso 3 (CA-04-06, CA-04-07).
3. Nueva contraseña → éxito → login con la nueva. La anterior responde "Correo o contraseña incorrectos" (CA-REC-02-01).
4. Pedir otro código antes de 60 s → "Espera un momento antes de pedir otro código" (CA-REC-03-03).
5. Con un correo sin cuenta se pasa al paso 2, no aparece ningún código en el log y cualquier código es incorrecto (CA-REC-01-02, CA-REC-01-04).
6. Leer `passwordResets` sin el Admin SDK (REST del emulador de Firestore) → `PERMISSION_DENIED` (RNF-REC-02).

**En producción** (después del despliegue):

7. Repetir los puntos 1 a 3 con un correo real: llega el correo "Tu código de SmartFood es …" y, después de recuperar, las sesiones en otros dispositivos se cierran (CA-REC-02-02).
