# Tareas 004: Correo de bienvenida al registrarse

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [x] T001 Rama `feature/004-correo-registro` (sale de `feature/003-recuperacion-real`) y la spec, el plan y las tareas. Nota en CA-01-07 y RF-AUTH-03 de la spec 001.

## Fase 1: Servidor (`functions/`)
- [x] T002 Puertos `RegisteredAccounts`, `WelcomeLog` y `WelcomeMailer` (plan §2).
- [x] T003 `emailTemplates.ts`: marco común, plantilla de recuperación (sin cambios de contenido), plantilla de bienvenida y `escapeHtml` (CA-MAIL-01-01, CA-MAIL-02-03).
- [x] T004 `WelcomeService` y `displayName` (plan §4; HU-MAIL-01 y HU-MAIL-02).
- [x] T005 [P] Adaptadores: `AdminUserDirectory` como `RegisteredAccounts`, `FirestoreWelcomeLog` y bienvenida en `GmailMailer`/`LoggingMailer` (RNF-MAIL-02).
- [x] T006 Función `sendWelcomeEmail` (`onDocumentCreated("users/{uid}")`, con secretos y sin reintentos; RNF-MAIL-01 y 03).
- [x] T007 Pruebas de `WelcomeService` y de la plantilla (`npm test`).
- [x] T008 Regla `welcomeEmails` cerrada a los clientes (RNF-MAIL-04).

## Fase 2: App Android
- [x] T009 `RegisterUseCase` sin `sendEmailVerification()`. Se elimina el método de `AuthRepository`, `FirebaseAuthRepository` y el fake (RF-MAIL-03).
- [x] T010 [P] `register_success_body` menciona el correo de bienvenida.
- [x] T011 `RegisterUseCaseTest` actualizado (CA-01-07, CA-MAIL-01-03).

## Fase 3: Cierre
- [x] T012 `README.md` y `AGENTS.md`.
- [x] T013 Verificación local: `npm test`, `testDebugUnitTest` y `assembleDebug`.
- [ ] T014 Despliegue (`firebase deploy --only functions,firestore:rules`) y checklist con el emulador y con un correo real.

## Matriz de trazabilidad

Leyenda: **S** = prueba del servidor (`functions/src/test/registrationService.test.ts`), **U** = prueba unitaria de la
app (`app/src/test`), **M** = verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-MAIL-01-01 | S (3), M |
| CA-MAIL-01-02 | M |
| CA-MAIL-01-03 | U `RegisterUseCaseTest`, M |
| CA-MAIL-01-04 | S (2), M |
| CA-MAIL-02-01 | S |
| CA-MAIL-02-02 | S |
| CA-MAIL-02-03 | S (2) |
| CA-MAIL-02-04 | S (2) |
| RNF-MAIL-04 | M (verificado el 2026-10-06 con los emuladores: leer `welcomeEmails` → 403) |

### Checklist de verificación manual

**Con los emuladores** (`firebase emulators:start --only auth,firestore,functions` y la app con
`-PuseFirebaseEmulator=true`):

1. Registrar una cuenta. En el log de Functions aparece `[EMULADOR] Correo de bienvenida para … (Nombre): <enlace>`
   y `Bienvenida de <uid>: SENT` (CA-MAIL-01-01).
2. Abrir el enlace en el navegador: en la UI del emulador (Authentication) la cuenta queda con el correo verificado
   (CA-MAIL-01-02).
3. En la UI del emulador (Firestore), editar `users/{uid}` o borrarlo y crearlo de nuevo: el log dice
   `SKIPPED_ALREADY_SENT` (CA-MAIL-02-01).
4. Leer `welcomeEmails` sin el Admin SDK (REST del emulador de Firestore) → `PERMISSION_DENIED` (RNF-MAIL-04).

**En producción** (después del despliegue):

5. Registrar una cuenta con un correo real: llega **un solo** correo, "¡Bienvenido a SmartFood, …!", con el botón
   "Verificar mi correo"; al tocarlo, la página de Firebase confirma la verificación (CA-MAIL-01-01 a 01-03).
