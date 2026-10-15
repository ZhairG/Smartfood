# Plan técnico 001: Autenticación

> El CÓMO de [`spec.md`](./spec.md). Cumple la [constitución](../constitution.md).

## 1. Stack

| Necesidad | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Estado | ViewModel + `StateFlow` + `Channel` (efectos) |
| DI | Hilt |
| Navegación | Navigation Compose type-safe (`@Serializable`) |
| Autenticación | Firebase Authentication (correo/contraseña) |
| Perfil | Cloud Firestore: colección `users` |
| Preferencias locales | Jetpack DataStore (Preferences) |
| Pruebas | JUnit4, kotlinx-coroutines-test, Compose UI Test, Hilt testing |

## 2. Arquitectura

```
                 ┌──────────────────────── feature/auth ────────────────────────┐
  Compose ──▶ ui/ (Screen ◀─ Route ─▶ ViewModel) ──▶ domain/usecase ──▶ domain/repository (interfaces)
                                                                               ▲
                                                         data/repository (implementaciones)
                                                         ├─ FirebaseAuth
                                                         ├─ FirebaseFirestore
                                                         ├─ DataStore
                                                         ├─ Cloud Functions (recuperación real, spec 003)
                                                         └─ demo/ (recuperación simulada, modo demo)
```

```
com.smartfood.app/
├── core/
│   ├── common/          safeRunCatching
│   ├── designsystem/    theme/, components/, util/ (modificadores y barras del sistema)
│   ├── di/              CoreDataModule (FirebaseAuth, Firestore, DataStore)
│   └── navigation/      Routes, ScreenRoute, AppNavHost, NavTransitions
└── feature/
    ├── splash/          SplashScreen, SplashViewModel
    ├── home/            HomeScreen, HomeViewModel
    └── auth/
        ├── domain/      model/, repository/, usecase/
        ├── data/        repository/, mapper/, demo/, local/, di/
        └── ui/          login/, register/, recovery/, components/, AuthErrorText, AuthNavGraph
```

**Patrón Route/Screen**: cada destino de navegación tiene un `XRoute` que obtiene el ViewModel con
`hiltViewModel()`, recolecta su estado y sus efectos, y delega en `XScreen`, que es **stateless**.
Las pruebas de UI usan directamente `XScreen`.

## 3. Modelo de dominio

```kotlin
data class User(val uid: String, val fullName: String, val email: String)

enum class AuthField { FULL_NAME, EMAIL, PASSWORD, OTP_CODE }

sealed interface AuthError {
    data class EmptyFields(val fields: Set<AuthField>) : AuthError
    data object InvalidEmail : AuthError
    data object WeakPassword : AuthError
    data object PasswordsDontMatch : AuthError
    data object TermsNotAccepted : AuthError
    data object InvalidCredentials : AuthError
    data object AccountNotFound : AuthError
    data object EmailAlreadyInUse : AuthError
    data object InvalidOtpCode : AuthError
    data object Network : AuthError
    data object ServiceUnavailable : AuthError
    data class Unknown(val cause: String?) : AuthError
}

class AuthException(val error: AuthError) : Exception(error.toString())
```

Todas las operaciones devuelven `Result<T>`. Un fallo siempre lleva un `AuthException`, que se recupera con
`Throwable.toAuthError()`.

## 4. Contratos (interfaces del dominio)

```kotlin
interface AuthRepository {
    fun currentUserId(): String?
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun createAccount(email: String, password: String): Result<String>   // uid
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun deleteCurrentAccount(): Result<Unit>
    fun logout()
}

interface UserRepository {
    suspend fun createProfile(user: User): Result<Unit>
    suspend fun getProfile(uid: String): Result<User>
}

interface PasswordRecoveryRepository {
    suspend fun requestCode(email: String): Result<Duration>   // espera antes de otro código (spec 003)
    suspend fun verifyCode(email: String, code: String): Result<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>
}

interface SessionPreferences {
    suspend fun isRememberMeEnabled(): Boolean
    suspend fun setRememberMe(enabled: Boolean)
}
```

## 5. Casos de uso

