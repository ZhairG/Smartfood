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
                                                         └─ demo/ (recuperación simulada)
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
    suspend fun requestCode(email: String): Result<Unit>
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

`DemoPasswordRecoveryRepository` implementa `PasswordRecoveryRepository` usando `DemoRecoveryData`:

```kotlin
object DemoRecoveryData {
    val REGISTERED_EMAILS = setOf("demo@smartfood.com", "prueba@smartfood.com")
    const val VALID_CODE = "1234"
    const val SIMULATED_LATENCY_MS = 800L
}
```

El ViewModel **nunca** conoce el código válido: solo lo conoce el repositorio.

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
| ADR-01 | **Recuperación simulada** detrás de `PasswordRecoveryRepository` | (a) OTP generado en el cliente y enviado con la API de Brevo; (b) Cloud Function | (a) exponía la API key en el APK y además no cambiaba la contraseña. (b) Requiere el plan Blaze y un backend que no cabe en el plazo. Con la interfaz, migrar es cambiar un `@Binds`. |
| ADR-02 | Paquetes por feature en un solo módulo | Multi-módulo Gradle | Mismo aislamiento conceptual con mucha menos configuración para un equipo de 4 en un semestre. |
| ADR-03 | Un ViewModel por pantalla | `AuthViewModel` compartido | El estado combinado mezclaba campos de 3 pantallas y hacía frágiles las pruebas. |
| ADR-04 | El registro se orquesta en `RegisterUseCase` | Orquestarlo en el repositorio | El rollback (CA-01-09) se puede probar con fakes, sin Firebase. |
| ADR-05 | Perfil en Firestore `users/{uid}` | `displayName` de Firebase Auth | `users/{uid}` será la raíz del inventario y del presupuesto (features 002 y 004). |
| ADR-06 | "Recuérdame" en DataStore | `SharedPreferences` | API asíncrona basada en coroutines y recomendada por Android. |

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
