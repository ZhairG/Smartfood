# Smartfood App

Proyecto Android moderno desarrollado con Kotlin y Jetpack Compose, estructurado utilizando **Clean Architecture** (organizada por capas) y el patrón **MVVM con UDF** (Unidirectional Data Flow) para la interfaz de usuario.

## 🏗 Estructura del Proyecto

El proyecto está organizado bajo el paquete principal `com.smartfood.app` con los siguientes directorios estratégicos:

### Estructura de Directorios Creada

```text
com.smartfood.app/
├── SmartfoodApp.kt                        # Aplicación base anotada con @HiltAndroidApp
├── MainActivity.kt
│
├── data/                                  # CAPA DE DATOS (Data Layer)
│   ├── source/
│   │   └── FirebaseAuthDataSource.kt     # Fuente de datos directa con Firebase Authentication
│   └── repository/
│       ├── AuthRepository.kt             # Interfaz de abstracción del repositorio
│       └── AuthRepositoryImpl.kt         # Implementación que orquesta FirebaseAuthDataSource
│
├── di/                                    # INYECCIÓN DE DEPENDENCIAS (Dagger Hilt)
│   └── AuthModule.kt                     # Módulo Hilt que provee FirebaseAuth, DataSource y AuthRepository
│
└── ui/                                    # CAPA DE INTERFAZ (UI Layer)
    ├── auth/                              # Módulo de autenticación (Login, Registro, Recuperación)
    │   ├── login/
    │   │   ├── LoginContract.kt          # LoginUiState y LoginUiEvent (UDF)
    │   │   └── LoginViewModel.kt         # Lógica de presentación y consumo de AuthRepository
    │   ├── register/
    │   │   ├── RegisterContract.kt       # RegisterUiState y RegisterUiEvent (UDF)
    │   │   └── RegisterViewModel.kt      # Lógica de registro y validación de contraseñas
    │   └── recovery/
    │       ├── PasswordRecoveryContract.kt # PasswordRecoveryUiState y PasswordRecoveryUiEvent
    │       └── PasswordRecoveryViewModel.kt # Lógica para restablecimiento de contraseña
    └── theme/
```

---

### Detalle de Archivos y Responsabilidades

#### 1. Capa de Datos (`data/`)
- **[AuthRepository.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/repository/AuthRepository.kt):** Define las operaciones que necesita la aplicación (`login`, `register`, `sendPasswordReset`, `logout`, `isUserAuthenticated`).
- **[FirebaseAuthDataSource.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/source/FirebaseAuthDataSource.kt):** Gestiona las llamadas directas al SDK de Firebase Auth utilizando `kotlinx.coroutines.tasks.await()`.
- **[AuthRepositoryImpl.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/repository/AuthRepositoryImpl.kt):** Implementa el repositorio encapsulando las respuestas en `Result<Unit>` para manejo seguro de excepciones.

#### 2. Inyección de Dependencias (`di/` y `SmartfoodApp`)
- **[SmartfoodApp.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/SmartfoodApp.kt):** Clase `Application` marcada con `@HiltAndroidApp` para habilitar Hilt en todo el proyecto.
- **[AndroidManifest.xml](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/AndroidManifest.xml):** Se vinculó `android:name=".SmartfoodApp"`.
- **[AuthModule.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/di/AuthModule.kt):** Módulo `@InstallIn(SingletonComponent::class)` que provee `FirebaseAuth.getInstance()`, `FirebaseAuthDataSource` y vincula `AuthRepositoryImpl` con `AuthRepository`.

#### 3. Capa de Interfaz (`ui/auth/`)
Sigue el patrón **UDF (Unidirectional Data Flow)** recomendado por Android:
- **Flujo hacia abajo (State):** El `ViewModel` expone un `StateFlow<UiState>` inmutable.
- **Flujo hacia arriba (Events):** Los elementos UI emiten eventos a través de `onEvent(UiEvent)`.

- **Login:**
  - [LoginContract.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/login/LoginContract.kt): `LoginUiState` (email, password, isLoading, isSuccess, errorMessage) y `LoginUiEvent`.
  - [LoginViewModel.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/login/LoginViewModel.kt): Controla validación y llamada al repositorio con corrutinas (`viewModelScope`).

- **Registro:**
  - [RegisterContract.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/register/RegisterContract.kt): `RegisterUiState` (email, password, confirmPassword, loading, etc.) y `RegisterUiEvent`.
  - [RegisterViewModel.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/register/RegisterViewModel.kt): Validación de coincidencia de contraseñas y registro.

- **Recuperación de Contraseña:**
  - [PasswordRecoveryContract.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/recovery/PasswordRecoveryContract.kt): `PasswordRecoveryUiState` y `PasswordRecoveryUiEvent`.
  - [PasswordRecoveryViewModel.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/recovery/PasswordRecoveryViewModel.kt): Envío de correo de reseteo mediante el repositorio.


### 🛠 Tecnologías y Herramientas

* **UI:** Jetpack Compose (Material 3)
* **Arquitectura:** Clean Architecture + MVVM + UDF
* **Inyección de Dependencias:** Dagger-Hilt (`@HiltAndroidApp`, `@HiltViewModel`)
* **Navegación:** Jetpack Navigation Compose
* **Asincronismo:** Kotlin Coroutines y Flow (`StateFlow`)
* **Backend as a Service:** Firebase (Auth, BOM)
* **Gestión de Dependencias:** Gradle Version Catalogs (`libs.versions.toml`)

---

## 🚀 Progreso de Implementación

### 1. Base del Proyecto
* Configuración de la clase `SmartfoodApp.kt` con la anotación `@HiltAndroidApp`.
* Estructura limpia para `MainActivity.kt` decorada con `@AndroidEntryPoint`, inicializando el contenedor de Compose con un marcador de posición listo para la navegación.
* Creación de la estructura de directorios y paquetes solicitados.

### 2. Módulo de Autenticación (Auth)
Se ha configurado la capa lógica de autenticación siguiendo las mejores prácticas:

* **Data Layer:**
  * `AuthRepository.kt`: Interfaz que abstrae las operaciones de sesión (`login`, `register`, `sendPasswordReset`).
  * `FirebaseAuthDataSource.kt`: Conexión directa y asíncrona con el SDK de Firebase Auth.
  * `AuthRepositoryImpl.kt`: Implementación del repositorio que encapsula las respuestas en objetos `Result<Unit>` de Kotlin.
  * `DataModule.kt`: Módulo de Hilt que enlaza (`@Binds`) la interfaz del repositorio con su implementación y provee (`@Provides`) la instancia de `FirebaseAuth`.

* **UI Layer (UDF):**
  * `AuthContract.kt`: Definición del estado inmutable `AuthUiState` (que incluye validaciones de contraseña en tiempo real) y la jerarquía de eventos `AuthEvent` para comunicar acciones de la vista al ViewModel.
  * `AuthViewModel.kt`: ViewModel (`@HiltViewModel`) que expone el estado vía `StateFlow`, valida los inputs y consume el repositorio de forma asíncrona utilizando `viewModelScope.launch`.