| UseCase | Responsabilidad | CA |
|---|---|---|
| `ValidateEmailUseCase` | Regex del correo | CA-01-02, CA-02-02, CA-04-02 |
| `ValidatePasswordUseCase` | Política de contraseña (detalle por regla) | CA-01-03, CA-04-10 |
| `LoginUseCase` | Valida, inicia sesión y guarda "Recuérdame" | HU-02, CA-03-01/02 |
| `RegisterUseCase` | Valida y **orquesta**: crear cuenta → guardar perfil (si falla, **rollback** con `deleteCurrentAccount`) → enviar verificación (si falla, se ignora) → `logout` | HU-01 |
| `ResolveSessionUseCase` | `true` si hay sesión **y** "Recuérdame"; si hay sesión sin "Recuérdame", cierra la sesión | HU-03 |
| `GetCurrentUserUseCase` | Perfil del usuario actual | CA-05-01/02 |
| `LogoutUseCase` | Cierra la sesión | CA-05-03 |
| `RequestRecoveryCodeUseCase` | Valida el correo y solicita el código | CA-04-01..04 |
| `VerifyRecoveryCodeUseCase` | Valida el formato (4 dígitos) y verifica | CA-04-05..07 |
| `ResetPasswordUseCase` | Valida la política y la coincidencia, y restablece | CA-04-10/11 |

El orden de validación en el registro es: campos vacíos → correo → contraseña → términos.

## 6. Datos

### 6.1 Firebase Auth: mapeo de errores (`FirebaseErrorMapper`)

| Excepción | `AuthError` |
|---|---|
| `FirebaseNetworkException` | `Network` |
| `FirebaseAuthInvalidUserException` | `AccountNotFound` |
| `FirebaseAuthUserCollisionException` | `EmailAlreadyInUse` |
| `FirebaseAuthWeakPasswordException` | `WeakPassword` |
| `FirebaseAuthInvalidCredentialsException` | `InvalidCredentials` |
| `FirebaseException` con `CONFIGURATION_NOT_FOUND` | `ServiceUnavailable` |
| `AuthException` | se conserva su error |
| Cualquier otra | `Unknown(message)` |

> Con la *protección contra enumeración de correos* de Firebase (activa por defecto en proyectos nuevos), un correo
> inexistente devuelve `InvalidCredentials` en lugar de `AccountNotFound`. Ambos casos tienen un mensaje
> definido en la spec.

### 6.2 Firestore

```
users/{uid}
  ├── fullName : string
  ├── email    : string
  └── createdAt: timestamp (serverTimestamp)
```

Se escribe con un `Map`, no con un POJO, para no depender de reflexión con R8. Reglas (`firestore.rules`):

