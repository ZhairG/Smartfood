# Plan técnico 003: Recuperación de contraseña real

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md). Implementa la opción B elegida en
> [plan 001 §6.5](../001-autenticacion/plan.md#65-ruta-a-una-recuperación-real-post-entrega).

## 1. Stack

| Necesidad | Tecnología |
|---|---|
| Servidor | Cloud Functions for Firebase (2.ª generación), TypeScript, Node 22, región `southamerica-east1` (la de Firestore) |
| Llamadas desde la app | Funciones *callable* (`onCall`) con el SDK `firebase-functions` de Android |
| Cambio de contraseña | Firebase Admin SDK (`updateUser`, `revokeRefreshTokens`) |
| Estado del código | Cloud Firestore, colección `passwordResets` (solo el servidor la usa) |
| Correo | Gmail SMTP con `nodemailer` y una contraseña de aplicación |
| Secretos | Secret Manager (`defineSecret`): `SMTP_USER` y `SMTP_PASS` |
| Desarrollo local | Firebase Local Emulator Suite (Auth, Firestore y Functions) |
| Pruebas del servidor | `node:test` con fakes en memoria |

## 2. Arquitectura

```
App ─ PasswordRecoveryViewModel ─▶ UseCases (sin cambios) ─▶ PasswordRecoveryRepository
                                                                ├─ RemotePasswordRecoveryRepository ──HTTPS──▶ functions/
                                                                └─ DemoPasswordRecoveryRepository (modo demo, solo debug)

functions/src/
├── index.ts            3 funciones onCall: traducen RecoveryError → HttpsError
├── recoveryService.ts  reglas del flujo, sin Firebase: se prueba con fakes
├── ports.ts            ResetStore, UserDirectory, Mailer, Clock
├── adapters.ts         Firestore (transacciones), Admin Auth, nodemailer / logger del emulador
├── passwordPolicy.ts   la misma política que ValidatePasswordUseCase (RF-AUTH-04)
└── test/               pruebas de recoveryService
```

La separación servicio/puertos replica en el servidor la regla de la app: la lógica no conoce Firebase y se prueba
con fakes, sin librería de mocks.

## 3. Contrato de las funciones

| Función | Entrada | Éxito | Razones de error |
|---|---|---|---|
| `requestPasswordResetCode` | `{ email }` | `{}` (igual exista o no la cuenta) | `INVALID_EMAIL`, `TOO_MANY_REQUESTS` |
| `verifyPasswordResetCode` | `{ email, code }` | `{}` | `INVALID_CODE`, `CODE_EXPIRED`, `TOO_MANY_ATTEMPTS` |
| `resetPasswordWithCode` | `{ email, code, newPassword }` | `{}` | `WEAK_PASSWORD`, `INVALID_CODE`, `CODE_EXPIRED`, `TOO_MANY_ATTEMPTS` |

Cada error sale como `HttpsError(code, mensaje, { reason })`. La app clasifica por `reason`, nunca por el mensaje.

| `reason` | Código HTTPS | `AuthError` |
|---|---|---|
| `INVALID_EMAIL` | `invalid-argument` | `InvalidEmail` |
| `INVALID_CODE` | `invalid-argument` | `InvalidOtpCode` |
| `WEAK_PASSWORD` | `invalid-argument` | `WeakPassword` |
| `CODE_EXPIRED` | `failed-precondition` | `OtpCodeExpired` (nuevo) |
| `TOO_MANY_ATTEMPTS` | `resource-exhausted` | `TooManyAttempts` (nuevo) |
| `TOO_MANY_REQUESTS` | `resource-exhausted` | `TooManyRequests` (nuevo) |
| *(sin `reason`)* `unavailable`, `deadline-exceeded` | | `Network` |
| *(sin `reason`)* `not-found` (función no desplegada) | | `ServiceUnavailable` |
| *(sin `reason`)* cualquier otro | | `Unknown` |

## 4. Modelo de datos

```
passwordResets/{sha256(correo en minúsculas)}
  ├── uid          : string | null   (null si el correo no tiene cuenta: CA-REC-01-02)
  ├── codeHash     : string | null   sha256(sal + código), en hex
  ├── salt         : string | null   16 bytes aleatorios por código, en hex
  ├── expiresAt    : number          epoch ms (envío + 10 min)
  ├── attemptsLeft : number          5 por código
  ├── lastSentAt   : number          epoch ms del último envío (espera de 60 s)
  ├── windowStart  : number          inicio de la ventana de 1 h
  └── sentInWindow : number          códigos enviados en la ventana (máx. 5)
```

- El id es un hash para no guardar el correo en claro como clave.
- Reglas: `match /passwordResets/{id} { allow read, write: if false; }`. El Admin SDK no está sujeto a las reglas.
- Cada lectura-modificación-escritura va en una transacción. La transacción **devuelve** el resultado y el error se
  lanza **después** de confirmarla, para que el descuento de intentos se guarde.

## 5. Reglas del servicio

**`requestCode(email)`**
1. Normaliza (`trim` + minúsculas) y valida el formato → `INVALID_EMAIL`.
2. Busca el `uid` (`getUserByEmail`; `user-not-found` → `null`).
3. En la transacción: si `now - lastSentAt < 60 s` o la ventana de 1 h ya tiene 5 envíos → `TOO_MANY_REQUESTS`.
   Si no, genera el código (`crypto.randomInt(0, 10000)` con relleno a 4 dígitos) y una sal nueva, y guarda el
   registro con 5 intentos y vencimiento a 10 min. Así, el código anterior deja de valer (CA-REC-01-05).
4. Si hay `uid`, envía el correo. Sin cuenta no se envía nada, pero la respuesta es idéntica.

**`verifyCode(email, code)`** (también lo usa `resetPassword`)
1. Formato distinto de 4 dígitos, sin registro o sin `codeHash` → `INVALID_CODE`.
2. `now > expiresAt` → `CODE_EXPIRED`. `attemptsLeft <= 0` → `TOO_MANY_ATTEMPTS`.
3. Coincide solo si hay `uid` **y** el hash es igual (`timingSafeEqual`). Un correo sin cuenta nunca coincide, pero
   descuenta intentos igual (CA-REC-03-05).
4. Si falla, descuenta un intento: si quedan intentos → `INVALID_CODE`; si era el último → `TOO_MANY_ATTEMPTS`.

**`resetPassword(email, code, newPassword)`**
1. Política de contraseña → `WEAK_PASSWORD` (antes de tocar el código, para no gastar intentos).
2. `verifyCode`.
3. `updateUser(uid, { password })`, `revokeRefreshTokens(uid)` y borra el registro.

## 6. App Android

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `firebase-functions` (BOM). `buildConfig = true` con los campos de la tabla siguiente. |
| `core/di/CoreDataModule.kt` | Provee `FirebaseFunctions.getInstance("southamerica-east1")`. Con `USE_FIREBASE_EMULATOR`, conecta Auth (9099), Firestore (8080) y Functions (5001) al emulador. |
| `feature/auth/data/repository/RemotePasswordRecoveryRepository.kt` | Llama a las 3 funciones con `Map` (sin reflexión ni reglas de R8 nuevas), `safeRunCatching` y `mapFirebaseError()`. |
| `feature/auth/data/di/AuthDataModule.kt` | `@Provides` que elige `Remote…` o `Demo…` según `BuildConfig.USE_DEMO_RECOVERY`. |
| `feature/auth/data/mapper/FirebaseErrorMapper.kt` | `FirebaseFunctionsException` según la tabla de la §3 (`mapFunctionsError(code, reason)`, que se prueba en la JVM). |
| `feature/auth/domain/model/AuthError.kt` | `OtpCodeExpired`, `TooManyAttempts` y `TooManyRequests`. |
| `feature/auth/ui/AuthErrorText.kt`, `strings.xml` | Textos nuevos y subtítulo neutro del paso 2. Se elimina `error_account_not_found_recovery`. |
| `feature/auth/ui/recovery/PasswordRecoveryContract.kt` | Las casillas se marcan con los 3 errores de código (CA-REC-03-06). |
| `feature/auth/ui/recovery/PasswordRecoveryViewModel.kt` | Si el paso 3 falla por un error de código, vuelve al paso 2 con el código borrado (CA-REC-02-03). |
| `feature/auth/data/demo/DemoPasswordRecoveryRepository.kt` | Respuesta neutra, igual que la real (RF-REC-06). |

**Propiedades de Gradle (solo afectan al build debug; release siempre usa la versión real y Firebase real):**

| Propiedad | `BuildConfig` | Por defecto | Uso |
|---|---|---|---|
| `-PrecoveryDemo=true` | `USE_DEMO_RECOVERY` | `false` | Demo sin backend (datos fijos de la 001). |
| `-PuseFirebaseEmulator=true` | `USE_FIREBASE_EMULATOR` | `false` | Usar la Emulator Suite. |
| `-PfirebaseEmulatorHost=…` | `FIREBASE_EMULATOR_HOST` | `10.0.2.2` | `127.0.0.1` en un teléfono físico con `adb reverse`. |

`app/src/debug/res/xml/network_security_config.xml` permite HTTP solo hacia `10.0.2.2`, `127.0.0.1` y `localhost`.

## 7. Configuración y despliegue

```bash
cd functions && npm ci && npm test                    # compilar y probar el servicio
firebase emulators:start --only auth,firestore,functions
./gradlew :app:installDebug -PuseFirebaseEmulator=true # la app contra los emuladores

firebase functions:secrets:set SMTP_USER              # correo de Gmail del equipo
firebase functions:secrets:set SMTP_PASS              # contraseña de aplicación (16 caracteres)
firebase deploy --only functions,firestore:rules
firebase functions:artifacts:setpolicy --location southamerica-east1 --force   # limpieza de imágenes (evita cobros)
```

- En el emulador, el código se escribe en el log de Functions y no se envía. `functions/.secret.local` (ignorado
  por git) lleva valores de relleno para que el emulador no consulte Secret Manager.
- `setGlobalOptions({ maxInstances: 5 })` acota el costo (RNF-REC-03). Se recomienda una alerta de presupuesto de
  1 USD en Google Cloud Billing.

## 8. Decisiones (ADR)

| # | Decisión | Alternativas descartadas | Motivo |
|---|---|---|---|
| ADR-REC-01 | **Funciones callable** en Cloud Functions; **reemplaza a ADR-01** de la 001 | Endpoints HTTP propios; Vercel | El SDK serializa, propaga `details` y se conecta al emulador con una línea. Vercel obligaba a manejar la cuenta de servicio fuera de Google. |
| ADR-REC-02 | **Respuesta neutra** en los 3 pasos | Mostrar "cuenta no encontrada" (CA-04-03) | Evita que cualquiera descubra qué correos están registrados. |
| ADR-REC-03 | Código guardado como sha256 con sal, con 10 min y 5 intentos | Guardarlo en claro | Si se filtrara la base, los códigos vigentes no quedan expuestos. Los intentos limitan la fuerza bruta a 5/10 000 por código. |
| ADR-REC-04 | Gmail SMTP | Brevo | Gratis, sin verificar dominio y suficiente (unos 500 correos al día) para el proyecto. |
| ADR-REC-05 | Modo demo seleccionable por Gradle | Eliminarlo | Permite hacer demos sin backend y sirve de respaldo si vence la prueba de Blaze. |

## 9. Estrategia de pruebas

- **Servidor (`npm test`)**: `recoveryService` con un `ResetStore`, un `UserDirectory`, un `Mailer` y un reloj en
  memoria. Cubre todos los CA-REC.
- **Unitarias de la app**: `FirebaseErrorMapperTest` (tabla de la §3), `PasswordRecoveryViewModelTest` (regreso al
  paso 2) y `DemoPasswordRecoveryRepositoryTest` (respuesta neutra).
- **Instrumentadas**: `PasswordRecoveryScreenTest` (subtítulo neutro y casillas en rojo).
- **Manual**: recorrido completo contra los emuladores y, después del despliegue, con un correo real
  ([`tasks.md`](./tasks.md#checklist-de-verificación-manual)).
