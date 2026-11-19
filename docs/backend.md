# Backend: Cloud Functions, despliegue y emuladores

El backend vive en Firebase. Esta guía explica las Cloud Functions, cómo desplegarlas, cómo trabajar sin tocar el proyecto real (Emulator Suite) y el modo demo.

## Cloud Functions: recuperación de contraseña y correo de bienvenida

Las funciones de `functions/` (región `southamerica-east1`) envían correos por Gmail. Las credenciales de Gmail viven
en Secret Manager, nunca en el repositorio ni en el APK.

- **Recuperación real** ([spec 003](../specs/003-recuperacion-real/spec.md)): tres funciones callable envían un código de
  4 dígitos y cambian la contraseña con el Admin SDK.
- **Bienvenida** ([spec 004](../specs/004-correo-registro/spec.md)): `sendWelcomeEmail` se dispara al crearse
  `users/{uid}` y envía un único correo de bienvenida con el enlace de verificación (reemplaza el correo genérico de
  Firebase). Se envía como máximo uno por cuenta (`welcomeEmails/{uid}`). Conviene configurar en español la página de
  acción (consola → Authentication → Plantillas → idioma).

### Despliegue (una vez por proyecto, requiere el plan Blaze)

1. En la cuenta de Gmail del equipo, activar la verificación en 2 pasos y crear una **contraseña de aplicación**
   (Cuenta de Google → Seguridad → Contraseñas de aplicaciones).
2. Guardar los secretos (los pide por consola, no quedan en ningún archivo):
   ```bash
   firebase functions:secrets:set SMTP_USER   # correo de Gmail, p. ej. smartfood.equipo@gmail.com
   firebase functions:secrets:set SMTP_PASS   # contraseña de aplicación de 16 caracteres
   ```
3. Desplegar las funciones y las reglas, y configurar la limpieza de imágenes antiguas (evita pequeños cobros):
   ```bash
   cd functions && npm ci && cd ..
   firebase deploy --only functions,firestore:rules   # la primera vez habilita Eventarc (disparador de Firestore)
   firebase functions:artifacts:setpolicy --location southamerica-east1 --force
   ```
4. Recomendado: una alerta de presupuesto de 1 USD en Google Cloud Billing. La prueba gratuita de Blaze vence el
   4 de enero de 2027 (ver [plan 001 §6.5](../specs/001-autenticacion/plan.md#65-ruta-a-una-recuperación-real-post-entrega)).

### Desarrollo local con la Emulator Suite

```bash
cd functions && npm ci && npm test            # pruebas del servidor
firebase emulators:start --only auth,firestore,functions,storage
./gradlew :app:installDebug -PuseFirebaseEmulator=true
```

- El inventario usa Firestore y Storage: con `storage` en la lista, las fotos se suben al emulador y se ven en su UI.
- Las reglas se prueban con `firebase emulators:exec --only firestore,storage "npm --prefix rules-test test"`
  (antes, una vez: `npm --prefix rules-test ci`).

- El código de recuperación **no se envía**: aparece en la consola de los emuladores como
  `[EMULADOR] Código de recuperación para …`.
- El correo de bienvenida tampoco se envía: el enlace de verificación aparece como `[EMULADOR] Correo de bienvenida
  para …`. Al abrirlo, la cuenta queda verificada en el emulador de Auth.
- Crea `functions/.secret.local` (ignorado por git) con `SMTP_USER=x` y `SMTP_PASS=x` para que el emulador no
  consulte Secret Manager.
- En un teléfono físico: `adb reverse tcp:9099 tcp:9099`, lo mismo para los puertos 8080, 9199 y 5001, y
  `-PfirebaseEmulatorHost=127.0.0.1`.
- Las cuentas que crees en el emulador no existen en el proyecto real.

### Modo demo (sin backend)

`./gradlew :app:installDebug -PrecoveryDemo=true` usa la recuperación simulada de la v1: no envía correos ni cambia la
contraseña. Solo existe en debug; release usa siempre la versión real.

| Dato | Valor |
|---|---|
| Correos con cuenta | `demo@smartfood.com`, `prueba@smartfood.com` |
| Código de verificación | `1234` |

## Modo sin facturación (spec 012)

El proyecto deja el plan Blaze en noviembre de 2026. En el plan Spark no funcionan las Cloud Functions ni Storage, así
que la app tiene un modo gratuito que se elige con el parámetro **`blaze_activo`** de Remote Config:

| `blaze_activo` | Recuperación | Cambio de correo | Al registrarse | Fotos |
|---|---|---|---|---|
| `true` (Blaze) | Código por las funciones (spec 003) | Código por las funciones (spec 008) | Correo de bienvenida (spec 004) | Storage |
| `false` o sin leer | Enlace de Firebase | Enlace de Firebase al correo nuevo | Correo de verificación de Firebase | `users/{uid}/media` en Firestore |

- **Cambiar el modo:** consola de Firebase → Remote Config → `blaze_activo` → Publicar. La app lo toma al abrirse,
  como tarde una hora después (en debug, en cada apertura).
- **Probar en debug:** `./gradlew :app:installDebug -PbackendMode=free` (o `blaze`) fuerza el modo sin tocar la consola.
- **Plantillas de Authentication:** los correos del enlace usan las plantillas de Authentication → Plantillas, en
  español.
- **Protección contra la enumeración de correos:** desactivada (Authentication → Configuración). Con ella activa,
  Firebase no avisa que el correo nuevo ya tiene cuenta y no envía el enlace, pero la app diría que sí lo envió
  (spec 012 §7, CA-SPK-03-04).

### Antes de quitar la facturación: migrar las fotos

Con Blaze todavía activo, copia las fotos de Storage a Firestore. Necesita credenciales de administrador del proyecto:

```bash
gcloud auth application-default login
node functions/scripts/migrar-storage-a-firestore.mjs             # simulación: solo lista
node functions/scripts/migrar-storage-a-firestore.mjs --aplicar   # copia y cambia photoPath y photoUrl
```

Es idempotente y no borra nada de Storage. Las fotos de más de 900 KB se saltan con un aviso. Después, publica las
reglas (`firebase deploy --only firestore:rules`) y deja `blaze_activo = false`.

### Volver a Blaze

1. Consola de Google Cloud → Facturación → vincular `smartfood-f62a1`.
2. Volver a guardar los secretos si se perdieron: `firebase functions:secrets:set SMTP_USER` y `SMTP_PASS`.
3. `firebase deploy --only functions,firestore:rules,storage`.
4. Remote Config → `blaze_activo = true` → Publicar.
5. En un teléfono, reabrir la app pasada una hora (o con un build debug) y probar la recuperación con código y una foto.

Las fotos guardadas en `media` se siguen mostrando, porque su URL `smartfood-media://` dice de dónde leerlas.
