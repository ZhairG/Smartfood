# SmartFood

Aplicación Android para reducir el desperdicio de comida en los hogares. Une **inventario**, **fechas de vencimiento** y
**presupuesto**, y sobre ellos actúa un **agente de recomendación de recetas basado en utilidad**.

**Equipo:** Dariany Natalia Bernal Duarte · Laura Nikoll Malagón Gómez · Santiago Ardila Vargas · Zhair David Gómez Miranda
**Cursos:** Construcción de Aplicaciones Móviles · Inteligencia Artificial — UTADEO 2026-2

---

## Estado

| Tareas                              | Spec | Estado |
|-------------------------------------|---|---|
| 001 Autenticación                   | [`specs/001-autenticacion`](specs/001-autenticacion/spec.md) | ✅ Implementado (v1) |
| 002 Adaptabilidad de la UI          | [`specs/002-adaptabilidad-ui`](specs/002-adaptabilidad-ui/spec.md) | ✅ Implementado |
| 003 Recuperación de contraseña real | [`specs/003-recuperacion-real`](specs/003-recuperacion-real/spec.md) | ✅ Implementado y desplegado |
| 004 Correo de bienvenida            | [`specs/004-correo-registro`](specs/004-correo-registro/spec.md) | ✅ Implementado (falta desplegar) |
| 005 Documentos legales              | [`specs/005-documentos-legales`](specs/005-documentos-legales/spec.md) | ✅ Implementado (texto en vista previa) |
| 006 Inventario                      | — | Pendiente |
| 007 Catálogo de recetas             | — | Pendiente |
| 008 Agente recomendador             | — | Pendiente |

## Metodología: Spec Driven Development

El proyecto sigue **SDD**: ninguna funcionalidad se programa sin especificación.

```
specs/
├── constitution.md              Principios del proyecto (arquitectura, convenciones, Definition of Done)
└── 001-autenticacion/
    ├── spec.md                  QUÉ y POR QUÉ: historias de usuario y criterios Dado/Cuando/Entonces
    ├── plan.md                  CÓMO: arquitectura, contratos, modelo de datos y decisiones (ADR)
    └── tasks.md                 Tareas y matriz de trazabilidad criterio → prueba
```

Cada criterio de aceptación (p. ej. `CA-02-03`) tiene al menos una prueba que lo cita en su nombre.

## Arquitectura

**Clean Architecture + MVVM con UDF**, organizada **por features**:

```
com.smartfood.app/
├── core/
│   ├── common/          safeRunCatching (coroutines seguras)
│   ├── designsystem/    theme/ (tokens de Figma y FormMetrics), components/ (SmartFood*, AnchoredPopup), util/
│   ├── di/              CoreDataModule: FirebaseAuth, Firestore, Functions, DataStore (y emuladores en debug)
│   └── navigation/      AppNavHost, Routes type-safe, NavTransitions, ObserveEffects
└── feature/
    ├── splash/          Decide el destino inicial según la sesión
    ├── home/            Saludo y cierre de sesión (punto de entrada de las próximas features)
    ├── legal/           Términos y Política de privacidad (plantilla en vista previa, spec 005)
    └── auth/
        ├── domain/      model/ (User, AuthError), repository/ (interfaces), usecase/
        ├── data/        repository/ (Firebase y Cloud Functions), mapper/, local/ (DataStore), demo/, di/
        └── ui/          login/, register/, recovery/, components/, AuthErrorText, AuthNavGraph
```

- `ui → domain ← data`: el dominio es Kotlin puro y define las interfaces; la capa de datos las implementa.
- Un ViewModel por pantalla con `StateFlow<UiState>`, eventos `onEvent(Event)` y efectos de un solo uso para navegar.
- Errores tipados (`AuthError`) en lugar de comparar textos de excepciones.

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · Hilt · Navigation Compose type-safe · Coroutines/Flow ·
Firebase Authentication · Cloud Firestore · Cloud Functions (TypeScript) · DataStore · JUnit4 · Compose UI Test

El diseño de referencia está en Figma (archivo `z1AoQvluQcmx98gjP4vHF6`, frames "_Claro"). La app usa la
tipografía Poppins, incluida en el APK con licencia OFL (`docs/licenses/OFL-Poppins.txt`).

## Puesta en marcha

