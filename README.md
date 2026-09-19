# Smartfood App

Proyecto Android moderno desarrollado con Kotlin y Jetpack Compose, estructurado utilizando **Clean Architecture** (organizada por capas) y el patrón **MVVM con UDF** (Unidirectional Data Flow) para la interfaz de usuario.

## 🏗 Estructura del Proyecto

El proyecto está organizado bajo el paquete principal `com.smartfood.app` con los siguientes directorios estratégicos:

### Estructura de Directorios Creada

```text
com.smartfood.app/
├── SmartfoodApp.kt                        # Aplicación base anotada con @HiltAndroidApp
├── MainActivity.kt                        # Entrada principal con NavHost y @AndroidEntryPoint
│
├── data/                                  # CAPA DE DATOS (Data Layer)
│   ├── source/
│   │   └── FirebaseAuthDataSource.kt     # Fuente de datos directa con Firebase Authentication
│   ├── repository/
│   │   ├── AuthRepository.kt             # Interfaz de abstracción del repositorio
│   │   └── AuthRepositoryImpl.kt         # Implementación que orquesta FirebaseAuthDataSource
│   └── di/                               # INYECCIÓN DE DEPENDENCIAS DE DATOS
│       └── DataModule.kt                 # Módulo Hilt que provee Firebase y el AuthRepository
│
├── ui/                                    # CAPA DE INTERFAZ (UI Layer)
│   ├── auth/                              # Módulo de autenticación (Login, Registro, Recuperación)
│   │   ├── AuthContract.kt               # AuthUiState y AuthEvent (UDF centralizado)
│   │   └── AuthViewModel.kt              # Lógica de presentación y consumo del repositorio
│   └── theme/
│
├── domain/                                # CAPA DE DOMINIO (Domain Layer)
│   └── usecase/
│       └── auth/
│           ├── ValidatePasswordUseCase.kt
│           ├── LoginUseCase.kt
│           ├── RegisterUseCase.kt
│           └── RecoverPasswordUseCase.kt
│
├── navigation/                            # NAVEGACIÓN (Jetpack Navigation Compose)
│   ├── Routes.kt                         # Clases/Objetos de rutas Type-Safe con @Serializable
│   └── AppNavigation.kt                  # NavHost y declaración de grafos de pantallas
│
├── notifications/                         # Gestión de notificaciones push
├── admin/                                 # Módulos de administración
└── utils/                                 # Extensiones y utilidades
    └── CoroutinesExt.kt                  # Manejo seguro de corrutinas (safeRunCatching)
```

---

### Detalle de Archivos y Responsabilidades

#### 1. Capa de Datos (`data/`)
- **[AuthRepository.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/repository/AuthRepository.kt):** Define las operaciones que necesita la aplicación (`login`, `register`, `sendPasswordReset`, `logout`, `isUserAuthenticated`).
- **[FirebaseAuthDataSource.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/source/FirebaseAuthDataSource.kt):** Gestiona las llamadas directas al SDK de Firebase Auth utilizando `kotlinx.coroutines.tasks.await()`.
- **[AuthRepositoryImpl.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/repository/AuthRepositoryImpl.kt):** Implementa el repositorio encapsulando las respuestas en `Result<Unit>` para manejo seguro de excepciones.

#### 2. Inyección de Dependencias (`data/di/` y `SmartfoodApp`)
- **[SmartfoodApp.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/SmartfoodApp.kt):** Clase `Application` marcada con `@HiltAndroidApp` para habilitar Hilt en todo el proyecto.
- **[AndroidManifest.xml](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/AndroidManifest.xml):** Se vinculó `android:name=".SmartfoodApp"`.
- **[DataModule.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/data/di/DataModule.kt):** Módulo `@InstallIn(SingletonComponent::class)` que provee `FirebaseAuth.getInstance()` y enlaza (`@Binds`) `AuthRepositoryImpl` con `AuthRepository`.

#### 3. Capa de Dominio (`domain/`)
- Encapsula la lógica de negocio pura en **Casos de Uso (UseCases)** inyectables para evitar sobrecargar los ViewModels.
- **[ValidatePasswordUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/ValidatePasswordUseCase.kt):** Verifica si una contraseña cumple los requisitos (longitud, mayúsculas, caracteres especiales).
- **[LoginUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/LoginUseCase.kt)**, **[RegisterUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/RegisterUseCase.kt)**, **[RecoverPasswordUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/RecoverPasswordUseCase.kt):** Orquestan las validaciones previas y la llamada al `AuthRepository`.

#### 4. Capa de Interfaz (`ui/auth/`)
Sigue el patrón **UDF (Unidirectional Data Flow)** recomendado por Android:
- **Flujo hacia abajo (State):** El `ViewModel` expone un `StateFlow<AuthUiState>` inmutable.
- **Flujo hacia arriba (Events):** Los elementos UI emiten eventos a través de `onEvent(AuthEvent)`.

- **Autenticación (Centralizada):**
  - [AuthUiState.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthUiState.kt) y [AuthEvent.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthEvent.kt): Estado completo (campos, booleanos de validación, carga, errores) y eventos sellados.
  - [AuthViewModel.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthViewModel.kt): Inyecta los UseCases correspondientes para controlar el flujo asíncrono y actualizar la UI de manera reactiva.

#### 5. Navegación Type-Safe (`navigation/`)
- **[Routes.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/navigation/Routes.kt):** Objetos anotados con `@Serializable` para garantizar el tipado estricto de rutas.
- **[AppNavigation.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/navigation/AppNavigation.kt):** Configuración central del `NavHost` con Jetpack Compose.

#### 6. Utilidades (`utils/`)
- **[CoroutinesExt.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/utils/CoroutinesExt.kt):** Define la función `safeRunCatching` para evitar atrapar `CancellationException`, garantizando la correcta cancelación estructurada de las corrutinas de Kotlin.


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
  * `AuthUiState.kt` y `AuthEvent.kt`: Estado inmutable (con validaciones dinámicas) y jerarquía de eventos `AuthEvent` para comunicar acciones de la vista al ViewModel.
  * `AuthViewModel.kt`: ViewModel (`@HiltViewModel`) que inyecta UseCases y gestiona el flujo unidireccional de datos de forma asíncrona.

### 3. Refactorización a Clean Architecture y Corrutinas Seguras
* **Domain Layer:** Extracción de toda la lógica de negocio pura desde el ViewModel hacia Casos de Uso independientes (`ValidatePasswordUseCase`, `LoginUseCase`, `RegisterUseCase`, `RecoverPasswordUseCase`).
* **UI Layer Cleanup:** Limpieza de carpetas obsoletas (`login/`, `register/`, `recovery/`) en favor de una gestión de autenticación centralizada.
* **Seguridad Asíncrona:** Implementación de la extensión `safeRunCatching` para asegurar el respeto a la cancelación estructurada (`CancellationException`) de Kotlin Coroutines en el Data Layer.
