# Spec 012: Modo sin facturación

| Campo | Valor |
|---|---|
| Feature | `012-modo-sin-facturacion` |
| Estado | En implementación |
| Fecha | 2026-11-18 |
| Depende de | 003-recuperacion-real, 004-correo-registro, 006-inventario, 008-perfil |
| Afecta | `core` (interruptor y fotos en Firestore), recuperación de contraseña, registro, cambio de correo, fotos del inventario y del perfil, `firestore.rules` |
| Entrega | Un PR en *draft* desde `feature/012-modo-sin-facturacion` ([`docs/pendientes.md`](../../docs/pendientes.md), punto 1) |

> Los códigos de esta spec llevan el prefijo `SPK` (`HU-SPK-01`, `CA-SPK-01-01`, `RF-SPK-01`, `RNF-SPK-01`).

## 1. Contexto y motivación

El proyecto `smartfood-f62a1` deja el plan **Blaze** en noviembre de 2026. En el plan gratuito **Spark**:

| Pieza | Requiere Blaze | Qué pasa en Spark |
|---|---|---|
| Authentication | No | Sigue igual. |
| Firestore | No | Sigue igual, con la cuota gratuita diaria. |
| Remote Config | No | Disponible. Se usa para el interruptor. |
| Cloud Functions (6) | **Sí** | No se ejecutan: recuperación con código (spec 003), cambio de correo con código (spec 008) y correo de bienvenida (spec 004). |
| Secret Manager (`SMTP_USER`, `SMTP_PASS`) | **Sí** | Inaccesible. |
| Cloud Storage | **Sí** | Responde 402/403: las fotos del inventario y del perfil no se suben ni se leen. |