```
match /users/{uid} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

### 6.3 DataStore

Archivo `session_prefs` con la clave `remember_me` (boolean, por defecto `true`).

### 6.4 Recuperación simulada (`data/demo`)

> Desde la spec 003 es el **modo demo** (`-PrecoveryDemo=true`, solo debug) y responde de forma neutra ante un
> correo sin cuenta. La recuperación real está en el [plan 003](../003-recuperacion-real/plan.md).

`DemoPasswordRecoveryRepository` implementa `PasswordRecoveryRepository` usando `DemoRecoveryData`:

```kotlin
object DemoRecoveryData {
    val REGISTERED_EMAILS = setOf("demo@smartfood.com", "prueba@smartfood.com")
    const val VALID_CODE = "1234"
    const val SIMULATED_LATENCY_MS = 800L
}
```

El ViewModel **nunca** conoce el código válido: solo lo conoce el repositorio.

### 6.5 Ruta a una recuperación real (post-entrega)

> **Implementada en la [spec 003](../003-recuperacion-real/spec.md)** con la opción B. Esta sección conserva el
> análisis que llevó a la decisión.

La v1 entrega la recuperación simulada (ADR-01). Esta sección deja documentado cómo hacerla real después.

**Lo que no sirve para recuperar.** `FirebaseUser.reauthenticate(credencial)` seguido de `updatePassword(nueva)`
cambia la contraseña solo si el usuario **ya inició sesión** y **conoce la contraseña actual**. En "¿Olvidaste tu
contraseña?" no se cumple ninguna de las dos condiciones: `currentUser` es `null` y la credencial necesita la
contraseña olvidada. Ese código queda para una futura historia "Cambiar contraseña" desde el perfil, con:
- Kotlin, `await()`, `safeRunCatching` y `.mapFirebaseError()`.
- Casos nuevos en `AuthError`: contraseña actual incorrecta y `requires-recent-login`.

**Cambiar la contraseña de un usuario sin sesión exige el Admin SDK.** Sus credenciales (cuenta de servicio) no
pueden ir en el APK (RNF-01), así que la versión real necesita una de estas dos opciones.

| Opción | Cómo funciona | Costo | Efecto en el flujo |
|---|---|---|---|
| **A. Correo de Firebase** | `FirebaseAuth.sendPasswordResetEmail(email)`. Firebase envía un enlace y el usuario elige la nueva contraseña en su página. | Gratis, sin servidor | Envía un enlace en lugar de un código: los pasos 2 y 3 se sustituyen por "Revisa tu correo". Con la protección contra enumeración de correos, Firebase no revela si el correo existe, así que no se puede mostrar "correo no registrado". |
| **B. Código real de 4 dígitos** | Servidor con `firebase-admin` y tres endpoints, descritos abajo. | Gratis: Vercel Hobby (sin tarjeta) o Cloud Functions (plan Blaze, con tarjeta y costo $0 dentro de la cuota gratuita). Correo por Gmail SMTP con contraseña de aplicación o Brevo. | Conserva los tres pasos y el diseño de Figma. |

Endpoints de la opción B:
1. `request-code`: genera el código y guarda en Firestore su **hash**, con vencimiento (unos 10 minutos) y contador
   de intentos. Envía el código por correo y responde igual exista o no la cuenta.
2. `verify-code`: compara el hash y descuenta intentos.
3. `reset-password`: vuelve a validar el código, ejecuta `admin.auth().updateUser(uid, { password })` y lo invalida.

Los secretos (cuenta de servicio y SMTP) viven solo en el servidor.

**Migración en la app (cualquier opción):**
- Crear un `RemotePasswordRecoveryRepository` en `data/` y cambiar únicamente el `@Binds` de `AuthDataModule`.
- El contrato `PasswordRecoveryRepository` (`requestCode`, `verifyCode`, `resetPassword`) ya encaja con la opción B
  sin cambios. La opción A solo usaría `requestCode` y exigiría actualizar la HU-04.
- Antes del código, se crea su spec, como pide la constitución.

**Ruta elegida (decisión del equipo, 2026-10-05): opción B con Cloud Functions en el plan Blaze.**
- **Costo $0 dentro de la cuota gratuita.** Blaze exige una tarjeta, así que se configura una **alerta de
  presupuesto** baja (por ejemplo, 1 USD) en Google Cloud Billing para enterarse de cualquier consumo inesperado.
  Las funciones se despliegan en la misma región que Firestore.
- **Estado de la facturación (2026-10-05):** el proyecto está en Blaze gracias a la **prueba gratuita de Google
  Cloud**, con unos 1.000.000 COP de crédito que vencen el **4 de enero de 2027**.
  - Durante la prueba no se cobra a la tarjeta.
  - Si al vencer la prueba no se pasa a una cuenta de facturación pagada ("Actualizar"), los servicios de pago se
    detienen, y con ellos las Cloud Functions de la recuperación. Ya con la cuenta pagada, el uso dentro de la cuota
    gratuita sigue costando $0.
  - Al desplegar las funciones se configura la política de limpieza de imágenes de Artifact Registry
    (`firebase functions:artifacts:setpolicy`): las imágenes antiguas acumuladas son la causa más común de pequeños
    cobros.
- **Desarrollo y pruebas con la Firebase Local Emulator Suite (Auth, Firestore y Functions):**
  - Las funciones se escriben y prueban en local antes de desplegarlas. En el emulador, `firebase-admin` apunta al
    Auth emulado y el código se lee en el log en lugar de enviarse por correo.
  - Las pruebas instrumentadas de `FirebaseAuthRepository` y `FirestoreUserRepository`, que hoy solo usan fakes,
    pueden ejecutarse contra Auth y Firestore emulados, sin crear cuentas en `smartfood-f62a1`.
  - Las `firestore.rules` se prueban en local antes de `firebase deploy`.
- **Integración en la app, solo en debug:**
  - `CoreDataModule` llama a `useEmulator(...)` de `FirebaseAuth` y `FirebaseFirestore` cuando se activa con una
    propiedad de Gradle (por ejemplo, `-PuseFirebaseEmulator=true`). Así nadie del equipo depende de tener los
    emuladores encendidos.
  - Un `network_security_config` de debug permite HTTP solo hacia el host del emulador.
  - En un teléfono físico se usa `adb reverse` para los puertos (9099 para Auth y 8080 para Firestore); en el
    emulador de Android, `10.0.2.2`.
- **Configuración:** `firebase.json` añade la sección `emulators` y la carpeta `functions/`. Los secretos (SMTP)
  van en Secret Manager (`defineSecret`), nunca en el repositorio ni en el APK (RNF-01).

## 7. Navegación

```
Splash (raíz) ──(con sesión recordada)──▶ MainGraph ─ Home ──(cerrar sesión)──▶ Login*
   │
   └──(sin sesión)──▶ AuthGraph ─ Login ──(éxito)──▶ MainGraph*
                                    ├──▶ Register ──▶ RegisterSuccess ──▶ Login*
                                    └──▶ PasswordRecovery ──▶ PasswordRecoverySuccess ──▶ Login*
