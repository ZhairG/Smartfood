# Tareas 005: Términos y condiciones y Política de privacidad

> Derivadas de [`plan.md`](./plan.md). `[P]` = se puede hacer en paralelo con la tarea anterior (toca otros archivos).
> Cada tarea cita los requisitos o criterios que cubre. Se marca `[x]` cuando cumple la Definition of Done de la
> [constitución](../constitution.md#7-definition-of-done).

## Fase 0: Preparación
- [x] T001 La spec, el plan y las tareas. Actualizar la spec 001 §7 (los enlaces ya no son decorativos).

## Fase 1: Documentos
- [x] T002 `LegalDocumentType` y `LegalVersion` en `feature/legal/domain` (RF-LEG-02).
- [x] T003 [P] `strings_legal.xml` con la plantilla de los dos documentos (RNF-LEG-01).
- [x] T004 `LegalContent`: tipo → recursos (título, introducción y secciones).
- [x] T005 `LegalDocumentScreen`: barra con "Volver", aviso de vista previa y secciones con encabezados (CA-LEG-01-05, CA-LEG-01-06).
- [x] T006 Rutas `TermsOfService` y `PrivacyPolicy`, `legalDestinations()` en la raíz de `AppNavHost` (RF-LEG-01).

## Fase 2: Registro
- [x] T007 `SmartFoodTermsCheckbox` con enlaces que no alternan la casilla y acciones para el lector de pantalla (CA-LEG-01-01, 02, 04 y 07).
- [x] T008 `RegisterScreen` y `AuthNavGraph`: `onOpenTerms` y `onOpenPrivacy` (CA-LEG-01-03).
- [x] T009 Constancia: `UserRepository.createProfile(user, acceptedTermsVersion)`, `FirestoreUserRepository` y `RegisterUseCase` (CA-LEG-02-01).
- [x] T010 Reglas: el `create` de `users/{uid}` exige `termsVersion` y `termsAcceptedAt == request.time` (CA-LEG-02-02).

## Fase 3: Pruebas y cierre
- [x] T011 Unitarias: `RegisterUseCaseTest` y `FakeUserRepository`.
- [x] T012 Instrumentadas: `RegisterScreenTest`, `LegalDocumentScreenTest` y `AppNavigationTest`; llamadas a `RegisterScreen` actualizadas en `AdaptiveLayoutTest` y `FormFocusTest`.
- [x] T013 `README.md` y `AGENTS.md`.
- [ ] T014 Verificación completa en dispositivo (`AppNavigationTest`, `AdaptiveLayoutTest` y `FormFocusTest`) y checklist manual.
- [ ] T015 Publicar las reglas (`firebase deploy --only firestore:rules`) **junto con** la versión de la app que envía la constancia: una app anterior ya no podría crear perfiles.

## Matriz de trazabilidad

Leyenda: **U** = prueba unitaria (`app/src/test`), **I** = prueba instrumentada (`app/src/androidTest`),
**M** = verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-LEG-01-01 | I `RegisterScreenTest.caLeg0101_*`, M |
| CA-LEG-01-02 | I `RegisterScreenTest.caLeg0102_*`, M |
| CA-LEG-01-03 | I `AppNavigationTest.caLeg0103_*` (2), I `LegalDocumentScreenTest.caLeg0103_*` |
| CA-LEG-01-04 | I `RegisterScreenTest.caLeg0104_*` |
| CA-LEG-01-05 | I `LegalDocumentScreenTest.caLeg0105_*` (2) |
| CA-LEG-01-06 | I `LegalDocumentScreenTest.caLeg0106_*` (2), M |
| CA-LEG-01-07 | I `RegisterScreenTest.caLeg0107_*`, M (TalkBack) |
| CA-LEG-02-01 | U `RegisterUseCaseTest`, M |
| CA-LEG-02-02 | M (verificado el 2026-10-06 con los emuladores por REST: sin versión, con fecha del cliente y en el perfil de otro usuario → 403; con constancia → 200) |

### Checklist de verificación manual

**Con los emuladores** (`firebase emulators:start --only auth,firestore,functions` y la app con
`-PuseFirebaseEmulator=true`):

1. En "Crea tu cuenta", tocar "Términos y condiciones" y "Política de privacidad": cada uno abre su pantalla con el
   aviso de vista previa, y la casilla no cambia (CA-LEG-01-01, 02 y 05).
2. Escribir el nombre, marcar la casilla, abrir un documento y volver con la flecha y con el "atrás" del sistema: el
   formulario sigue igual (CA-LEG-01-03).
3. Activar el modo oscuro y revisar el documento a 360 dp de ancho (CA-LEG-01-06).
4. Con TalkBack, en la casilla: el menú de acciones ofrece abrir los dos documentos (CA-LEG-01-07).
5. Registrar una cuenta y revisar en la UI del emulador que `users/{uid}` tiene `termsVersion` y `termsAcceptedAt`
   (CA-LEG-02-01).
6. Crear `users/{uid}` sin `termsVersion` con el SDK de cliente o la REST del emulador → `PERMISSION_DENIED`
   (CA-LEG-02-02).
