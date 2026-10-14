# SmartFood

Aplicación Android para reducir el desperdicio de comida en los hogares. Une **inventario**, **fechas de vencimiento** y
**presupuesto**, y sobre ellos actúa un **agente de recomendación de recetas basado en utilidad**.

**Equipo:** Dariany Natalia Bernal Duarte · Laura Nikoll Malagón Gómez · Santiago Ardila Vargas · Zhair David Gómez Miranda
**Cursos:** Construcción de Aplicaciones Móviles · Inteligencia Artificial — UTADEO 2026-2

---

## ⚠️ Primero: configurar tu equipo (obligatorio al clonar o hacer pull)

Haz estos dos pasos **antes de ejecutar la app** en un computador nuevo y cada vez que cambie la configuración de
Firebase. Si no los haces, la app compila y abre, pero el registro y el login fallan con *"Ocurrió un error
inesperado"*.

### 1. Pedir el `google-services.json` vigente

El archivo `app/google-services.json` **no está en el repositorio** (está en `.gitignore`), así que `git pull` no lo
trae ni lo actualiza.

- La única API key válida es la que administra **Zhair** en Google Cloud Console (proyecto `smartfood-f62a1`).
  Pídele a él el `google-services.json` actual por un canal privado y cópialo en `app/`.
- No uses copias viejas ni archivos de otro proyecto. Una key vencida o distinta produce:
  `API key expired. Please renew the API key.`
- Después de reemplazarlo: **Build → Clean Project** y volver a ejecutar.

### 2. Registrar la huella SHA-1 de tu computador

La API key solo acepta peticiones de apps Android autorizadas (paquete `com.smartfood.app` + huella SHA-1). Cada
computador firma el APK de debug con su propio `~/.android/debug.keystore`, así que **cada integrante tiene una SHA-1
distinta** y debe registrarla. Si falta, aparece:
`Requests from this Android client application com.smartfood.app are blocked.`

1. En la raíz del proyecto, ejecuta:

   ```bash
   ./gradlew :app:signingReport        # En Windows: gradlew.bat :app:signingReport
   ```

   Copia la línea `SHA1:` del bloque `Variant: debug`. La huella no es un secreto.
2. Envíasela a Zhair. Él la agrega en dos lugares:
   - **Google Cloud Console** → *APIs y servicios → Credenciales* → la API key de Android → *Restricciones de
     aplicación* → **Agregar** `com.smartfood.app` + la SHA-1. Este paso es el que quita el bloqueo.
   - **Firebase Console** → *Configuración del proyecto → Tus apps → Android* → **Agregar huella digital**.
3. Espera unos 5 minutos a que se propague el cambio. No hace falta recompilar.

Si cambias de computador o borras `~/.android/debug.keystore`, tu SHA-1 cambia: repite el paso 2.

### ¿Cómo sé cuál de los dos falla?

En Logcat, filtra por `package:com.smartfood.app level:error`. El mensaje que sale con la etiqueta
`RecaptchaCallWrapper` indica la causa:

| Mensaje en Logcat | Causa | Solución |
|---|---|---|
| `API key expired. Please renew the API key.` | `google-services.json` viejo o de otra fuente | Paso 1 |
| `Requests from this Android client application com.smartfood.app are blocked.` | SHA-1 de tu computador sin registrar | Paso 2 |

El conflicto en archivos de `.idea/` (como `deploymentTargetSelector.xml`) **no** tiene relación con estos errores:
solo guardan preferencias de Android Studio.

## Estado

| Tareas                              | Spec | Estado |
|-------------------------------------|---|---|
| 001 Autenticación                   | [`specs/001-autenticacion`](specs/001-autenticacion/spec.md) | ✅ Implementado (v1) |
| 002 Adaptabilidad de la UI          | [`specs/002-adaptabilidad-ui`](specs/002-adaptabilidad-ui/spec.md) | ✅ Implementado |
| 003 Recuperación de contraseña real | [`specs/003-recuperacion-real`](specs/003-recuperacion-real/spec.md) | ✅ Implementado y desplegado |
| 004 Correo de bienvenida            | [`specs/004-correo-registro`](specs/004-correo-registro/spec.md) | ✅ Implementado y desplegado |
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

SmartFood tiene dos partes: la **app Android** (Kotlin + Jetpack Compose) y un **backend en Firebase**. La app guarda
las cuentas en Firebase Authentication y los perfiles en Cloud Firestore. Lo que no es seguro hacer desde un teléfono
(enviar correos, cambiar la contraseña de una cuenta) lo hacen nuestras **Cloud Functions**, escritas en TypeScript.

