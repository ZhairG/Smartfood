# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) or others when working with code in this repository.

## Proyecto

App Android (Kotlin + Jetpack Compose, Material 3) de un solo módulo `:app`, paquete `com.smartfood.app`. Clean Architecture por capas (`data` → `domain` → `ui`) con MVVM + UDF, Hilt para DI, Navigation Compose type-safe y Firebase Auth. `compileSdk`/`targetSdk` 37, `minSdk` 24, Java 11. Dependencias en `gradle/libs.versions.toml` (algunas declaradas directamente en `app/build.gradle.kts`).

Instrucción del proyecto (`AGENTS.md`): revisar los comentarios `TODO` y generar el código correspondiente aplicando patrones MVVM.

## Comandos

En Windows usar `gradlew.bat` (o `./gradlew` desde Git Bash).

```bash
./gradlew :app:assembleDebug                 # Compilar APK debug
./gradlew :app:testDebugUnitTest             # Pruebas unitarias (JVM)
./gradlew :app:testDebugUnitTest --tests "com.smartfood.app.ui.auth.AuthViewModelTest"          # Una clase
./gradlew :app:testDebugUnitTest --tests "com.smartfood.app.ui.auth.AuthViewModelTest.*EmailChanged*"  # Un test
./gradlew :app:connectedDebugAndroidTest     # Pruebas instrumentadas (requiere emulador/dispositivo)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartfood.app.ui.auth.login.LoginScreenTest
./gradlew :app:lintDebug                     # Android Lint (no hay ktlint/detekt configurados)
```

El build `release` usa R8 en modo completo (`android.enableR8.fullMode=true`) con minify y shrink de recursos; los cambios con reflexión/serialización deben revisarse contra `app/proguard-rules.pro`.

## Arquitectura

**Flujo de datos:** `FirebaseAuthDataSource` / `BrevoEmailDataSource` → `AuthRepositoryImpl` (devuelve `Result<T>`) → UseCases en `domain/usecase/auth` → `AuthViewModel` → pantallas Compose.

- **Data:** `AuthRepositoryImpl` envuelve todas las llamadas con `safeRunCatching` (`utils/CoroutinesExt.kt`), que relanza `CancellationException`. Usarlo en lugar de `runCatching` en código suspendido. `DataModule` hace `@Binds` del repositorio y provee `FirebaseAuth`, `FirebaseFunctions` y `OkHttpClient`.
- **Recuperación de contraseña (OTP):** `AuthRepositoryImpl.sendPasswordReset` genera un OTP de 4 dígitos, lo envía por correo vía API REST de Brevo (`BrevoEmailDataSource`) y lo guarda en un `ConcurrentHashMap` en memoria (no persiste entre procesos). El flujo de UI tiene pasos (`RecoveryStep`) manejados por `ResetPasswordUseCase` (`sendResetCode` → `verifyOtpCode` → `resetPassword`).
- **Domain:** los UseCases señalan errores de validación con `Result.failure(IllegalArgumentException("CODIGO"))` (p. ej. `"EMPTY_EMAIL"`, `"INVALID_OTP_CODE"`, `"VALIDATION_ERROR"`). `AuthViewModel` mapea esos mensajes —y las excepciones de Firebase en `handleError`— a `AuthErrorType` (`ui/auth/AuthEnums.kt`). Al añadir un error nuevo hay que tocar UseCase, enum y el mapeo del ViewModel.
- **UI (UDF):** un único `AuthViewModel` cubre Login, Registro y Recuperación: expone `StateFlow<AuthUiState>` y recibe todo vía `onEvent(AuthEvent)` (sealed). Las pantallas son stateless (`state`, `onEvent`, callbacks de navegación). Cada destino llama a `hiltViewModel()` dentro de su `composable`, por lo que cada pantalla obtiene su **propia instancia** (estado no compartido entre pantallas).
- **Navegación:** `navigation/Routes.kt` define `sealed interface Route` con objetos `@Serializable`, cada uno ligado a un `ScreenRoute` (enum). `AppNavigation.kt` arma dos grafos anidados: `AuthGraph` (Splash → Login → Register/RegisterSuccess, PasswordRecovery/PasswordRecoverySuccess) y `MainGraph` (Home, aún un `PlaceholderScreen`). Las transiciones son slides de 600 ms con `FastOutSlowInEasing`. Las pantallas de éxito reutilizan `ui/components/ActionSuccessScreen`.
- **Design system:** tokens en `ui/theme` (`Color`, `Spacing`, `Type`, `Theme`) y componentes reutilizables en `ui/components`. Los textos visibles van en `res/values/strings.xml`.

## Pruebas

- **Unitarias (`app/src/test`):** JUnit4 + `kotlinx-coroutines-test`, sin librería de mocks. Se usan fakes escritos a mano: `FakeAuthRepository` está definido dentro de `LoginUseCaseTest.kt` y `MainDispatcherRule` dentro de `AuthViewModelTest.kt`; reutilizarlos en vez de duplicarlos. Las data sources son `open` para poder sobrescribirlas en tests.
- **Instrumentadas (`app/src/androidTest`):** usan `@HiltAndroidTest`, `CustomTestRunner` (sustituye la app por `HiltTestApplication`) y `createAndroidComposeRule<HiltTestActivity>()`; `HiltTestActivity` vive en `app/src/debug`.
- Los nombres de test se escriben en español con backticks (`` `cuando se envia evento X, el estado se actualiza` ``).

## Convenciones

- Código, KDoc, comentarios y mensajes de commit en español.
- Cada archivo Kotlin abre con un bloque KDoc de cabecera: `Nombre`, `Autor: SmartFood Team`, `Fecha`, `Descripción` (y `@param` cuando aplica). Mantener este formato en archivos nuevos.

## Pendientes conocidos

- `BrevoEmailDataSource` contiene la API key de Brevo y el remitente en el código fuente (marcado con `TODO esto no debe estar aqui`); debería moverse fuera del código (p. ej. `local.properties` → `BuildConfig`, o un backend/Cloud Function).
- `firebase.json` declara un codebase de Cloud Functions en `functions/`, pero ese directorio no existe en el repo.
- El `README.md` describe la estructura de carpetas pero puede estar desactualizado (p. ej. no menciona `ui/auth/recovery` ni `ResetPasswordUseCase`).
