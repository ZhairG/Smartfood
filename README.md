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
├── domain/                                # CAPA DE DOMINIO (Domain Layer)
│   └── usecase/
│       └── auth/
│           ├── ValidateEmailUseCase.kt   # Validación sintáctica de correo electrónico
│           ├── ValidatePasswordUseCase.kt# Verificación de fortaleza y criterios de contraseña
│           ├── LoginUseCase.kt           # Caso de uso para inicio de sesión
│           └── RegisterUseCase.kt        # Caso de uso para registro de usuarios
│
├── ui/                                    # CAPA DE INTERFAZ (UI Layer)
│   ├── auth/                              # Módulo de autenticación (Login y Registro)
│   │   ├── AuthEnums.kt                  # Tipos de errores y enumeraciones del flujo de Auth
│   │   ├── AuthEvent.kt                  # Jerarquía de eventos de UI para UDF
│   │   ├── AuthUiState.kt                # Estado reactivo inmutable expuesto al usuario
│   │   ├── AuthViewModel.kt              # Lógica de presentación y orquestación de UseCases
│   │   ├── components/                   # Componentes específicos de la autenticación
│   │   │   ├── AuthErrorMessage.kt
│   │   │   ├── AuthScreenScaffold.kt
│   │   │   └── PasswordRequirementChecklist.kt
│   │   ├── login/
│   │   │   └── LoginScreen.kt            # Pantalla de inicio de sesión
│   │   └── register/
│   │       └── RegisterScreen.kt         # Pantalla de registro
│   ├── components/                        # Componentes UI reutilizables
│   │   ├── ActionSuccessScreen.kt
│   │   ├── Brand.kt
│   │   ├── Buttons.kt
│   │   ├── Dialogs.kt
│   │   ├── Inputs.kt
│   │   └── Links.kt
│   ├── splash/
│   │   └── SplashScreen.kt              # Pantalla de carga/bienvenida
│   └── theme/                            # Sistema de diseño y temas (Material 3)
│       ├── Color.kt
│       ├── Spacing.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── navigation/                            # NAVEGACIÓN (Jetpack Navigation Compose)
│   ├── Routes.kt                         # Clases/Objetos de rutas Type-Safe con @Serializable
│   ├── ScreenRoute.kt                    # Mapeo de rutas a títulos
│   └── AppNavigation.kt                  # NavHost y declaración de grafos de pantallas
│
├── notifications/                         # Gestión de notificaciones push
└── utils/                                 # Extensiones y utilidades
    ├── CoroutinesExt.kt                  # Manejo seguro de corrutinas (safeRunCatching)
    ├── ModifierExt.kt                    # Modificadores de UI personalizados
    ├── ShadowExt.kt                      # Sombreados de componentes
    └── SystemBarEffect.kt                # Configuración de barras del sistema
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
- **[ValidateEmailUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/ValidateEmailUseCase.kt):** Valida si la estructura sintáctica de un correo es correcta mediante expresiones regulares.
- **[ValidatePasswordUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/ValidatePasswordUseCase.kt):** Verifica si una contraseña cumple los requisitos (longitud, mayúsculas, caracteres especiales).
- **[LoginUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/LoginUseCase.kt)** y **[RegisterUseCase.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/domain/usecase/auth/RegisterUseCase.kt):** Orquestan las validaciones previas y la llamada al `AuthRepository`.

#### 4. Capa de Interfaz (`ui/auth/`)
Sigue el patrón **UDF (Unidirectional Data Flow)** recomendado por Android:
- **Flujo hacia abajo (State):** El `ViewModel` expone un `StateFlow<AuthUiState>` inmutable.
- **Flujo hacia arriba (Events):** Los elementos UI emiten eventos a través de `onEvent(AuthEvent)`.

- **Autenticación (Centralizada):**
  - [AuthUiState.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthUiState.kt), [AuthEvent.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthEvent.kt) y [AuthEnums.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthEnums.kt): Estado completo (campos, booleanos de validación, carga, errores) y eventos sellados.
  - [AuthViewModel.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/AuthViewModel.kt): Inyecta los UseCases correspondientes para controlar el flujo asíncrono y actualizar la UI de manera reactiva.
  - [LoginScreen.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/login/LoginScreen.kt) y [RegisterScreen.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/ui/auth/register/RegisterScreen.kt): Pantallas de inicio de sesión y registro desarrolladas con Jetpack Compose.

#### 5. Navegación Type-Safe (`navigation/`)
- **[Routes.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/navigation/Routes.kt):** Objetos anotados con `@Serializable` para garantizar el tipado estricto de rutas.
- **[AppNavigation.kt](file:///C:/Users/Zhair/Documents/ZhairGomez/A_Importantes/Codigos/Kotlin/Smartfood/app/src/main/java/com/smartfood/app/navigation/AppNavigation.kt):** Configuración central del `NavHost` con Jetpack Compose y transiciones animadas.

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
* Estructura limpia para `MainActivity.kt` decorada con `@AndroidEntryPoint`, inicializando el contenedor de Compose con el grafo de navegación.
* Creación de la estructura de directorios y paquetes solicitados.

### 2. Módulo de Autenticación (Auth)
Se ha configurado la capa lógica y de interfaz de autenticación siguiendo las mejores prácticas:

* **Data Layer:**
  * `AuthRepository.kt`: Interfaz que abstrae las operaciones de sesión (`login`, `register`, `sendPasswordReset`).
  * `FirebaseAuthDataSource.kt`: Conexión directa y asíncrona con el SDK de Firebase Auth.
  * `AuthRepositoryImpl.kt`: Implementación del repositorio que encapsula las respuestas en objetos `Result<Unit>` de Kotlin.
  * `DataModule.kt`: Módulo de Hilt que enlaza (`@Binds`) la interfaz del repositorio con su implementación y provee (`@Provides`) la instancia de `FirebaseAuth`.

* **UI Layer (UDF):**
  * `AuthUiState.kt`, `AuthEvent.kt` y `AuthEnums.kt`: Estado inmutable (con validaciones dinámicas) y jerarquía de eventos `AuthEvent` para comunicar acciones de la vista al ViewModel.
  * `AuthViewModel.kt`: ViewModel (`@HiltViewModel`) que inyecta UseCases y gestiona el flujo unidireccional de datos de forma asíncrona.
  * `LoginScreen.kt` y `RegisterScreen.kt`: Vistas desacopladas mediante Compose.

### 3. Refactorización a Clean Architecture y Corrutinas Seguras
* **Domain Layer:** Lógica de negocio pura representada en Casos de Uso independientes (`ValidateEmailUseCase`, `ValidatePasswordUseCase`, `LoginUseCase`, `RegisterUseCase`).
* **Seguridad Asíncrona:** Implementación de la extensión `safeRunCatching` para asegurar el respeto a la cancelación estructurada (`CancellationException`) de Kotlin Coroutines en el Data Layer.