* = se limpia toda la pila (navigateToMainGraph / navigateToLoginClearingBackStack): "atrás" no regresa
```

Las transiciones de empuje de 600 ms con `FastOutSlowInEasing` se declaran **una sola vez** en el `NavHost`
(`NavTransitions`). Cada feature aporta su grafo como extensión de `NavGraphBuilder` (`authGraph`, `mainGraph`).

## 8. Decisiones (ADR)

| # | Decisión | Alternativas descartadas | Motivo |
|---|---|---|---|
| ADR-01 | **Recuperación simulada** detrás de `PasswordRecoveryRepository`. *Reemplazada por ADR-REC-01 ([plan 003](../003-recuperacion-real/plan.md#8-decisiones-adr)); la simulada queda como modo demo.* | (a) OTP generado en el cliente y enviado con la API de Brevo; (b) Cloud Function | (a) exponía la API key en el APK y además no cambiaba la contraseña. (b) Requiere el plan Blaze y un backend que no cabe en el plazo. Con la interfaz, migrar es cambiar un `@Binds`. Ruta para hacerla real: §6.5 (después de la entrega: opción B en Blaze, desarrollada con la Emulator Suite). |
| ADR-02 | Paquetes por feature en un solo módulo | Multi-módulo Gradle | Mismo aislamiento conceptual con mucha menos configuración para un equipo de 4 en un semestre. |
| ADR-03 | Un ViewModel por pantalla | `AuthViewModel` compartido | El estado combinado mezclaba campos de 3 pantallas y hacía frágiles las pruebas. |
| ADR-04 | El registro se orquesta en `RegisterUseCase` | Orquestarlo en el repositorio | El rollback (CA-01-09) se puede probar con fakes, sin Firebase. |
| ADR-05 | Perfil en Firestore `users/{uid}` | `displayName` de Firebase Auth | `users/{uid}` será la raíz del inventario y del presupuesto (features 002 y 004). |
| ADR-06 | "Recuérdame" en DataStore | `SharedPreferences` | API asíncrona basada en coroutines y recomendada por Android. |
| ADR-07 | Autocompletado de Android con `ContentType` y `AutofillManager.commit()` (HU-07, #67) | (a) Credential Manager; (b) lista de cuentas propia | No agrega dependencias ni guarda contraseñas en la app (RNF-01): el gestor del teléfono (Google u otro) sugiere, guarda y propone contraseñas seguras. (a) suma una dependencia y una UI propia sin ganar nada para correo y contraseña; (b) obligaría a guardar credenciales. |

### 8.1 Autocompletado (HU-07)

- `SmartFoodTextField` recibe `contentType: ContentType?` y lo aplica con `semantics { contentType = … }`;
  `SmartFoodPasswordField` usa `ContentType.Password` por defecto y `PasswordFieldWithChecklist`,
  `ContentType.NewPassword` (su `modifier` va al popup, por eso el tipo es un parámetro).
- Login: correo `Username + EmailAddress` y contraseña `Password`. Registro: nombre `PersonFullName`, correo
  `Username + EmailAddress` y contraseñas `NewPassword`. Recuperación: correo del paso 1 `Username + EmailAddress`
  y contraseñas del paso 3 `NewPassword`.
- `AuthNavGraph` llama a `LocalAutofillManager.current?.commit()` justo antes de navegar en
  `LoginEffect.NavigateToHome`, `RegisterEffect.NavigateToSuccess` y `PasswordRecoveryEffect.NavigateToSuccess`:
  así el teléfono ofrece guardar la cuenta (CA-07-02). Las pantallas siguen sin estado.
- Las contraseñas guardadas dependen de la firma del APK: las del release no aparecen en un debug firmado en otro
  equipo.

## 9. Estrategia de pruebas

- **Unitarias**: UseCases (con fakes de `testutil/`), `FirebaseErrorMapper`, `DemoPasswordRecoveryRepository`,
  `LoginViewModel`, `RegisterViewModel`, `PasswordRecoveryViewModel`, `SplashViewModel` y `HomeViewModel`.
- **Instrumentadas**: `LoginScreen`, `RegisterScreen`, `PasswordRecoveryScreen` (stateless) y `AppNavHost`
  (recorrido Splash → Login → Registro / Recuperación).
- La matriz CA → prueba está en [`tasks.md`](./tasks.md#matriz-de-trazabilidad).

## 10. Configuración externa (manual)

1. Firebase Console → **Authentication** → habilitar *Correo/contraseña*.
2. Firebase Console → **Firestore Database** → crear la base de datos (modo producción).
3. Publicar las reglas: `firebase deploy --only firestore:rules`.