1. Descargar `google-services.json` desde la consola de Firebase (Configuración del proyecto → app Android
   `com.smartfood.app`) y copiarlo en `app/`. **No se versiona**: está en `.gitignore` y se comparte solo
   por canales privados.
2. Abrir el proyecto en Android Studio y sincronizar Gradle.
3. En la consola de Firebase (`smartfood-f62a1`):
   - **Authentication** → habilitar *Correo/contraseña*.
   - **Firestore Database** → crear la base de datos.
   - Publicar las reglas: `firebase deploy --only firestore:rules`.
4. Ejecutar la configuración `app`.

La recuperación de contraseña y el correo de bienvenida necesitan además las Cloud Functions desplegadas (ver la
sección siguiente). Sin ellas, usa el modo demo o los emuladores.

## Documentos legales

Los Términos y condiciones y la Política de privacidad ([spec 005](specs/005-documentos-legales/spec.md)) se abren desde
el registro. Hoy son una **plantilla en vista previa**. Para publicar el texto definitivo:

1. Reemplazar los textos `[Pendiente de redacción…]` de `app/src/main/res/values/strings_legal.xml` (y agregar o quitar
   secciones en `feature/legal/ui/LegalContent.kt` si cambia la estructura).
2. Cambiar `LegalVersion.CURRENT` y poner `IS_PREVIEW = false` en `feature/legal/domain/model/LegalDocument.kt`.

Cada perfil guarda la versión aceptada (`termsVersion`) y la fecha del servidor (`termsAcceptedAt`).

## Ícono de la app

El ícono oficial (adaptativo, monocromático, Android 7.x y Play Store) se genera desde
`app/src/main/res/drawable-nodpi/logo_smartfood_circle.png`. Si cambia el logo, regenéralo:

```bash
pip install pillow
python tools/generate_launcher_icon.py
```

El script verifica que el logo quede dentro de la zona segura de 66 dp. No edites los `mipmap-*` a mano.

## Cloud Functions: recuperación de contraseña y correo de bienvenida

Las funciones de `functions/` (región `southamerica-east1`) envían correos por Gmail. Las credenciales de Gmail viven
en Secret Manager, nunca en el repositorio ni en el APK.

- **Recuperación real** ([spec 003](specs/003-recuperacion-real/spec.md)): tres funciones callable envían un código de
  4 dígitos y cambian la contraseña con el Admin SDK.
- **Bienvenida** ([spec 004](specs/004-correo-registro/spec.md)): `sendWelcomeEmail` se dispara al crearse
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
   4 de enero de 2027 (ver [plan 001 §6.5](specs/001-autenticacion/plan.md#65-ruta-a-una-recuperación-real-post-entrega)).

### Desarrollo local con la Emulator Suite

```bash
cd functions && npm ci && npm test            # pruebas del servidor
firebase emulators:start --only auth,firestore,functions
./gradlew :app:installDebug -PuseFirebaseEmulator=true
```

- El código de recuperación **no se envía**: aparece en la consola de los emuladores como
  `[EMULADOR] Código de recuperación para …`.
- El correo de bienvenida tampoco se envía: el enlace de verificación aparece como `[EMULADOR] Correo de bienvenida
  para …`. Al abrirlo, la cuenta queda verificada en el emulador de Auth.
- Crea `functions/.secret.local` (ignorado por git) con `SMTP_USER=x` y `SMTP_PASS=x` para que el emulador no
  consulte Secret Manager.
- En un teléfono físico: `adb reverse tcp:9099 tcp:9099`, lo mismo para los puertos 8080 y 5001, y
  `-PfirebaseEmulatorHost=127.0.0.1`.
- Las cuentas que crees en el emulador no existen en el proyecto real.

### Modo demo (sin backend)

`./gradlew :app:installDebug -PrecoveryDemo=true` usa la recuperación simulada de la v1: no envía correos ni cambia la
contraseña. Solo existe en debug; release usa siempre la versión real.

| Dato | Valor |
|---|---|
| Correos con cuenta | `demo@smartfood.com`, `prueba@smartfood.com` |
| Código de verificación | `1234` |

## Comandos

```bash
./gradlew :app:assembleDebug              # Compilar APK debug
./gradlew :app:testDebugUnitTest          # Pruebas unitarias
./gradlew :app:connectedDebugAndroidTest  # Pruebas instrumentadas (requiere emulador)
./gradlew :app:lintDebug                  # Android Lint
npm --prefix functions test               # Pruebas de las Cloud Functions
```
