# Plan técnico 008: Perfil funcional

> Cómo se cumple la [spec](./spec.md). Escrito para que cualquier integrante, o un agente como OpenCode con DeepSeek,
> pueda seguirlo paso a paso: cada sección dice **qué archivo** se crea o cambia, **qué código existente se reutiliza**
> y **qué no se toca**. Ruta base del código: `app/src/main/java/com/smartfood/app/` (abreviada `…/`).

## 1. Arquitectura

```
feature/profile/
├── domain/
│   ├── model/        UserProfile, FoodPreferences (+ enums), NotificationSettings, ProfileField, ProfileError
│   ├── repository/   ProfileRepository, ProfilePhotoRepository, NotificationSettingsRepository
│   └── usecase/      ObserveProfile, UpdateProfile, ValidateProfile, RequestEmailChange, SyncProfileEmail,
│                     GetFoodPreferences, SaveFoodPreferences, ObserveNotificationSettings,
│                     SetNotificationSetting, ChangePassword
├── data/
│   ├── repository/   FirestoreProfileRepository, FirebaseStorageProfilePhotoRepository
│   ├── local/        DataStoreNotificationSettings
│   └── di/           ProfileDataModule
└── ui/
    ├── ProfileScreen.kt / ProfileViewModel.kt   (existentes: se mueven aquí y se amplían)
    ├── edit/          EditProfileContract, EditProfileViewModel, EditProfileScreen, EditProfileRoute
    ├── preferences/   FoodPreferencesContract, …ViewModel, …Screen
    ├── notifications/ NotificationSettingsContract, …ViewModel, …Screen
    ├── password/      ChangePasswordContract, …ViewModel, …Screen
    ├── theme/         ThemeSheet
    └── ProfileNavGraph.kt  (existente: agrega las 4 rutas internas)
core/preferences/      ThemePreferences (interfaz) y DataStoreThemePreferences  ← el tema es de toda la app
```

Reglas de siempre (constitución §3): `domain` sin Android ni Firebase; validaciones solo en los UseCases; un
ViewModel por pantalla con `XUiState`, `XEvent` y `XEffect`; pantallas sin estado; errores como `Result` con una
excepción tipada.

## 2. Modelo de datos

### 2.1 `users/{uid}` en Firestore (amplía el documento de la spec 001/005)

| Campo | Tipo | Notas |
|---|---|---|
| `fullName` | string | 2-50 caracteres (CA-PER-02-05) |
| `email` | string | se iguala al de Authentication al iniciar sesión (CA-PER-02-12) |
| `phone` | string, opcional | 10 dígitos que empiezan por 3, sin "+57" ni espacios (CA-PER-02-06) |
| `birthDate` | string `AAAA-MM-DD`, opcional | (CA-PER-02-07) |
| `photoPath` / `photoUrl` | string, opcionales, juntos | `users/{uid}/profile/{uuid}.jpg` (CA-PER-02-09) |
| `preferences` | map, opcional | `{ restrictions: [..], cuisines: [..], spice: "MILD"|"MEDIUM"|"HOT"|null, budget: "ECONOMIC"|"MODERATE"|"UNLIMITED"|null }` |
| `updatedAt` | timestamp | hora del servidor en cada actualización |
| `createdAt`, `termsVersion`, `termsAcceptedAt` | — | **no cambian** después de crear (RNF-PER-01) |

Se escribe y se lee con `Map`, como `FirestoreUserRepository` (sin reflexión, por R8).

### 2.2 Modelos de dominio (`feature/profile/domain/model`)

```kotlin
data class UserProfile(
    val uid: String, val fullName: String, val email: String,
    val phone: String? = null, val birthDate: LocalDate? = null,
    val photoUrl: String? = null, val photoPath: String? = null,
)
enum class DietaryRestriction { VEGETARIAN, VEGAN, GLUTEN_FREE, LACTOSE_FREE, NUT_FREE }
enum class Cuisine { COLOMBIAN, ITALIAN, ASIAN, MEXICAN, MEDITERRANEAN }
enum class SpiceLevel { MILD, MEDIUM, HOT }
enum class MealBudget { ECONOMIC, MODERATE, UNLIMITED }
data class FoodPreferences(
    val restrictions: Set<DietaryRestriction> = emptySet(), val cuisines: Set<Cuisine> = emptySet(),
    val spice: SpiceLevel? = null, val budget: MealBudget? = null,
)
data class NotificationSettings(
    val expiringProducts: Boolean = true, val suggestedRecipes: Boolean = true, val emptyInventory: Boolean = true,
)
enum class ProfileField { FULL_NAME, PHONE, BIRTH_DATE, EMAIL, CURRENT_PASSWORD }
```

Los errores reutilizan `AuthError`/`AuthException` (spec 001): `EmptyFields`, `InvalidEmail`, `WeakPassword`,
`PasswordsDontMatch`, `InvalidCredentials` (contraseña actual incorrecta), `EmailAlreadyInUse`, `TooManyRequests`,
`Network`, `Unknown`. Se agregan dos casos a `AuthError` y a `AuthErrorText`: `InvalidName` y `InvalidPhone`
(y `InvalidBirthDate`), siguiendo el procedimiento de AGENTS.md ("Para agregar un error…").

