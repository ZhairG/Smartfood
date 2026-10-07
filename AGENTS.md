# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) or others when working with code in this repository.

## Proyecto

App Android (Kotlin + Jetpack Compose, Material 3) de un solo módulo `:app`, paquete `com.smartfood.app`. Proyecto universitario (UTADEO 2026-2) para reducir el desperdicio de comida en hogares: inventario + vencimientos + presupuesto, con un agente de recomendación de recetas (features futuras). Hoy está implementado el **módulo 1: autenticación**.

`compileSdk`/`targetSdk` 37, `minSdk` 24, Java 11. Todas las dependencias van en `gradle/libs.versions.toml`.

## Flujo de trabajo: Spec Driven Development (obligatorio)

Antes de escribir código, leer **`specs/constitution.md`** (principios no negociables) y la spec de la feature.

- Specs actuales: `001-autenticacion`, `002-adaptabilidad-ui`, `003-recuperacion-real`, `004-correo-registro` y `005-documentos-legales`. A partir de la 002, los códigos llevan prefijo (`CA-UI-01-01`, `CA-REC-01-01`, `CA-MAIL-01-01`, `CA-LEG-01-01`).
- Cada feature vive en `specs/NNN-nombre/` con `spec.md` (QUÉ: historias, criterios Dado/Cuando/Entonces), `plan.md` (CÓMO: contratos, decisiones) y `tasks.md` (tareas + matriz CA → test).
- Una feature nueva empieza creando su `spec.md`; el código viene después. Si el código y la spec discrepan, se corrige primero la spec.
- Códigos de trazabilidad: `HU-NN`, `CA-NN-MM`, `RF-XXX-NN`, `RNF-NN`. Los nombres de los tests citan el CA que cubren.
- Al revisar comentarios `TODO`, implementarlos aplicando MVVM según la constitución.

## Comandos

En Windows usar `gradlew.bat` (o `./gradlew` desde Git Bash).

```bash
./gradlew :app:assembleDebug                 # Compilar APK debug
./gradlew :app:testDebugUnitTest             # Pruebas unitarias (JVM)
./gradlew :app:testDebugUnitTest --tests "com.smartfood.app.feature.auth.ui.login.LoginViewModelTest"   # Una clase
./gradlew :app:testDebugUnitTest --tests "*LoginViewModelTest.CA-02-03*"                                # Un test por su CA
./gradlew :app:connectedDebugAndroidTest     # Pruebas instrumentadas (requiere emulador/dispositivo)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartfood.app.feature.auth.ui.login.LoginScreenTest
./gradlew :app:lintDebug                     # Android Lint (no hay ktlint/detekt configurados)
firebase deploy --only firestore:rules       # Publicar firestore.rules
npm --prefix functions test                  # Compilar y probar las Cloud Functions (node:test)
firebase emulators:start --only auth,firestore,functions   # Emulator Suite; la app con -PuseFirebaseEmulator=true
firebase deploy --only functions             # Desplegar las funciones (requiere los secretos SMTP_USER y SMTP_PASS)
```

El build `release` usa R8 en modo completo; los cambios con reflexión/serialización deben revisarse contra `app/proguard-rules.pro`.

## Arquitectura

Paquetes **por feature** (`feature/<nombre>/{domain,data,ui}`) más `core/` compartido. Dependencias: `ui → domain ← data`.

- **`domain`** es Kotlin puro: modelos (`User`, `AuthError` sealed, `AuthField`) e **interfaces** de repositorio (`AuthRepository`, `UserRepository`, `PasswordRecoveryRepository`, `SessionPreferences`). Las validaciones de negocio viven **solo** en los UseCases; los ViewModels no las repiten.
- **Errores tipados:** todo devuelve `Result<T>`; un fallo lleva `AuthException(AuthError)` y se lee con `Throwable.toAuthError()`. `data/mapper/FirebaseErrorMapper` es el único lugar que conoce excepciones de Firebase. `ui/AuthErrorText.kt` es el único lugar que traduce `AuthError` a `strings.xml`. Para agregar un error: nuevo caso en `AuthError` → mapper (si viene de Firebase) → `AuthErrorText` → `strings.xml`.
- **`data`**: `FirebaseAuthRepository`, `FirestoreUserRepository` (perfil en `users/{uid}`, escrito con `Map` para no depender de reflexión) y `DataStoreSessionPreferences` ("Recuérdame"). Usar `safeRunCatching` (`core/common`) + `.mapFirebaseError()`, nunca `runCatching` (se traga `CancellationException`).
- **Recuperación de contraseña real (spec 003):** `data/repository/RemotePasswordRecoveryRepository` llama a 3 funciones callable de `functions/` (TypeScript, región `southamerica-east1`): `requestPasswordResetCode`, `verifyPasswordResetCode` y `resetPasswordWithCode`.
  - Los datos viajan como `Map`. Los errores llegan con `details.reason` y `FirebaseErrorMapper.mapFunctionsError` los clasifica.
  - Responde igual exista o no la cuenta: no hay error de "cuenta no encontrada" en la recuperación.
  - En el servidor, la lógica vive en `functions/src/recoveryService.ts`, sin Firebase y probada con fakes. Firebase y Gmail solo aparecen en `adapters.ts`. Los secretos `SMTP_USER` y `SMTP_PASS` están en Secret Manager.
  - La colección `passwordResets` es solo del servidor; las reglas niegan todo acceso a los clientes.
  - **Modo demo:** con `-PrecoveryDemo=true` (solo debug), `AuthDataModule` enlaza `data/demo/DemoPasswordRecoveryRepository` (correos `demo@smartfood.com` y `prueba@smartfood.com`; código `1234`).
  - Con `-PuseFirebaseEmulator=true` (solo debug), `CoreDataModule` conecta Auth, Firestore y Functions a la Emulator Suite. En el emulador, el código se lee en el log.
