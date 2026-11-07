# Arquitectura de SmartFood

Cómo está organizada la app (Clean Architecture con MVVM y flujo unidireccional) y dónde va el código de cada funcionalidad. Resumen en el [README](../README.md#estructura).

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
[`LoginContract.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginContract.kt)):

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
    ├── profile/         Perfil del Figma: datos de la cuenta y "Cerrar sesión" (avisa si hay cambios sin subir)
    ├── recipes/         Recetas + IA (spec 009): catálogo propio, agente CSP, flujo de la IA y cocina guiada
    │   ├── domain/      agent/ (MealPlanner, RecipeEvaluator, PlanExplainer, IngredientMatcher), model/, usecase/
    │   ├── data/        catalog/ (JSON de assets/), repository/ (historial de cocinadas), di/
    │   └── ui/          discover/, detail/, ai/ (flujo de la IA y Ayuda), cooking/ (guía y temporizador), components/
    ├── inventory/       Inventario (spec 006): lista por vencimiento, búsqueda por texto y voz, editor con foto
    │   ├── domain/      model/ (InventoryItem, WriteOutcome), repository/ (InventoryRepository, PhotoSync…), usecase/
    │   ├── data/        repository/ (Firestore y Storage), sync/ (fotos con WorkManager), reminder/ (aviso diario),
    │   │                speech/, photo/ (compresión), di/
    │   └── ui/          list/, search/, editor/, camera/, components/, InventoryNavGraph
    └── auth/
        ├── domain/      model/ (User, AuthError), repository/ (interfaces), usecase/
        ├── data/        repository/ (Firebase y Cloud Functions), mapper/, local/ (DataStore), demo/, di/
        └── ui/          login/, register/, recovery/, components/, AuthErrorText, AuthNavGraph
```

### ¿Dónde va el código de una feature nueva?

1. Escribir `specs/NNN-nombre/spec.md`, `plan.md` y `tasks.md` (ver [Metodología](metodologia.md#metodología-spec-driven-development)).
2. Crear `feature/<nombre>/domain` con los modelos, las interfaces de repositorio y los UseCases.
3. Implementar las interfaces en `feature/<nombre>/data` y enlazarlas en un módulo de Hilt.
4. En `feature/<nombre>/ui`, por cada pantalla: `XContract.kt`, `XViewModel.kt` y una `XScreen` stateless.
5. Registrar las rutas con una extensión de `NavGraphBuilder` (como `authGraph()` o `mainGraph()`).
6. Escribir las pruebas citando el criterio que cubren, por ejemplo `` `CA-02-03 credenciales incorrectas…` ``.

Las reglas completas están en [`specs/constitution.md`](../specs/constitution.md).


## Tecnologías

Kotlin · Jetpack Compose (Material 3) · Hilt · Navigation Compose type-safe · Coroutines/Flow ·
Firebase Authentication · Cloud Firestore · Cloud Functions (TypeScript) · DataStore · JUnit4 · Compose UI Test

El diseño de referencia está en Figma (archivo `z1AoQvluQcmx98gjP4vHF6`, frames "_Claro"). La app usa la
tipografía Poppins, incluida en el APK con licencia OFL (`docs/licenses/OFL-Poppins.txt`).


## Documentos legales

Los Términos y condiciones y la Política de privacidad ([spec 005](../specs/005-documentos-legales/spec.md)) se abren desde
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