### 2.3 En el teléfono (DataStore)

- `notification_prefs`: `expiringProducts`, `suggestedRecipes`, `emptyInventory` (booleanos, `true` por defecto).
- `theme_prefs`: `themeMode` = `LIGHT` | `DARK` | `SYSTEM` (por defecto `LIGHT`).

Cada DataStore se declara en `core/di/CoreDataModule.kt` igual que `sessionDataStore`, con su propio nombre de
archivo y un `@Qualifier` para no confundirlos.

## 3. Contratos

```kotlin
// feature/profile/domain/repository
interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>                  // del uid con sesión
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun syncEmail(email: String): Result<Unit>       // CA-PER-02-12
    suspend fun getPreferences(): Result<FoodPreferences>
    suspend fun savePreferences(preferences: FoodPreferences): Result<Unit>
}
interface ProfilePhotoRepository {
    suspend fun upload(localPath: String): Result<Pair<String, String>>   // (photoPath, photoUrl)
    suspend fun delete(path: String): Result<Unit>
}
interface NotificationSettingsRepository {
    fun observe(): Flow<NotificationSettings>
    suspend fun set(settings: NotificationSettings)
}
// core/preferences
interface ThemePreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
// feature/auth/domain/repository/AuthRepository (se agregan 3 métodos)
fun currentEmail(): String?
suspend fun reauthenticate(currentPassword: String): Result<Unit>        // EmailAuthProvider.getCredential
suspend fun updatePassword(newPassword: String): Result<Unit>
suspend fun requestEmailChange(newEmail: String): Result<Unit>          // user.verifyBeforeUpdateEmail(newEmail)
```

`FirebaseAuthRepository` implementa los nuevos métodos con `safeRunCatching { … }.mapFirebaseError()`;
`FirebaseErrorMapper` ya convierte `FirebaseAuthInvalidCredentialsException` en `InvalidCredentials`,
la colisión en `EmailAlreadyInUse` y la red en `Network`. `FakeAuthRepository` (`app/src/sharedTest/…/testutil`)
recibe los mismos métodos con resultados configurables.

## 4. Pantallas

Todas usan `MainTabScaffold(currentTab = MainTab.PROFILE)` para conservar la barra inferior (CA-PER-01-03) y las
transiciones de `NavTransitions` (sin valores propios, spec 007). Rutas nuevas en `core/navigation/Routes.kt`:
`EditProfile`, `FoodPreferences`, `NotificationSettings`, `ChangePassword`, registradas en `ProfileNavGraph.kt`.

| Pantalla | Reutiliza | Notas |
|---|---|---|
| Perfil | `ProfileScreen` actual | Foto con Coil (`AsyncImage`, ya está en el inventario) o iniciales; fila "Tema de la app"; filas navegan; tarjetas 0 / inventario / $0 |
| Editar información | `SmartFoodTextField`, un `BirthDatePicker` copiado de `ExpiryDatePicker` pero que solo deja elegir fechas hasta hoy, `PhotoSourceSheet`, `SmartFoodWarningDialog`, `SmartFoodLoadingDialog`, `SmartFoodResultDialog` | Ver §5 |
| Preferencias | chip de `CategoryChips` como base de un `PreferenceChip` (verde si está elegido) | Multi o único por grupo (CA-PER-03-02) |
| Notificaciones | `Switch` de Material 3 con los colores del Figma | Guarda al tocar; permiso como `ProductEditorRoute` (Android 13+) |
| Cambiar contraseña | `PasswordFieldWithChecklist`, `SmartFoodPasswordField`, `ValidatePasswordUseCase` | "Guardado" → `NavigateBack` a los 2 s |
| Hoja de tema | `OptionSheet` (hoja de categoría del inventario) | 3 opciones, Guardar/Cancelar |

Los avisos "¿Desea guardar los cambios?" y "Guardado" son los mismos componentes que ya usa el inventario
(`SmartFoodWarningDialog` y `SmartFoodResultDialog` con cierre automático de 2 s, como `EditorResultHost`).

## 5. Editar información: foto y correo

- **Cámara del sistema (desviación):** `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())`.
  Necesita un `FileProvider`: se agrega en `AndroidManifest.xml` (`androidx.core.content.FileProvider`, autoridad
  `${applicationId}.fileprovider`) y `res/xml/file_paths.xml` con `<cache-path name="profile" path="profile/"/>`.
  La foto se escribe en `cacheDir/profile/{uuid}.jpg`. La cámara propia del inventario no cambia.
- **Galería:** igual que `ProductEditorRoute` (`PickVisualMedia` + `copyPickedPhoto`).
- **Subida:** al confirmar el guardado, `FirebaseStorageProfilePhotoRepository.upload` comprime con `PhotoCompressor`
  (1080 px, JPEG) y sube a `users/{uid}/profile/{uuid}.jpg`; si había una foto anterior se borra después. Sin red,
  error `Network` y no se guarda nada (CA-PER-02-09). No se usa WorkManager en el Perfil.
