# Plan técnico 004: Correo de bienvenida al registrarse

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md). Reutiliza el servidor de la
> [spec 003](../003-recuperacion-real/plan.md).

## 1. Stack

| Necesidad | Tecnología |
|---|---|
| Disparador | Cloud Functions (2.ª gen.), `onDocumentCreated("users/{uid}")`, región `southamerica-east1` |
| Datos de la cuenta y enlace | Firebase Admin SDK (`getUser`, `generateEmailVerificationLink`) |
| Registro de envíos | Cloud Firestore, colección `welcomeEmails/{uid}` (solo el servidor la usa) |
| Correo | Gmail SMTP con `nodemailer` y los secretos `SMTP_USER` y `SMTP_PASS` de la 003 |
| Pruebas | `node:test` con fakes en memoria |

## 2. Arquitectura

```
App ─ RegisterUseCase ─▶ createAccount ─▶ createProfile (users/{uid}) ─▶ logout
                                               │
                                               ▼ (evento de Firestore)
functions/src/
├── index.ts               sendWelcomeEmail: arma WelcomeService y le pasa uid + nombre del perfil
├── registrationService.ts WelcomeService, sin Firebase: se prueba con fakes
├── ports.ts               + RegisteredAccounts, WelcomeLog, WelcomeMailer
├── adapters.ts            + AdminUserDirectory (cuentas), FirestoreWelcomeLog, Gmail/Logging (bienvenida)
├── emailTemplates.ts      plantillas HTML/texto puras (recuperación y bienvenida) y escapeHtml
└── test/                  + registrationService.test.ts
```

## 3. Decisiones (ADR)

| # | Decisión | Alternativas descartadas | Motivo |
|---|---|---|---|
| ADR-MAIL-01 | Disparar con la **creación del perfil** en Firestore | Callable desde la app; `beforeUserCreated` (bloqueante) | Un cliente no puede invocarlo a voluntad. El perfil ya tiene el nombre y solo existe si el registro no hizo rollback (CA-01-09). La función bloqueante exige Identity Platform. |
| ADR-MAIL-02 | **Un solo correo** con el enlace de verificación generado por el Admin SDK | Bienvenida aparte + correo de Firebase | El usuario recibe un único correo, con la marca y en español (CA-MAIL-01-03). |
| ADR-MAIL-03 | El destinatario sale de **Auth**, no del perfil | Usar el campo `email` del perfil | El perfil lo escribe el cliente: usarlo permitiría mandar correos a terceros (CA-MAIL-02-02). |
| ADR-MAIL-04 | Registro `welcomeEmails/{uid}` creado con `create()` **antes** de enviar; se borra si el envío falla | Sin registro; reintentos de Eventarc | Los eventos pueden llegar más de una vez y el perfil puede recrearse (CA-MAIL-02-01). Sin reintentos, un fallo no genera correos repetidos. |

## 4. Reglas del servicio: `WelcomeService.onProfileCreated(uid, profileName)`

1. `accounts.find(uid)`: si es `null` o `emailVerified`, termina sin enviar (CA-MAIL-02-04).
2. `welcomeLog.claim(uid)`: crea el registro de forma atómica. Si ya existía, termina (CA-MAIL-02-01).
3. `accounts.verificationLink(account.email)`.
4. `mailer.sendWelcome(account.email, displayName(profileName), link)` (CA-MAIL-02-02). `displayName` recorta espacios y
   deja 60 caracteres como máximo; si queda vacío, el saludo es genérico.
5. Si el paso 3 o 4 falla: `welcomeLog.release(uid)`, se relanza el error y `index.ts` lo registra **sin** pedir
   reintento (CA-MAIL-01-04).

Devuelve un resultado (`SENT`, `SKIPPED_NO_ACCOUNT`, `SKIPPED_VERIFIED`, `SKIPPED_ALREADY_SENT`) que se escribe en el
log y facilita las pruebas.

## 5. Modelo de datos

```
welcomeEmails/{uid}
  └── sentAt : timestamp   hora en que se reservó el envío
```

Reglas: `match /welcomeEmails/{id} { allow read, write: if false; }` (RNF-MAIL-04).

## 6. Plantilla del correo

- Asunto: `¡Bienvenido a SmartFood, {nombre}!` (o `¡Bienvenido a SmartFood!` sin nombre).
- Cuerpo: saludo, una línea sobre la app, botón **Verificar mi correo** con el enlace y una nota: "Si no creaste esta
  cuenta, ignora este correo". Versión de texto plano con el mismo contenido.
- El nombre pasa por `escapeHtml` en la versión HTML (CA-MAIL-02-03). El HTML comparte `emailLayout` con el correo
  de la recuperación.

## 7. App Android

| Archivo | Cambio |
|---|---|
| `feature/auth/domain/usecase/RegisterUseCase.kt` | Ya no llama a `sendEmailVerification()` (RF-MAIL-03). |
| `feature/auth/domain/repository/AuthRepository.kt`, `data/repository/FirebaseAuthRepository.kt` | Se elimina `sendEmailVerification()`, que ya no se usa. |
| `res/values/strings.xml` | `register_success_body` menciona el correo de bienvenida. |

## 8. Configuración y despliegue

```bash
npm --prefix functions test
firebase emulators:start --only auth,firestore,functions   # el enlace aparece en el log: [EMULADOR] Correo de bienvenida…
firebase deploy --only functions,firestore:rules           # la primera vez habilita Eventarc
```

En el emulador, el enlace apunta al emulador de Auth y al abrirlo marca la cuenta como verificada.

## 9. Estrategia de pruebas

- **Servidor**: `registrationService.test.ts` con fakes de los tres puertos (todos los CA-MAIL salvo 01-02 y 01-03)
  y `escapeHtml`.
- **Unitarias de la app**: `RegisterUseCaseTest` (CA-01-07 sin verificación desde el cliente).
- **Manual**: emulador y un correo real ([`tasks.md`](./tasks.md#checklist-de-verificación-manual)).