```
 APP ANDROID                            GOOGLE CLOUD / FIREBASE

 ┌────────────────────────────┐
 │ ui                         │
 │ pantallas y ViewModels     │
 └──────────────┬─────────────┘
                │ llama a
                ▼
 ┌────────────────────────────┐
 │ domain                     │
 │ reglas de negocio          │
 │ Kotlin puro, sin Firebase  │
 └────────────────────────────┘
                ▲
                │ implementa
 ┌──────────────┴─────────────┐         ┌────────────────────────────────┐
 │ data                       │    ┌───▶│ Firebase Authentication        │
 │ Firebase y DataStore       ├────┤    │ cuentas y sesiones             │
 └────────────────────────────┘    │    └────────────────────────────────┘
                                   │    ┌────────────────────────────────┐
                                   ├───▶│ Cloud Firestore                │
                                   │    │ perfiles en users/{uid}        │
                                   │    └────────────────┬───────────────┘
                                   │                     │ al crear un perfil
                                   │                     ▼
                                   │    ┌────────────────────────────────┐
                                   └───▶│ Cloud Functions (TypeScript)   │
                                        │ recuperación y bienvenida      │
                                        └────────────────┬───────────────┘
                                                         │ envía correos
                                                         ▼
                                        ┌────────────────────────────────┐
                                        │ Gmail (SMTP)                   │
                                        │ credenciales en Secret Manager │
                                        └────────────────────────────────┘
```

### Las tres capas de la app

El código de cada funcionalidad se divide en tres capas, cada una con un solo trabajo (**Clean Architecture**):

| Capa | Qué contiene | Qué **no** puede conocer | Ejemplo |
|---|---|---|---|
| `ui` | Pantallas de Compose y un ViewModel por pantalla | Firebase | `LoginScreen`, `LoginViewModel` |
| `domain` | Modelos, reglas de negocio (UseCases) e **interfaces** de repositorio. Kotlin puro | Android, Firebase, Compose | `LoginUseCase`, `AuthRepository` |
| `data` | Las implementaciones de esas interfaces | Las pantallas | `FirebaseAuthRepository` |

Las dependencias apuntan siempre hacia el dominio: `ui → domain ← data`. El dominio no sabe que existe Firebase; solo
conoce interfaces como `AuthRepository`. Gracias a eso:

- Las reglas se prueban sin internet, pasándole al UseCase un repositorio falso (*fake*).
- Cambiar de backend es cambiar qué implementación se entrega, sin tocar pantallas ni reglas.

### Ejemplo completo: el usuario toca "Iniciar sesión"

1. `LoginScreen` no decide nada: solo envía el evento `LoginEvent.LoginClicked` al ViewModel.
2. `LoginViewModel` pone `isLoading = true` en el estado y llama a `LoginUseCase`.
3. `LoginUseCase` valida los datos (campos vacíos, formato del correo) y llama a `AuthRepository.login(...)`.
4. `FirebaseAuthRepository`, la implementación real, habla con Firebase Authentication y devuelve un `Result`.
5. Si todo salió bien, el UseCase guarda "Recuérdame" y el ViewModel emite `LoginEffect.NavigateToHome`. Si falló, el
   ViewModel guarda el error en `LoginUiState` y la pantalla se redibuja con el mensaje y los campos en rojo.

Este patrón es **MVVM con flujo unidireccional (UDF)**: los eventos suben de la pantalla al ViewModel y el estado baja
del ViewModel a la pantalla. Cada pantalla define su "contrato" en un archivo `XContract.kt` (ver
[`LoginContract.kt`](app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginContract.kt)):

- **`UiState`**: todo lo que la pantalla necesita para dibujarse. Es inmutable y se expone como `StateFlow`.
- **`Event`**: lo que hace el usuario (escribir, tocar un botón).
- **`Effect`**: acciones que ocurren una sola vez, como navegar. No van en el estado para que no se repitan al rotar
  el teléfono.

Las pantallas son *stateless*: reciben el estado y funciones, nunca el ViewModel. Por eso se prueban con un estado
construido a mano.

### Errores con nombre propio

En lugar de comparar mensajes de texto, cada fallo es un caso del tipo `AuthError` (por ejemplo `InvalidCredentials`
o `Network`):

```
excepción de Firebase ──▶ FirebaseErrorMapper ──▶ AuthError ──▶ AuthErrorText ──▶ texto de strings.xml
     (capa data)              (capa data)          (domain)        (capa ui)
```