- **Correo (CA-PER-02-10 a 02-12):** si el correo cambió, el ViewModel muestra un aviso con un campo de contraseña
  ("Confirma tu contraseña"). `RequestEmailChangeUseCase` valida el correo (`ValidateEmailUseCase`), llama a
  `reauthenticate` y luego a `requestEmailChange`. El resto de datos se guarda igual. El perfil conserva el correo
  anterior hasta la confirmación.
- **Sincronización:** `SyncProfileEmailUseCase` se llama en `SplashViewModel`/`LoginUseCase` después de iniciar
  sesión: si `currentEmail()` difiere del correo del perfil, llama a `syncEmail`.

## 6. Notificaciones y tema

- **Aviso diario:** `ExpiryReminderWorker` lee `NotificationSettingsRepository`:
  - "Productos por vencer" apagado → no publica el aviso de vencimientos (además, `SetNotificationSettingUseCase`
    llama a `ExpiryReminderScheduler.cancel()`; al encenderlo, `schedule()`).
  - "Inventario vacío" encendido y 0 productos → publica el aviso de inventario vacío (canal y `PendingIntent` igual
    que el de vencimientos; textos en `strings_profile.xml`). El trabajo diario se programa si **cualquiera** de los dos
    está encendido.
- **Tema:** `MainActivity` recoge `ThemePreferences.themeMode` con `collectAsStateWithLifecycle(ThemeMode.Default)` y
  lo pasa a `SmartfoodTheme(themeMode = …)`. `ThemeMode.kt` sigue siendo el único lugar que consulta el modo del
  sistema (spec 002).

## 7. Reglas

### `firestore.rules` (`match /users/{uid}`)
```
allow update: if isOwner(uid)
  && request.resource.data.diff(resource.data).affectedKeys()
       .hasOnly(['fullName','email','phone','birthDate','photoPath','photoUrl','preferences','updatedAt'])
  && request.resource.data.fullName is string && request.resource.data.fullName.size() >= 2
  && request.resource.data.fullName.size() <= 50
  && (!('phone' in request.resource.data) || request.resource.data.phone.matches('^3[0-9]{9}$'))
  && (!('birthDate' in request.resource.data) || request.resource.data.birthDate.matches('^[0-9]{4}-[0-9]{2}-[0-9]{2}$'))
  && request.resource.data.updatedAt == request.time;
```
(La foto y las preferencias se validan con funciones como `validPhoto` del inventario.)

### `storage.rules`
```
match /users/{uid}/profile/{file} {
  allow read, delete: if request.auth != null && request.auth.uid == uid;
  allow create: if request.auth != null && request.auth.uid == uid && resource == null
    && request.resource.size < 2 * 1024 * 1024 && request.resource.contentType == 'image/jpeg';
}
```
Ambas se prueban en `rules-test/` antes de desplegar; el despliegue lo confirma Zhair.

## 8. Decisiones (ADR)

| Decisión | Alternativas | Motivo |
|---|---|---|
| Cámara del sistema (`TakePicture`) en el Perfil | Reutilizar la cámara propia (CameraX) | Pedido de Zhair; menos código y la del Figma se mejorará luego. |
| Notificaciones y tema en DataStore (por teléfono) | En Firestore | Son ajustes del dispositivo; funcionan sin conexión. |
| Preferencias en `users/{uid}.preferences` | Subcolección | Un solo documento, una sola lectura; las reglas validan el mapa. |
| Un solo trabajo diario para los dos avisos | Un worker nuevo para inventario vacío | Menos código y un solo horario. |
| Correo con `verifyBeforeUpdateEmail` | `updateEmail` directo | `updateEmail` está obsoleto y no verifica el correo nuevo. |

## 9. Estrategia de pruebas

- **Unitarias** (con fakes de `sharedTest`): `ValidateProfileUseCaseTest` (tabla de casos de nombre, teléfono y
  fecha), `UpdateProfileUseCaseTest`, `RequestEmailChangeUseCaseTest`, `SyncProfileEmailUseCaseTest`,
  `ChangePasswordUseCaseTest`, `SetNotificationSettingUseCaseTest`, y un `…ViewModelTest` por pantalla
  (habilitar guardar, aviso, carga, resultado, errores).
- **Instrumentadas** (con `PantallaEnFlujo`): `ProfileScreenTest` (foto/iniciales, tarjetas, filas, tema),
  `EditProfileScreenTest`, `FoodPreferencesScreenTest`, `NotificationSettingsScreenTest`, `ChangePasswordScreenTest`;
  `MainNavigationTest` para entrar y volver de cada pantalla.
- **Reglas:** casos nuevos en `rules-test/firestore.test.js` (no cambiar `termsVersion`, teléfono inválido, campo
  desconocido) y `rules-test/storage.test.js` (foto de perfil).
- **Manual:** checklist de `tasks.md` en el teléfono, incluidos el enlace del correo nuevo y el tema oscuro.