- **Orquestación del registro** en `RegisterUseCase`: crear cuenta → perfil Firestore (si falla, rollback con `deleteCurrentAccount`) → `logout`. La app **no** pide el correo de verificación de Firebase.
- **Correo de bienvenida (spec 004):** la función `sendWelcomeEmail` (`onDocumentCreated("users/{uid}")`, sin reintentos) envía un único correo con el nombre y el enlace de verificación (`generateEmailVerificationLink`).
  - La lógica vive en `functions/src/registrationService.ts` (`WelcomeService`, probado con fakes) y las plantillas en `emailTemplates.ts` (puras, con `escapeHtml`).
  - El destinatario sale siempre de Auth, nunca del perfil. `welcomeEmails/{uid}` (solo servidor) garantiza un correo por cuenta; si el envío falla, se libera.
  - En el emulador, el enlace se lee en el log (`[EMULADOR] Correo de bienvenida…`).
- **UI (MVVM + UDF):** un ViewModel por pantalla. Cada pantalla tiene un `XContract.kt` con `XUiState` (los campos en rojo se **derivan** de `error`), `XEvent` y `XEffect`. Los efectos de un solo uso (navegación) salen por `Channel` → `Flow` y se recolectan con `ObserveEffects`. Patrón Route/Screen: los `*NavGraph.kt` obtienen el ViewModel con `hiltViewModel()` y llaman a la `XScreen` **stateless**, que es la que se prueba.
- **Documentos legales (spec 005, `feature/legal`):** `LegalDocumentScreen` (stateless, sin ViewModel) muestra los Términos o la Política según `LegalDocumentType`.
  - El texto vive en `res/values/strings_legal.xml` y `ui/LegalContent.kt` es el único mapeo tipo → recursos. Hoy es una plantilla: al publicar el texto definitivo se cambian `LegalVersion.CURRENT` e `IS_PREVIEW`.
  - Las rutas `TermsOfService` y `PrivacyPolicy` se registran en la raíz de `AppNavHost` con `legalDestinations()`.
  - `SmartFoodTermsCheckbox` resuelve el toque por carácter (enlace → abre el documento; resto → alterna) y ofrece los enlaces como `customActions` para TalkBack.
  - Constancia: `UserRepository.createProfile(user, acceptedTermsVersion)` guarda `termsVersion` y `termsAcceptedAt` (hora del servidor); las reglas lo exigen al crear `users/{uid}`.
- **Navegación (`core/navigation`):** `AppNavHost` arranca en `Splash`; `SplashViewModel` usa `ResolveSessionUseCase` para ir a `MainGraph` (Home) o a `AuthGraph` (Login). `authGraph()` y `mainGraph()` son extensiones de `NavGraphBuilder` en cada feature. Las transiciones push de 600 ms se declaran una sola vez (`NavTransitions`). `navigateToMainGraph()` y `navigateToLoginClearingBackStack()` limpian toda la pila.
- **DI:** `core/di/CoreDataModule` provee `FirebaseAuth`, `FirebaseFirestore`, `FirebaseFunctions` y el `DataStore`, siempre **no nulos**; `feature/auth/data/di/AuthDataModule` enlaza las interfaces.
- **Design system (`core/designsystem`):** `theme/` (tokens), `components/` (prefijo `SmartFood`) y `util/` (modificadores, `SystemBarEffect`, `ClearFocusOnKeyboardDismiss`).
- **Adaptabilidad (spec 002):** las medidas de los formularios salen de `theme/FormMetrics.kt`.
  - `AuthScreenScaffold` toma el perfil inicial (`Compact`, `Medium` o `Regular`) con `formMetricsForWindow(maxHeight)`, que usa el alto útil sin barras.
  - Si con el teclado cerrado el formulario no cabe durante 400 ms, baja al perfil siguiente con `smaller()`. Los desbordes pasajeros, como el fundido entre pasos, no cuentan. En las pruebas, avanzar `mainClock` antes de comprobar el perfil. Lo provee con `LocalFormMetrics`, y los componentes leen `LocalFormMetrics.current`. No se usan medidas fijas nuevas.
  - El alto sobrante se reparte con `FlexibleGap(minHeight, weight)`. Dentro de un `AnimatedContent`, el contenido usa `Modifier.fillFormViewport(LocalFormViewport.current)`.