Cada traducción vive en un solo lugar. Para agregar un error: nuevo caso en `AuthError` → mapper (si viene de Firebase)
→ `AuthErrorText` → `strings.xml`.

### El backend (`functions/`)

Las Cloud Functions siguen la misma idea de capas, con el nombre de **puertos y adaptadores**:

| Archivo | Papel |
|---|---|
| `ports.ts` | Interfaces: almacén de códigos, directorio de usuarios, envío de correos, reloj |
| `recoveryService.ts`, `registrationService.ts` | Lógica pura, probada con implementaciones en memoria |
| `adapters.ts` | Único archivo que conoce Firebase Admin y Gmail |
| `index.ts` | Publica las 4 funciones |

- La app llama a tres funciones *callable* para recuperar la contraseña: `requestPasswordResetCode`,
  `verifyPasswordResetCode` y `resetPasswordWithCode`.
- `sendWelcomeEmail` no la llama nadie: se dispara sola cuando se crea un perfil en `users/{uid}`.
- Las colecciones `passwordResets` y `welcomeEmails` solo las usa el servidor; las reglas de Firestore niegan todo
  acceso a los clientes.

### Inyección de dependencias (Hilt)

Hilt entrega a cada clase lo que necesita. `core/di/CoreDataModule` crea los clientes de Firebase y el DataStore, y
`feature/auth/data/di/AuthDataModule` decide qué implementación recibe cada interfaz (por ejemplo, `AuthRepository` →
`FirebaseAuthRepository`). El modo demo funciona así: con `-PrecoveryDemo=true`, ese módulo entrega
`DemoPasswordRecoveryRepository` en lugar de la recuperación real, y ninguna pantalla cambia.

### Mapa de carpetas

```
com.smartfood.app/
├── core/                compartido por todas las features
│   ├── common/          safeRunCatching (manejo de errores seguro con coroutines)
│   ├── designsystem/    theme/ (colores, tipografía, medidas), components/ (SmartFood*), util/
│   ├── di/              CoreDataModule: Firebase y DataStore (y emuladores en debug)
│   └── navigation/      AppNavHost, rutas type-safe, transiciones, ObserveEffects
└── feature/             una carpeta por funcionalidad
    ├── splash/          Decide si ir a Home o al Login según la sesión
    ├── home/            Saludo y cierre de sesión (punto de entrada de las próximas features)
    ├── legal/           Términos y Política de privacidad (spec 005)
    └── auth/
        ├── domain/      model/ (User, AuthError), repository/ (interfaces), usecase/
        ├── data/        repository/ (Firebase y Cloud Functions), mapper/, local/ (DataStore), demo/, di/
        └── ui/          login/, register/, recovery/, components/, AuthErrorText, AuthNavGraph
```

### ¿Dónde va el código de una feature nueva?

1. Escribir `specs/NNN-nombre/spec.md`, `plan.md` y `tasks.md` (ver [Metodología](#metodología-spec-driven-development)).
2. Crear `feature/<nombre>/domain` con los modelos, las interfaces de repositorio y los UseCases.
3. Implementar las interfaces en `feature/<nombre>/data` y enlazarlas en un módulo de Hilt.
4. En `feature/<nombre>/ui`, por cada pantalla: `XContract.kt`, `XViewModel.kt` y una `XScreen` stateless.
5. Registrar las rutas con una extensión de `NavGraphBuilder` (como `authGraph()` o `mainGraph()`).
6. Escribir las pruebas citando el criterio que cubren, por ejemplo `` `CA-02-03 credenciales incorrectas…` ``.

Las reglas completas están en [`specs/constitution.md`](specs/constitution.md).

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · Hilt · Navigation Compose type-safe · Coroutines/Flow ·
Firebase Authentication · Cloud Firestore · Cloud Functions (TypeScript) · DataStore · JUnit4 · Compose UI Test

El diseño de referencia está en Figma (archivo `z1AoQvluQcmx98gjP4vHF6`, frames "_Claro"). La app usa la
tipografía Poppins, incluida en el APK con licencia OFL (`docs/licenses/OFL-Poppins.txt`).

## Puesta en marcha

1. Completar la sección **"Primero: configurar tu equipo"** del inicio de este README: `google-services.json`
   vigente en `app/` y SHA-1 registrada.
2. Abrir el proyecto en Android Studio y sincronizar Gradle.
3. Solo una vez por proyecto (ya está hecho en `smartfood-f62a1`), en la consola de Firebase:
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