El objetivo es que la app funcione completa en Spark y que vuelva al backend actual con **solo cambiar un interruptor**,
sin publicar otra versión. No se borra nada de `functions/` ni del código de Storage. El patrón es el de la spec 008
«Modo sin facturación» de [Huellitas_YA](https://github.com/ErickEsquivel/Huellitas_YA).

## 2. Actores

- **Usuario:** recupera su contraseña, cambia su correo y sube fotos sin notar qué plan tiene el proyecto.
- **Zhair:** activa o desactiva el modo Blaze desde la consola de Firebase.

## 3. Historias de usuario y criterios de aceptación

### HU-SPK-01: Interruptor del backend
**Como** Zhair **quiero** elegir el backend desde la consola **para** volver a Blaze sin publicar otra versión.

| Código | Criterio |
|---|---|
| CA-SPK-01-01 | **Dado** `blaze_activo = false` en Remote Config, o que no se pudo leer, **entonces** la app usa el modo gratuito. |
| CA-SPK-01-02 | **Dado** `blaze_activo = true`, **entonces** la app se comporta como antes de esta spec (specs 003, 004, 006 y 008). |
| CA-SPK-01-03 | **Dado** un cambio del parámetro en la consola, **cuando** la app se abre después del intervalo de lectura (1 h en release, inmediato en debug), **entonces** usa el modo nuevo. |
| CA-SPK-01-04 | **Dado** un build debug con `-PbackendMode=blaze` o `-PbackendMode=free`, **entonces** ese modo manda sobre Remote Config. En release la propiedad no existe. |

### HU-SPK-02: Recuperar la contraseña con un enlace
**Como** usuario **quiero** recuperar mi contraseña aunque no haya Cloud Functions **para** volver a entrar.

| Código | Criterio |
|---|---|
| CA-SPK-02-01 | **Dado** el modo gratuito, **cuando** escribo mi correo en «¿Olvidaste tu contraseña?», **entonces** el botón dice «Enviar enlace» y la app pide a Firebase el correo de restablecimiento, en español. |
| CA-SPK-02-02 | **Dado** el envío, **entonces** la misma pantalla muestra «Revisa tu correo», con un texto neutro que no revela si el correo tiene cuenta (como CA-REC-01-02), sin indicador de pasos. |
| CA-SPK-02-03 | **Dado** «Revisa tu correo», **entonces** «Reenviar enlace» se habilita a los 60 s con cuenta regresiva y «Volver a Iniciar sesión» vuelve al Login. |
| CA-SPK-02-04 | **Dado** el enlace del correo, **cuando** creo la contraseña nueva en la página de Firebase, **entonces** puedo iniciar sesión con ella. |
| CA-SPK-02-05 | **Dado** el modo Blaze o el modo demo (`-PrecoveryDemo`), **entonces** el flujo sigue siendo el de 3 pasos con código. |

### HU-SPK-03: Cambiar el correo con un enlace
**Como** usuario **quiero** cambiar mi correo en el Perfil sin Cloud Functions **para** mantener mis datos al día.

| Código | Criterio |
|---|---|
| CA-SPK-03-01 | **Dado** el modo gratuito, **cuando** guardo un correo nuevo con mi contraseña actual, **entonces** Firebase envía un enlace de verificación al correo nuevo y no se abre el diálogo del código. |
| CA-SPK-03-02 | **Dado** el envío, **entonces** el aviso de guardado dice «Te enviamos un enlace a X. Tu correo cambia cuando lo abras.» y el Perfil sigue mostrando el correo anterior. |
| CA-SPK-03-03 | **Dado** que abrí el enlace, **cuando** vuelvo a entrar y abro el Perfil, **entonces** `users/{uid}.email` toma el correo nuevo (`SyncProfileEmailUseCase`). |
| CA-SPK-03-04 | **Dado** contraseña incorrecta, correo en uso o sin red, **entonces** se muestran los mismos errores de CA-PER-02-11. |

### HU-SPK-04: Verificar el correo al registrarse
**Como** usuario nuevo **quiero** recibir un correo para verificar mi cuenta **aunque** no haya correo de bienvenida.

| Código | Criterio |
|---|---|
| CA-SPK-04-01 | **Dado** el modo gratuito, **cuando** me registro, **entonces** la app pide a Firebase el correo de verificación estándar, en español, antes de cerrar la sesión del registro. |
| CA-SPK-04-02 | **Dado** que ese envío falla, **entonces** la cuenta queda creada y el registro termina igual (como CA-MAIL-01-03). |
| CA-SPK-04-03 | **Dado** el modo Blaze, **entonces** la app no envía nada: el correo de bienvenida lo envía `sendWelcomeEmail`. |

### HU-SPK-05: Fotos sin Storage
**Como** usuario **quiero** subir y ver las fotos de mis productos y de mi perfil **aunque** no haya Storage.

| Código | Criterio |
|---|---|
| CA-SPK-05-01 | **Dado** el modo gratuito, **cuando** agrego o cambio la foto de un producto, **entonces** se guarda como JPEG de menos de 900 KB en `users/{uid}/media/{mediaId}` y el producto la muestra. |
| CA-SPK-05-02 | **Dado** el modo gratuito, **cuando** cambio la foto del perfil, **entonces** se guarda igual y el Perfil la muestra. |
| CA-SPK-05-03 | **Dado** una foto reemplazada o un producto borrado, **entonces** la foto anterior se borra de `media` después de subir la nueva, como CA-INV-05-05. |
| CA-SPK-05-04 | **Dado** que no hay conexión, **entonces** la foto queda pendiente y se sube con red, como CA-INV-10-04. |
| CA-SPK-05-05 | **Dado** una foto que sigue en Storage, en el modo gratuito, **entonces** se muestra el marcador sin foto y la app no se cierra. Borrarla no hace nada en Storage. |
| CA-SPK-05-06 | **Dado** una foto guardada en `media`, **cuando** el modo vuelve a Blaze, **entonces** se sigue mostrando, porque su URL `smartfood-media://` indica de dónde leer. |

### HU-SPK-06: Migrar las fotos de Storage
**Como** Zhair **quiero** copiar a Firestore las fotos que ya están en Storage **para** que se sigan viendo sin Blaze.

| Código | Criterio |
|---|---|
| CA-SPK-06-01 | **Dado** el script `functions/scripts/migrar-storage-a-firestore.mjs` sin `--aplicar`, **entonces** solo lista lo que copiaría. |
| CA-SPK-06-02 | **Dado** `--aplicar`, **entonces** copia cada foto del perfil y del inventario a `media` y cambia su `photoPath` y su `photoUrl`. |
| CA-SPK-06-03 | **Dado** que se corre dos veces, **entonces** la segunda no copia nada (idempotente) y nunca borra Storage. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-SPK-01 | Interruptor `blaze_activo` en Remote Config, leído en `core/data` y expuesto al dominio como `BackendModeProvider`. | HU-SPK-01 |
| RF-SPK-02 | Cada repositorio afectado conserva su versión Blaze y suma una gratuita; la que se registra en Hilt delega según el modo. | HU-SPK-02 a 05 |
| RF-SPK-03 | Recuperación con `sendPasswordResetEmail` y pantalla «Revisa tu correo». | HU-SPK-02 |
| RF-SPK-04 | Cambio de correo con `verifyBeforeUpdateEmail` y aviso del enlace. | HU-SPK-03 |
| RF-SPK-05 | `sendEmailVerification` al registrarse en el modo gratuito. | HU-SPK-04 |
| RF-SPK-06 | Fotos en `users/{uid}/media/{mediaId}` (un documento por foto, `Blob` de 900 KB como máximo), leídas por Coil con el esquema `smartfood-media://`. | HU-SPK-05 |
| RF-SPK-07 | Script de migración de Storage a `media`, que se corre una vez antes de quitar la facturación. | HU-SPK-06 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-SPK-01 | Ninguna pantalla ni caso de uso conoce Remote Config: solo `core/data`. |
| RNF-SPK-02 | Costo $0 en Spark: lectura de Remote Config gratuita y fotos dentro de la cuota de Firestore (1 GiB). |
| RNF-SPK-03 | Las reglas de `media` solo permiten al dueño leer, crear y borrar JPEG de 900 KB como máximo, sin editar. |
| RNF-SPK-04 | Sin secretos nuevos en el APK (RNF-01). |
| RNF-SPK-05 | `functions/src` no cambia y sus pruebas siguen pasando. |

## 6. Fuera de alcance

- Detectar Blaze automáticamente: el modo lo decide solo el interruptor.
- Limpiar las fotos que quedan en Storage después de migrar.
- Aplicar la política de contraseñas de la app en la página de Firebase del enlace (depende de la configuración de
  Authentication; se revisa en la consola).
- Un backend propio o proveedores externos de almacenamiento.

## 7. Supuestos

- La protección contra la enumeración de correos de Authentication está **desactivada** desde el 2026-11-19 (decisión de
  Zhair). Con ella activa, `verifyBeforeUpdateEmail` responde éxito sin enviar nada cuando el correo nuevo ya tiene
  cuenta, y la app no podría cumplir CA-SPK-03-04. Consecuencia: se puede averiguar si un correo tiene cuenta, y el
  inicio de sesión con un correo inexistente vuelve a mostrar «No existe una cuenta registrada…». La recuperación sigue
  respondiendo igual exista o no la cuenta, porque `LinkPasswordRecoveryRepository` calla ese error (CA-REC-01-02).
- Las fotos comprimidas a 1080 px y JPEG 80 pesan unos 300 KB, así que caben en un solo documento.
- Zhair corre la migración con credenciales de administrador mientras Blaze sigue activo.

## 8. Fidelidad con Figma

- **Frames de referencia:** los de la recuperación de contraseña (spec 003) y Editar información (spec 008).
- **Interacciones:** las mismas de esos flujos. «Revisa tu correo» aparece con el fundido entre pasos del
  `AnimatedContent` existente.
- **Desviaciones** (aprobadas por Zhair el 2026-11-18):
  - La pantalla «Revisa tu correo» del modo gratuito no existe en el Figma: reutiliza el paso 1 de la recuperación
    con otro título, otro texto y sin indicador de pasos.
  - El aviso «Te enviamos un enlace a X» reemplaza al diálogo del código en el modo gratuito.

## 9. Volver a Blaze

1. Consola de Google Cloud → Facturación → vincular `smartfood-f62a1`.
2. Volver a guardar los secretos si se perdieron: `firebase functions:secrets:set SMTP_USER` y `SMTP_PASS`.
3. `firebase deploy --only functions,firestore:rules,storage`.
4. Remote Config → `blaze_activo = true` → Publicar.
5. En un teléfono, reabrir la app pasada una hora (o con un build debug) y probar la recuperación con código y una foto.

Las fotos guardadas en `media` durante el modo gratuito se siguen mostrando (CA-SPK-05-06); las nuevas vuelven a Storage.

## 10. Plan de entrega

- **Una rama:** `feature/012-modo-sin-facturacion`, con un commit por tarea de [`tasks.md`](./tasks.md).
- **Un PR en *draft*** hacia `master`. Las pruebas se corren en la ronda final de [`docs/pendientes.md`](../../docs/pendientes.md).
- **Features verticales:** interruptor (core), recuperación (auth), correo (perfil), registro (auth), fotos (core,
  inventario y perfil), reglas y migración.