- **Modo de color (spec 002, HU-UI-05):**
  - El modo se decide **solo** con `ThemeMode` (`theme/ThemeMode.kt`, por defecto `LIGHT`): `SmartfoodTheme(themeMode)` provee `LocalIsDarkTheme`.
  - Los componentes leen `LocalIsDarkTheme.current`; **prohibido `isSystemInDarkTheme()` fuera de `ThemeMode.kt`**.
  - La paleta y los esquemas claro y oscuro no se tocan.
  - El tema XML es claro (`Base.Theme.Smartfood`, ventana `GreenLight`). `values-v31` hace que la pantalla de arranque de Android 12+ sea igual a la Splash (`splash_icon`).
- **Fidelidad con Figma (spec 002, HU-UI-07):** referencia en los frames "_Claro" del archivo Figma `z1AoQvluQcmx98gjP4vHF6`.
  - Tipografía Poppins empaquetada en `res/font` (licencia en `docs/licenses/OFL-Poppins.txt`).
  - Para ajustar un color a Figma se cambia el **rol** en `LightColorScheme` (`Theme.kt`), nunca el valor de un token. El único token agregado es `GreenCheck`.
- **Ícono (spec 002, HU-UI-06):** los `mipmap-*/ic_launcher*` y `ic_launcher-playstore.png` los genera `tools/generate_launcher_icon.py` desde `drawable-nodpi/logo_smartfood_circle.png`. Nunca se editan a mano; si cambia el logo, se vuelve a ejecutar el script.
- **Foco y teclado (spec 002):**
  - **Prohibido el scroll programático** (`scrollTo`, `animateScrollTo`) en las pantallas. El panel aplica el inset del teclado fuera del `verticalScroll` y Compose mantiene visible el campo enfocado.
  - El encabezado de marca se colapsa leyendo `WindowInsets.ime` en la fase de layout.
  - La etiqueta de `SmartFoodTextField` está dentro del `decorationBox`: tocarla enfoca el campo y forma parte de su semántica. En las pruebas, `onNodeWithText("<etiqueta>")` devuelve el campo editable.
  - Elementos flotantes: `AnchoredPopup` con `AbovePopupPositionProvider`, sin offsets fijos. El popup va **siempre encima** del campo; si no cabe, se ajusta a la barra de estado, nunca debajo. El checklist de contraseña es `PasswordFieldWithChecklist`.

## Pruebas

- **Unitarias:** JUnit4 + `kotlinx-coroutines-test`, sin librería de mocks. Los fakes compartidos están en `app/src/test/.../testutil/` (`FakeAuthRepository`, `FakeUserRepository`, `FakeSessionPreferences`, `FakePasswordRecoveryRepository`, `MainDispatcherRule`, `Result.authError`). Para probar efectos: `viewModel.effects.first()` dentro de `runTest`.
- `unitTests.isReturnDefaultValues = true` está activo porque los constructores de las excepciones de Firebase usan `TextUtils`.
- **Instrumentadas:** usan `@HiltAndroidTest`, `CustomTestRunner` (`HiltTestApplication`) y `createAndroidComposeRule<HiltTestActivity>()` (`HiltTestActivity` vive en `app/src/debug`). Las pruebas de pantalla usan la `XScreen` stateless con un `XUiState` construido a mano.

## Convenciones

- Código, KDoc, comentarios, commits y nombres de test en español.
- Cada archivo Kotlin abre con la cabecera KDoc que exige el curso: `Nombre`, `Autor: SmartFood Team`, `Fecha`, `Descripción` (y `@param` cuando aplica).
- Ningún secreto en el código ni en el APK (RNF-01).

## Configuración externa

Firebase (`smartfood-f62a1`, plan Blaze): Authentication con correo/contraseña, Cloud Firestore (`southamerica-east1`) y Cloud Functions. Reglas en `firestore.rules`: cada usuario solo accede a `users/{su uid}` (al crearlo debe incluir la constancia de la spec 005), y `passwordResets` y `welcomeEmails` están cerrados a los clientes. Los secretos de Gmail se configuran con `firebase functions:secrets:set` (ver README).
