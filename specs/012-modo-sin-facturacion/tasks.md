# Tareas 012: Modo sin facturación

> Rama `feature/012-modo-sin-facturacion`, un commit por tarea. Contratos en [`plan.md`](./plan.md). Las pruebas se
> escriben con cada tarea y se corren en la ronda final de [`docs/pendientes.md`](../../docs/pendientes.md).

- [x] T001 La spec, el plan, las tareas y las enmiendas en las specs 003, 004, 006 y 008.
- [x] T002 **Interruptor** (RF-SPK-01, HU-SPK-01): `BackendMode`, `RemoteConfigBackendModeProvider`,
  `GetBackendModeUseCase`, `BackendModeModule`, Gradle y `SmartfoodApp`.
- [x] T003 **Recuperación con enlace** (RF-SPK-03, HU-SPK-02): `LinkPasswordRecoveryRepository`, modo en
  `AuthDataModule`, `PasswordRecoveryViewModel`, `LinkSentStep` y textos.
- [x] T004 **Correo con enlace** (RF-SPK-04, HU-SPK-03): `LinkEmailChangeRepository`, `ProfileDataModule`,
  `EditProfileViewModel` y el aviso.
- [x] T005 **Verificación al registrarse** (RF-SPK-05, HU-SPK-04): `AuthRepository.sendEmailVerification` y
  `RegisterUseCase`.
- [x] T006 **Almacén de media y Coil** (RF-SPK-06): `FirestoreMediaStore`, `MediaUrls`, `MediaFetcher` y `SmartfoodApp`.
- [x] T007 **Fotos del inventario** (CA-SPK-05-01, 05-03 a 05-06): `compressToFit`, `FirestorePhotoRepository` y
  `ModeAwarePhotoRepository`.
- [x] T008 **Foto del perfil** (CA-SPK-05-02): `FirestoreProfilePhotoRepository` y `ModeAwareProfilePhotoRepository`.
- [x] T009 **Reglas** (RNF-SPK-03): `media` y `photoPath` del perfil en `firestore.rules`, con sus pruebas.
- [x] T010 **Migración** (RF-SPK-07, HU-SPK-06): `functions/scripts/migrar-storage-a-firestore.mjs`.
- [x] T011 **Documentación**: `AGENTS.md`, `docs/backend.md` y `docs/pendientes.md`.
- [x] T012 **Cobertura de los CA-SPK**: `tools/ca_coverage.py` reconoce el área SPK en sus expresiones regulares (solo
  estaba en `AREAS`) y `tools/ca_coverage_baseline.txt` suma los 17 CA-SPK cubiertos (2026-11-21).

## Matriz de trazabilidad

| CA | Prueba |
|---|---|
| CA-SPK-01-01, 01-02 | `ModeAwareRepositoriesTest`, `BackendModeTest` (U) |
| CA-SPK-01-04 | `BackendModeTest` (U) |
| CA-SPK-01-03 | M |
| CA-SPK-02-01 a 02-03, 02-05 | `PasswordRecoveryLinkViewModelTest` (U), `PasswordRecoveryScreenTest` (I) |
| CA-SPK-02-04 | M |
| CA-SPK-03-01, 03-02 | `EditProfileViewModelTest` (U) |
| CA-SPK-03-03, 03-04 | M |
| CA-SPK-04-01 a 04-03 | `RegisterUseCaseTest` (U) |
| CA-SPK-05-01, 05-02 | `MediaUrlsTest`, `ModeAwareRepositoriesTest` (U), `PhotoCompressorTest` (I), M |
| CA-SPK-05-03, 05-05 | `ModeAwareRepositoriesTest` (U) |
| CA-SPK-05-04, 05-06 | M |
| RNF-SPK-03 | `rules-test/firestore.test.js` |
| CA-SPK-06-01 a 06-03 | M (simulación y `--aplicar`) |

U = unitaria, I = instrumentada, M = verificación manual en la ronda final.

### Checklist de verificación manual (ronda final)

- [ ] `-PbackendMode=free`: recuperar la contraseña con el enlace e iniciar sesión con la nueva.
- [ ] `-PbackendMode=free`: registrarse y recibir el correo de verificación.
- [ ] `-PbackendMode=free`: cambiar el correo con el enlace; al volver a entrar, el Perfil muestra el nuevo.
- [ ] `-PbackendMode=free`: foto de un producto con y sin red, reemplazo y borrado; foto del perfil.
- [ ] `-PbackendMode=blaze`: los flujos de las specs 003, 004, 006 y 008 funcionan como antes.
- [ ] Release: `blaze_activo` desde la consola cambia el modo.
- [ ] Migración: simulación y `--aplicar` antes de quitar la facturación.
