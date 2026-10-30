# Pantallas y ViewModels de SmartFood

Catálogo de las pantallas de `feature/**/*Screen.kt` y sus ViewModels: una ficha por pantalla, para encontrar rápido
cómo está hecha cada una. Los conceptos de Compose (estado, recomposición, `LazyColumn`, navegación…) se explican en
[`como-funcionan-las-pantallas.md`](como-funcionan-las-pantallas.md); aquí solo se describe lo propio de cada pantalla.

## El patrón común

Casi todas las pantallas tienen las mismas cuatro piezas:

| Pieza | Qué hace |
|---|---|
| `XContract.kt` | `XUiState` (lo que se ve), `XEvent` (lo que hace la persona) y `XEffect` (navegación y acciones de un solo uso) |
| `XViewModel.kt` | Recibe cada evento en `onEvent`, cambia el estado con `_uiState.update { it.copy(…) }` y envía los efectos por un `Channel` |
| `XScreen.kt` | Composable **sin estado**: recibe `state` y `onEvent`. Es lo que se prueba y lo que se ve en `@Preview` |
| Ruta | En el `*NavGraph.kt` o en un `XRoute.kt`: obtiene el ViewModel con `hiltViewModel()`, escucha los efectos con `ObserveEffects` y navega |

Cuando una pantalla necesita APIs de Android (permisos, cámara, selector de fotos, "atrás"), esa parte va en un
`XRoute.kt` aparte, para que la `XScreen` siga sin estado.

## Índice

| # | Pantalla | ViewModel | Ruta |
|---|---|---|---|
| 1 | [Splash](#1-splash) | `SplashViewModel` | `Route.Splash` |
| 2 | [Iniciar sesión](#2-iniciar-sesión) | `LoginViewModel` | `Route.Login` |
| 3 | [Registro](#3-registro) | `RegisterViewModel` | `Route.Register` |
| 4 | [Recuperar contraseña](#4-recuperar-contraseña) | `PasswordRecoveryViewModel` | `Route.PasswordRecovery` |
| 5 | [Documentos legales](#5-documentos-legales) | Sin ViewModel | `Route.TermsOfService`, `Route.PrivacyPolicy` |
| 6 | [Mi Inventario](#6-mi-inventario) | `InventoryViewModel` | `Route.Inventory` |
| 7 | [Buscar en el inventario](#7-buscar-en-el-inventario) | `InventorySearchViewModel` | `Route.InventorySearch` |
| 8 | [Añadir o editar producto](#8-añadir-o-editar-producto) | `ProductEditorViewModel` | `Route.ProductEditor` |
| 9 | [Cámara del producto](#9-cámara-del-producto) | Sin ViewModel | `Route.ProductCamera` |
| 10 | [Perfil](#10-perfil) | `ProfileViewModel` | `Route.Profile` |
| 11 | [Editar información](#11-editar-información) | `EditProfileViewModel` | `Route.EditProfile` |
| 12 | [Notificaciones](#12-notificaciones) | `NotificationSettingsViewModel` | `Route.NotificationSettings` |
| 13 | [Cambiar contraseña](#13-cambiar-contraseña) | `ChangePasswordViewModel` | `Route.ChangePassword` |
| 14 | [Preferencias alimentarias](#14-preferencias-alimentarias) | `FoodPreferencesViewModel` | `Route.FoodPreferences` |
| 15 | [Descubrir recetas](#15-descubrir-recetas) | `RecipesDiscoverViewModel` | `Route.Recipes` |
| 16 | [Detalle de receta](#16-detalle-de-receta) | `RecipeDetailViewModel` | `Route.RecipeDetail` |
| 17 | [Sugerencia con IA y Ayuda](#17-sugerencia-con-ia-y-ayuda) | `AiPlanViewModel` (compartido) | `Route.AiPlan`, `Route.AiHelp` |
| 18 | [Cocina guiada](#18-cocina-guiada) | `CookingViewModel` | `Route.CookingIntro`, `Route.Cooking` |

---

# Arranque

## 1. Splash

| | |
|---|---|
| Archivos | [`SplashScreen.kt`](../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt) · [`SplashViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/splash/SplashViewModel.kt) |
| Ruta | `Route.Splash` en [`AppNavHost.kt`](../app/src/main/java/com/smartfood/app/core/navigation/AppNavHost.kt#L43) |
| Spec | 001, HU-03 (sesión recordada) |

**Qué hace.** Muestra el logo mientras decide si hay sesión guardada. Con sesión va a "Mi Inventario"; sin sesión, a
"Iniciar sesión".

**Estado.** No usa Contract: el ViewModel expone solo `destination: StateFlow<SplashDestination?>` (`null` mientras
consulta, luego `HOME` o `LOGIN`).

**Dependencias del ViewModel.** `ResolveSessionUseCase` (¿hay sesión?) y `ExpiryReminderScheduler` (con sesión,
reprograma el aviso diario de vencimientos).

**Compose en esta pantalla.**
- Dos [`LaunchedEffect`](../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L53): uno espera el tiempo mínimo y otro navega cuando se
  cumplen las dos condiciones.
- `SystemBarEffect(isLightBackground = true)`: íconos oscuros en la barra de estado, porque el fondo es claro.

**Notas técnicas.**
- El tiempo mínimo (2 s, CA-03-04) vive en la **pantalla**, no en el ViewModel: el ViewModel solo resuelve la sesión
  y la pantalla decide cuándo navegar. Así, si la sesión se resuelve en 200 ms, el logo igual se ve 2 s.
- Al navegar se hace `popUpTo<Route.Splash> { inclusive = true }`: "atrás" nunca vuelve al Splash.

Archivo: [SplashScreen.kt, líneas 52–59](../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L52-L59)

```kotlin
var minimumTimeElapsed by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {                                    // una vez: espera 2 s
    delay(MIN_SPLASH_DURATION_MS)
    minimumTimeElapsed = true
}
LaunchedEffect(minimumTimeElapsed, destination) {         // se repite si cambia cualquiera de las dos claves
    if (minimumTimeElapsed && destination != null) onNavigate(destination)
}
```

---

# Autenticación

## 2. Iniciar sesión

| | |
|---|---|
| Archivos | [`LoginScreen.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginScreen.kt) · [`LoginContract.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginContract.kt) · [`LoginViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginViewModel.kt) |
| Ruta | `Route.Login` en [`AuthNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/AuthNavGraph.kt#L40) |
| Spec | 001, HU-02 y HU-06; Figma `585:4420` |

**Qué hace.** Correo, contraseña, "Recuérdame", "¿Olvidaste tu contraseña?" y "Regístrate". Sin conexión muestra el
diálogo de reconexión.

**Estado (`LoginUiState`).**

| Campo | Para qué |
|---|---|
| `email`, `password`, `rememberMe` | Lo escrito; "Recuérdame" empieza activo (CA-03-01) |
| `isLoading` | Botón en "Iniciando sesión…" y campos bloqueados |
| `error: AuthError?` | El último error |
| `isOffline`, `emailHasError`, `passwordHasError` | **Derivados** de `error` |

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `EmailChanged`, `PasswordChanged` | Guarda el texto y borra el error |
| `RememberMeChanged` | Cambia la casilla |
| `LoginClicked`, `RetryConnectionClicked` | `submit()`: llama a `LoginUseCase` |

**Efectos.** `NavigateToHome` → `navigateToMainGraph()`, que borra la pila. Antes, `autofill?.commit()` ofrece guardar
la contraseña en el teléfono (CA-07-02).

**Dependencias.** `LoginUseCase` (valida, inicia sesión en Firebase y guarda "Recuérdame").

**Compose en esta pantalla.** Usa [`AuthScreenScaffold.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/components/AuthScreenScaffold.kt), el armazón común de autenticación: encabezado de marca que
se encoge con el teclado, panel inferior con el inset del teclado y perfiles `Compact`/`Medium`/`Regular`.

**Notas técnicas.** Esta pantalla está explicada línea por línea en la guía de conceptos (secciones 1.2, 2.1, 2.2 y 4.1).
Lo esencial: el ViewModel **no valida** nada; las reglas están en `LoginUseCase`.

Archivo: [LoginViewModel.kt, líneas 53–70](../app/src/main/java/com/smartfood/app/feature/auth/ui/login/LoginViewModel.kt#L53-L70)

```kotlin
private fun submit() {
    val state = _uiState.value
    if (state.isLoading) return                            // ignora el doble toque
    // Si se reintenta tras un error de red, el diálogo sigue visible mostrando "Conectando" (CA-06-02).
    _uiState.update { it.copy(isLoading = true, error = it.error.takeIf { e -> e == AuthError.Network }) }
    viewModelScope.launch {
        login(state.email, state.password, state.rememberMe)
            .onSuccess {
                _uiState.update { it.copy(isLoading = false, error = null) }
                _effects.send(LoginEffect.NavigateToHome)
            }
            .onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, error = throwable.toAuthError()) }
            }
    }
}
```

## 3. Registro

| | |
|---|---|
| Archivos | [`RegisterScreen.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt) · [`RegisterContract.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterContract.kt) · [`RegisterViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterViewModel.kt) |
| Ruta | `Route.Register` en [`AuthNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/AuthNavGraph.kt#L63) |
| Spec | 001, HU-01; 005 (términos) |

**Qué hace.** Nombre, correo, contraseña con la lista de requisitos en vivo y la casilla de Términos y Política. Al
registrarse va a "¡Cuenta creada con éxito!".

**Estado (`RegisterUiState`).** `fullName`, `email`, `password`, `passwordValidation` (qué reglas cumple la contraseña
en este momento), `acceptedTerms`, `isLoading`, `error`, y los derivados `fullNameHasError`, `emailHasError` y
`passwordHasError`.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `FullNameChanged`, `EmailChanged` | Guarda el texto y borra el error |
| `PasswordChanged` | Guarda el texto **y recalcula** `passwordValidation` con `ValidatePasswordUseCase` |
| `AcceptedTermsChanged` | Cambia la casilla |
| `RegisterClicked`, `RetryConnectionClicked` | `submit()`: llama a `RegisterUseCase` |

**Efectos.** `NavigateToSuccess` → `Route.RegisterSuccess` con `popUpTo<Route.Register> { inclusive = true }`, para que
"atrás" no vuelva al formulario.

**Dependencias.** `RegisterUseCase` (crea la cuenta, el perfil con la constancia de términos y cierra la sesión) y
`ValidatePasswordUseCase`.

**Compose en esta pantalla.**
- [`PasswordFieldWithChecklist`](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L163): la lista de requisitos sale en un
  popup **encima** del campo mientras está enfocado.
- [`SmartFoodTermsCheckbox`](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterScreen.kt#L77): tocar "Términos" o "Política" abre el
  documento; tocar el resto del texto marca la casilla. Los enlaces también se ofrecen a TalkBack.

**Notas técnicas.** La lista de requisitos se calcula en el ViewModel con cada letra, no en la pantalla: es la misma
regla que usa el caso de uso al registrar, así que nunca se contradicen.

Archivo: [RegisterViewModel.kt, líneas 51–57](../app/src/main/java/com/smartfood/app/feature/auth/ui/register/RegisterViewModel.kt#L51-L57)

```kotlin
is RegisterEvent.PasswordChanged -> _uiState.update {
    it.copy(
        password = event.password,
        passwordValidation = validatePassword.execute(event.password),   // checklist en vivo (CA-01-03)
        error = null
    )
}
```

## 4. Recuperar contraseña

| | |
|---|---|
| Archivos | [`PasswordRecoveryScreen.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt) · [`PasswordRecoveryContract.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryContract.kt) · [`PasswordRecoveryViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryViewModel.kt) |
| Ruta | `Route.PasswordRecovery` en [`AuthNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/auth/ui/AuthNavGraph.kt#L97) |
| Spec | 001, HU-04; 003 (recuperación real con Cloud Functions) |

**Qué hace.** Tres pasos en una sola pantalla: correo → código de 4 dígitos → nueva contraseña, con indicador de pasos
y cuenta regresiva para reenviar el código.

**Estado (`PasswordRecoveryUiState`).**

| Campo | Para qué |
|---|---|
| `step: RecoveryStep` | `EMAIL_REQUEST`, `OTP_VERIFICATION` o `NEW_PASSWORD` |
| `email`, `otpCode`, `newPassword`, `confirmPassword` | Lo escrito en cada paso |
| `passwordValidation` | Requisitos de la nueva contraseña |
| `resendCooldownSeconds`, `cooldownEmail` | Cuenta regresiva y a qué correo aplica |
| `isEmailInCooldown` | **Derivado**: la espera aplica solo al correo escrito; otro correo no espera |
| `canResetPassword` | **Derivado**: contraseña válida y confirmada |

El código correcto **no** está en el estado: solo lo conoce el servidor.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `SendCodeClicked`, `ResendCodeClicked` | `sendCode()`: pide el código, pasa al paso 2 y arranca la cuenta regresiva |
| `VerifyCodeClicked` | `verify()`: comprueba el código y pasa al paso 3 |
| `ChangeEmailClicked` | Vuelve al paso 1 y borra el código |
| `ResetPasswordClicked` | `reset()`: cambia la contraseña; si el código venció, vuelve al paso 2 |
| `…Changed` | Guardan el texto; `NewPasswordChanged` recalcula los requisitos |

**Efectos.** `NavigateToSuccess` → `Route.PasswordRecoverySuccess`, quitando la recuperación de la pila.

**Dependencias.** `RequestRecoveryCodeUseCase`, `VerifyRecoveryCodeUseCase`, `ResetPasswordUseCase` y
`ValidatePasswordUseCase`.

**Compose en esta pantalla.**
- [`AnimatedContent`](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreen.kt#L84) con fundido entre pasos.
- `SmartFoodStepIndicator`, `SmartFoodOtpCodeInput` (4 casillas) y `SmartFoodPromptCountdown`.

**Notas técnicas.**
- Los tres pasos comparten **un solo ViewModel** y una sola ruta: el correo y el código no viajan entre pantallas.
- [`runStep`](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryViewModel.kt#L139) es una función genérica que centraliza la carga y
  los errores de los tres pasos. Si el servidor responde "demasiadas solicitudes", arranca la espera real que informa.
- El `AnimatedContent` usa `.using(null)` (sin `SizeTransform`): cada paso ya llena el alto, y animar el tamaño solo
  alargaría el alto del paso anterior.

Archivo: [PasswordRecoveryViewModel.kt, líneas 102–113](../app/src/main/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryViewModel.kt#L102-L113)

```kotlin
private fun startCooldown(email: String, wait: Duration) {
    cooldownJob?.cancel()                                  // una sola cuenta regresiva a la vez
    val seconds = ((wait.inWholeMilliseconds + 999) / 1000).toInt()
    _uiState.update { it.copy(resendCooldownSeconds = seconds, cooldownEmail = email.trim()) }
    cooldownJob = viewModelScope.launch {
        while (_uiState.value.resendCooldownSeconds > 0) {
            delay(1.seconds)
            _uiState.update { it.copy(resendCooldownSeconds = (it.resendCooldownSeconds - 1).coerceAtLeast(0)) }
        }
        _uiState.update { if (it.error is AuthError.TooManyRequests) it.copy(error = null) else it }
    }
}
```

## 5. Documentos legales

| | |
|---|---|
| Archivos | [`LegalDocumentScreen.kt`](../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt) · [`LegalContent.kt`](../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalContent.kt) |
| Ruta | `Route.TermsOfService` y `Route.PrivacyPolicy` en [`LegalNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalNavGraph.kt#L20) |
| Spec | 005, HU-LEG-01 |

**Qué hace.** Muestra los Términos y condiciones o la Política de privacidad según `LegalDocumentType`, con el aviso de
versión de vista previa.

**Sin ViewModel.** La pantalla no tiene estado ni eventos propios: recibe `type` y `onBack`. El texto sale de
`res/values/strings_legal.xml` a través de `LegalContent.kt`, el único lugar que relaciona el tipo con sus recursos.

**Compose en esta pantalla.**
- [`LazyColumn`](../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L97) con las secciones; los títulos se anuncian como encabezados para
  TalkBack (CA-LEG-01-06).
- `widthIn(max = …)` en el texto: en pantallas anchas, las líneas no pasan de un largo cómodo de leer.
- `SystemBarEffect(isLightBackground = !LocalIsDarkTheme.current)`: los íconos de la barra siguen el modo de color.

**Notas técnicas.** Se registra en la **raíz** del `AppNavHost` para que se pueda abrir desde el registro (sin
sesión) y desde cualquier otra parte. Al publicar el texto definitivo se cambian `LegalVersion.CURRENT` e `IS_PREVIEW`.

---

# Inventario

## 6. Mi Inventario

| | |
|---|---|
| Archivos | [`InventoryScreen.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt) · [`InventoryContract.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryContract.kt) · [`InventoryViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryViewModel.kt) |
| Ruta | `Route.Inventory` (pestaña) en [`InventoryNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/InventoryNavGraph.kt#L41) |
| Spec | 006, HU-INV-01; Figma `476:3095` |

**Qué hace.** Lista de productos agrupados por vencimiento ("Vencido", "Vence pronto"…), con buscador, botón "+" y los
estados de carga, vacío y error.

**Estado (`InventoryUiState`).** `isLoading`, `sections: List<InventorySection>`, `error`, y el derivado `isEmpty`.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `AddClicked` | Efecto `NavigateToEditor()` (sin id: producto nuevo) |
| `EditClicked(itemId)` | Efecto `NavigateToEditor(itemId)` |
| `SearchClicked` | Efecto `NavigateToSearch` |
| `RetryClicked` | Vuelve a observar el inventario |

**Dependencias.** `ObserveInventoryUseCase`: un `Flow` en vivo de Firestore ya agrupado por estado de vencimiento.

**Compose en esta pantalla.**
- `LazyColumn` con encabezados y filas, cada uno con su `key`.
- [`InventorySkeleton.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventorySkeleton.kt): esqueleto con brillo animado mientras carga.
- [`EditorResultHost.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/EditorResultHost.kt): muestra encima de la lista el resultado que dejó el editor ("Guardado", "Eliminado", "Guardado
  en tu teléfono").

**Notas técnicas.**
- La lista se actualiza sola cuando Firestore cambia, también desde la caché sin conexión, y cada fila marca si tiene
  cambios sin subir (`isPendingSync`).
- La observación vive en `viewModelScope` y sigue activa con la app minimizada. La búsqueda (ficha 7) ya usa
  `stateIn(WhileSubscribed)`, que la detiene; es una mejora pendiente para esta lista.
- Explicada línea por línea en la guía de conceptos (secciones 3.2, 3.3, 4.2 y 4.3).

Archivo: [InventoryViewModel.kt, líneas 67–79](../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryViewModel.kt#L67-L79)

```kotlin
private fun observeInventory() {
    observeJob?.cancel()                                   // al reintentar, nunca dos observaciones a la vez
    observeJob = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        observeInventoryUseCase()
            .catch { cause -> _uiState.update { it.copy(isLoading = false, error = cause.toInventoryError()) } }
            .collect { sections -> _uiState.update { it.copy(isLoading = false, sections = sections, error = null) } }
    }
}
```

## 7. Buscar en el inventario

| | |
|---|---|
| Archivos | [`InventorySearchScreen.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt) · [`InventorySearchContract.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchContract.kt) · [`InventorySearchViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchViewModel.kt) · [`InventorySearchRoute.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchRoute.kt) |
| Ruta | `Route.InventorySearch` en [`InventoryNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/InventoryNavGraph.kt#L64) |
| Spec | 006, HU-INV-06 (texto) y HU-INV-07 (voz); Figma `476:3190` y `476:5960` |

**Qué hace.** Busca por texto y por categoría a la vez. Con "Usa tu voz" se dictan ingredientes ("carne, leche,
tomate") que llenan la barra.

**Estado (`InventorySearchUiState`).**

| Campo | Para qué |
|---|---|
| `query`, `selectedCategory` | Texto y filtro (`null` = "Todos") |
| `results: List<SearchResultUi>` | Producto + su estado de vencimiento |
| `isVoiceAvailable` | Si el teléfono tiene reconocimiento de voz; si no, la tarjeta no se muestra |
| `voice: VoiceUiState` | `Hidden`, `Listening`, `Processing`, `Recognized(texto)` o `Failed(error)` |
| `isEmptyResult` | **Derivado**: terminó de cargar y no hay resultados |

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `QueryChanged`, `ClearQuery`, `CategorySelected` | Cambian un `MutableStateFlow`; el `combine` recalcula los resultados |
| `VoiceClicked` | Efecto `RequestMicPermission` |
| `VoicePermissionResult(granted)` | Concedido: empieza a escuchar. Negado: efecto `ShowPermissionRationale` |
| `VoiceRetry` / `VoiceDismissed` | Vuelve a escuchar / cancela la escucha |
| `ResultClicked(id)`, `BackClicked` | Efectos `NavigateToEditor(id)` y `NavigateBack` |

**Dependencias.** `ObserveInventoryUseCase`, `SearchInventoryUseCase`, `SpeechToText` (implementado en
[`AndroidSpeechToText.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/data/speech/AndroidSpeechToText.kt) con `SpeechRecognizer`) y `ParseSpokenIngredientsUseCase`.

**Compose en esta pantalla.**
- `LazyColumn` con [`key = { it.item.id }`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L132).
- `VoiceSearchOverlay`: el aviso de voz dibujado **encima** de la pantalla.
- En [`InventorySearchRoute.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchRoute.kt): `rememberLauncherForActivityResult(RequestPermission())` para `RECORD_AUDIO` y el
  enlace a los ajustes si se niega.

**Notas técnicas.**
- Es el único ViewModel del inventario que arma el estado con `combine(…).stateIn(WhileSubscribed(5_000))`: el
  estado **no se guarda**, se **calcula** de cuatro flujos. A los 5 s de salir de la pantalla deja de escuchar
  Firestore; el margen evita reiniciar todo al girar el teléfono.
- El estado de vencimiento de cada resultado sale de las **mismas secciones** de la lista: no se vuelve a clasificar.
- Al cerrar el aviso, `voiceJob?.cancel()` libera el micrófono; `onCleared()` lo hace también al salir.

Archivo: [InventorySearchViewModel.kt, líneas 67–93](../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchViewModel.kt#L67-L93)

```kotlin
val uiState: StateFlow<InventorySearchUiState> = combine(
    observeInventoryUseCase().catch { emit(emptyList()) },   // si falla, sin resultados en vez de cerrar la app
    query,
    selectedCategory,
    voice
) { sections, text, category, voiceState ->
    // … estado de vencimiento por id y búsqueda con SearchInventoryUseCase …
    InventorySearchUiState(query = text, selectedCategory = category, results = results,
        isVoiceAvailable = isVoiceAvailable, voice = voiceState)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(StopTimeoutMs),  // 5 s sin pantalla → deja de escuchar
    initialValue = InventorySearchUiState(isLoading = true)
)
```

## 8. Añadir o editar producto

| | |
|---|---|
| Archivos | [`ProductEditorScreen.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt) · [`ProductEditorContract.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorContract.kt) · [`ProductEditorViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorViewModel.kt) · [`ProductEditorRoute.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorRoute.kt) |
| Ruta | `Route.ProductEditor(itemId: String?)` en [`InventoryNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/InventoryNavGraph.kt#L77) |
| Spec | 006, HU-INV-02 a HU-INV-05 y HU-INV-10; Figma `476:3664` (añadir) y `476:3689` (editar) |

**Qué hace.** Formulario con foto, nombre, vencimiento, categoría, cantidad con unidad y precio. Con `itemId` edita y
permite eliminar; sin `itemId` añade.

**Estado (`ProductEditorUiState`).**

| Campo | Para qué |
|---|---|
| `mode` | `ADD` o `EDIT`, según si llegó `itemId` |
| `name`, `expiresOn`, `category`, `quantityText`, `unit`, `priceText` | Lo escrito; cantidad y precio como texto |
| `photo: PhotoUiState` | `None`, `Local` (recién tomada), `Remote` (en la nube) o `Pending` (guardada, esperando subir) |
| `isSaveEnabled` | Hay nombre, fecha y categoría |
| `savingAction` | `ADD`, `UPDATE` o `DELETE` mientras se guarda: el texto del diálogo de carga |
| `dialog: EditorDialog?` | Hoja de foto, de categoría o de unidad, calendario o confirmaciones |
| `missingFields`, `errorField`, `generalError` | **Derivados**: qué campos van en rojo y qué error va debajo |

**Eventos → ViewModel** (20 en total; los principales).

| Evento | Qué hace |
|---|---|
| `NameChanged`, `QuantityChanged`, `PriceChanged` | `edit { }`: guarda, borra el error y recalcula `isSaveEnabled` |
| `ExpiryClicked`, `CategoryClicked`, `UnitClicked`, `AddPhotoClicked` | Abren su `dialog` |
| `…Picked` | Guardan la elección y cierran el diálogo |
| `GalleryChosen` / `CameraChosen` | Efectos `OpenGallery` / `NavigateToCamera` |
| `PhotoCaptured(path)` | Pone la foto nueva y, si había otra local, pide borrarla del teléfono |
| `SaveClicked` | Botón gris: marca los campos que faltan. Si no: `AddItemUseCase` o `UpdateItemUseCase` |
| `DeleteClicked` → `DeleteConfirmed` | Confirma y llama a `DeleteItemUseCase` |
| `BackClicked` | Descarta los cambios y la foto nueva, y vuelve |

**Efectos.** `NavigateToCamera`, `OpenGallery`, `NavigateBack`, `NavigateBackWithResult(result)` (la lista muestra
"Guardado", "Actualizado", "Eliminado" o "Guardado en tu teléfono"), `DeleteLocalPhoto(path)` y
`RequestNotificationPermission` (solo tras el primer guardado).

**Dependencias.** `SavedStateHandle` (lee `itemId`), `GetItemUseCase`, `ValidateItemUseCase`, `AddItemUseCase`,
`UpdateItemUseCase`, `DeleteItemUseCase` e `InventoryPreferences` (recuerda si ya se pidió el permiso).

**Compose en esta pantalla.**
- [`BoxWithConstraints`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L127) + `formMetricsFor(maxHeight)`: medidas según el
  alto, calculadas **sin** el teclado (`imePadding` va adentro).
- [`EditorDialogs`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L292): un `when (state.dialog)` dibuja la hoja o el
  diálogo abierto.
- En [`ProductEditorRoute.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorRoute.kt): el selector de fotos (`PickVisualMedia`), el permiso de notificaciones,
  [`BackHandler`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorRoute.kt#L79) y la foto que
  devuelve la cámara por el `savedStateHandle` ([`LaunchedEffect(capturedPhoto)`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorRoute.kt#L72)).

**Notas técnicas.**
- Las fotos del teléfono se borran con **efectos** (`DeleteLocalPhoto`) y no desde el ViewModel: borrar un archivo es
  trabajo de disco y la Route lo hace en `Dispatchers.IO`.
- El diálogo de carga sigue visible **hasta que el editor se va**, para no dejar ver el formulario un instante antes de
  volver a la lista.
- Sin conexión, `WriteOutcome.PENDING` se convierte en el resultado "Guardado en tu teléfono".

Archivo: [ProductEditorViewModel.kt, líneas 262–269](../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorViewModel.kt#L262-L269)

```kotlin
/** Qué pasa con la foto al guardar una edición (CA-INV-05-04, CA-INV-05-05). */
internal fun photoEditFor(original: InventoryItem, photo: PhotoUiState): PhotoEdit = when (photo) {
    is PhotoUiState.Local -> PhotoEdit.Replace(LocalPhoto(photo.path))       // foto nueva: reemplaza
    // Quitar una foto que espera subirse también es quitarla: así se cancela su subida.
    PhotoUiState.None ->
        if (original.photo != null || original.localPhotoPath != null) PhotoEdit.Remove else PhotoEdit.Keep
    // La foto pendiente ya está en cola: se conserva (T050).
    is PhotoUiState.Remote, is PhotoUiState.Pending -> PhotoEdit.Keep
}
```

## 9. Cámara del producto

| | |
|---|---|
| Archivos | [`ProductCameraScreen.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt) · [`ProductCameraRoute.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraRoute.kt) · [`CameraCapture.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/CameraCapture.kt) |
| Ruta | `Route.ProductCamera` en [`InventoryNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/InventoryNavGraph.kt#L106) |
| Spec | 006, HU-INV-05; Figma `476:5959` |

**Qué hace.** Visor de la cámara con el recuadro de encuadre y el botón de captura. Si el permiso está negado, explica
para qué se usa y ofrece abrir los ajustes.

**Sin ViewModel.** Solo devuelve la ruta de la foto: guardarla es decisión del editor. Su poco estado (permiso, "tomando
foto", "falló") vive en `remember` dentro de [`ProductCameraRoute`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraRoute.kt#L42).

**Estado de la pantalla.** `ProductCameraScreen` recibe `permission: CameraPermission` (`UNKNOWN`, `GRANTED` o `DENIED`),
`isCapturing`, `captureFailed` y la vista previa como parámetro (`preview: @Composable () -> Unit`).

**Compose en esta pantalla.**
- [`AndroidView`](../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraRoute.kt#L75) con el `PreviewView` de CameraX (una View clásica dentro de
  Compose).
- `LaunchedEffect(Unit)` pide el permiso al abrir; `LifecycleResumeEffect` lo vuelve a revisar al regresar de los
  ajustes.
- `LifecycleCameraController` creado una vez con `remember` y conectado al ciclo de vida.

**Notas técnicas.**
- La vista previa entra como **slot**: `ProductCameraScreen` se prueba sin cámara real (`ProductCameraScreenTest`).
- La foto vuelve al editor por el `savedStateHandle` de la pantalla anterior (`CAPTURED_PHOTO_KEY`).

---

# Perfil

## 10. Perfil

| | |
|---|---|
| Archivos | [`ProfileScreen.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt) · [`ProfileViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileViewModel.kt) (el contrato está en el mismo archivo) · [`ProfileAvatar.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileAvatar.kt) |
| Ruta | `Route.Profile` (pestaña) en [`ProfileNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileNavGraph.kt#L32) |
| Spec | 001 HU-05; 006 CA-INV-09-03; 008 HU-PER-01 y HU-PER-06; Figma `306:369` |

**Qué hace.** Encabezado con foto o iniciales, nombre y correo; tres tarjetas de cifras (entre ellas, los
ingredientes en inventario y lo ahorrado este mes); opciones de la cuenta, tema de la app y "Cerrar sesión".

**Estado (`ProfileUiState`).** `isLoading`, `firstName`, `fullName`, `email`, `initials`, `photoUrl`, `inventoryCount`,
`monthlySavingsCop`, `themeMode`, `isThemeSheetVisible` y `dialog` (`ConfirmLogout(hasPendingChanges)`).

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `LogoutClicked` | Pregunta si hay cambios sin subir y abre "¿Cerrar sesión?" |
| `LogoutConfirmed` | `LogoutUseCase` y efecto `NavigateToLogin` |
| `EditProfileClicked`, `PreferencesClicked`, `NotificationsClicked`, `PasswordClicked` | Efecto de navegación a cada pantalla |
| `ThemeClicked` → `ThemeSelected(mode)` | Abre la hoja de tema y guarda el modo elegido |

**Dependencias.** `ObserveProfileUseCase`, `SyncProfileEmailUseCase`, `LogoutUseCase`, `ObserveInventoryUseCase`,
`HasPendingChangesUseCase`, `ThemePreferences` y `GetMonthlySavingsUseCase`.

**Compose en esta pantalla.**
- [`BoxWithConstraints`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L109) + [`profileDimens(maxWidth, maxHeight)`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileScreen.kt#L454):
  las medidas se ajustan al ancho y al alto.
- `AsyncImage` (Coil) para la foto, o las iniciales en un círculo.
- `SmartFoodWarningDialog`, que va en rojo si hay cambios sin subir, y `OptionSheet` para el tema.

**Notas técnicas.**
- El ViewModel lanza **cuatro observaciones** en paralelo (perfil, inventario, ahorro y tema). Cada una tiene su
  `.catch`: si una falla, las demás siguen y la pantalla no se cierra.
- Elegir el tema actualiza el estado **antes** de guardarlo en DataStore, así la hoja se cierra y el modo cambia al
  instante.
- La KDoc de `ProfileScreen.kt` dice que "Cambiar contraseña" se ve deshabilitada, pero hoy navega a su pantalla
  (ficha 13). La KDoc está desactualizada.

Archivo: [ProfileViewModel.kt, líneas 107–113](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileViewModel.kt#L107-L113)

```kotlin
// Siempre se pide confirmar (Figma 604:2668, CA-05-03); con cambios sin subir, el aviso dice que podrían perderse.
ProfileEvent.LogoutClicked -> viewModelScope.launch {
    _uiState.update { it.copy(dialog = ProfileDialog.ConfirmLogout(hasPendingChanges = hasPendingChanges())) }
}
ProfileEvent.LogoutConfirmed -> {
    _uiState.update { it.copy(dialog = null) }
    viewModelScope.launch { logoutAndLeave() }
}
```

## 11. Editar información

| | |
|---|---|
| Archivos | [`EditProfileScreen.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt) · [`EditProfileContract.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileContract.kt) · [`EditProfileViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileViewModel.kt) · [`EditProfileRoute.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileRoute.kt) · [`EmailCodeDialog.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EmailCodeDialog.kt) |
| Ruta | `Route.EditProfile` en [`ProfileNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileNavGraph.kt#L57) |
| Spec | 008, HU-PER-02; Figma `361:649`, `369:1195` y `369:905` |

**Qué hace.** Foto, nombre, correo, teléfono y fecha de nacimiento. Si el correo cambia, pide la contraseña actual y
luego un código enviado al correo nuevo.

**Estado (`EditProfileUiState`).**

| Campo | Para qué |
|---|---|
| `original: UserProfile?` | El perfil guardado, para comparar |
| `fullName`, `email`, `phone`, `birthDate`, `newPhotoPath` | Lo editado |
| `currentPassword`, `emailCode`, `resendCooldownSeconds` | Solo para el cambio de correo |
| `isEmailChanged`, `hasProfileChanges`, `hasChanges`, `isSaveEnabled` | **Derivados** de comparar con `original` |
| `errorField` | **Derivado**: qué campo va en rojo según el `AuthError` |
| `dialog` | Hoja de foto, calendario, "¿Guardar?", "Revisa tu correo" o "Guardado" |

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `…Changed` | `edit { }`: guarda y borra el error |
| `SaveClicked` → `SaveConfirmed` | Pide confirmar y luego `save()` |
| `EmailCodeChanged`, `ConfirmEmailCode`, `ResendEmailCode`, `CancelEmailCode` | El diálogo "Revisa tu correo" |
| `PhotoClicked`, `GalleryChosen`, `CameraChosen`, `PhotoPicked` | Foto nueva (la anterior sin guardar se borra) |
| `BackClicked` | Descarta sin preguntar y vuelve |

**Efectos.** `OpenCamera`, `OpenGallery`, `DeleteLocalPhoto(path)` y `NavigateBack`.

**Dependencias.** `ObserveProfileUseCase`, `ValidateProfileUseCase`, `UpdateProfileUseCase`, `RequestEmailChangeUseCase`
y `ConfirmEmailChangeUseCase` (Cloud Functions `requestEmailChangeCode` y `confirmEmailChange`).

**Compose en esta pantalla.**
- [`EmailCodeDialog`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreen.kt#L240) y `BirthDatePicker`.
- En [`EditProfileRoute.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileRoute.kt): `ActivityResultContracts.TakePicture()` (cámara del sistema, no CameraX),
  `PickVisualMedia()` y `BackHandler`.

**Notas técnicas.**
- El formulario se llena con `observeProfile().first()`: **solo la primera lectura**. Si se observara en vivo, cada
  cambio en Firestore pisaría lo que la persona está escribiendo.
- Orden del guardado: primero valida los datos, después pide el código y por último guarda el perfil. Así no se envía
  un código si el guardado no va a poder seguir.
- El correo cambia **sin cerrar la sesión** al confirmar el código.

Archivo: [EditProfileViewModel.kt, líneas 144–151](../app/src/main/java/com/smartfood/app/feature/profile/ui/edit/EditProfileViewModel.kt#L144-L151)

```kotlin
val result = safeRunCatching {
    // Primero los datos: así no se envía el código si después el guardado no puede seguir.
    validateProfile(state.fullName, state.phone, state.birthDate).getOrThrow()
    val wait = newEmail?.let { requestEmailChange(it, state.currentPassword).getOrThrow() }
    val edits = ProfileEdits(state.fullName, state.phone, state.birthDate, state.newPhotoPath)
    val saved = if (state.hasProfileChanges) updateProfile(original, edits).getOrThrow() else original
    saved to wait
}
```

## 12. Notificaciones

| | |
|---|---|
| Archivos | [`NotificationSettingsScreen.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt) · [`NotificationSettingsContract.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsContract.kt) · [`NotificationSettingsViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsViewModel.kt) · [`NotificationSettingsRoute.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsRoute.kt) |
| Ruta | `Route.NotificationSettings` en [`ProfileNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileNavGraph.kt#L75) |
| Spec | 008, HU-PER-04; Figma `593:2079` |

**Qué hace.** Tres interruptores: productos por vencer, recetas sugeridas e inventario vacío. Cada uno se guarda al
tocarlo, sin botón "Guardar".

**Estado (`NotificationSettingsUiState`).** `isLoading`, `settings: NotificationSettings` y `dialog`
(`PermissionDenied`). La función `isOn(setting)` lee el valor de cada interruptor.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `ToggleRequested(setting, enabled)` | Cambia el estado y guarda de inmediato |
| `PermissionDenied` | Abre el aviso que explica cómo activar el permiso |
| `OpenSettingsClicked` | Efecto `OpenAppSettings` |
| `BackClicked` | Efecto `NavigateBack` |

**Dependencias.** `NotificationSettingsRepository` (DataStore del teléfono) y `SetNotificationSettingUseCase`, que
además programa o cancela el aviso diario.

**Compose en esta pantalla.** Material 3 `Switch` con colores propios ([`notificationSwitchColors()`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreen.kt#L164)).

**Notas técnicas.**
- La **Route** intercepta `onEvent`: si se enciende un interruptor y falta el permiso `POST_NOTIFICATIONS` (Android 13+,
  [`needsNotificationPermission`](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsRoute.kt#L82)), primero lo
  pide y el evento no llega todavía al ViewModel. Si se niega, avisa con `PermissionDenied`.
- Actualización **optimista**: el interruptor cambia en pantalla antes de terminar de guardar.

Archivo: [NotificationSettingsViewModel.kt, líneas 72–76](../app/src/main/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsViewModel.kt#L72-L76)

```kotlin
/** Cambia un interruptor y guarda de inmediato, sin botón "Guardar" (CA-PER-04-02). */
private fun change(setting: NotificationSetting, enabled: Boolean) {
    val updated = _uiState.value.settings.with(setting, enabled)
    _uiState.update { it.copy(settings = updated) }              // la pantalla cambia ya
    viewModelScope.launch { setNotificationSetting(updated) }    // guarda y (re)programa el aviso diario
}
```

## 13. Cambiar contraseña

| | |
|---|---|
| Archivos | [`ChangePasswordScreen.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreen.kt) (con `ChangePasswordRoute`) · [`ChangePasswordContract.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordContract.kt) · [`ChangePasswordViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordViewModel.kt) |
| Ruta | `Route.ChangePassword` en [`ProfileNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileNavGraph.kt#L84) |
| Spec | 008, HU-PER-05; Figma `603:2358` y `603:2460` |

**Qué hace.** Contraseña actual, nueva (con la lista de requisitos) y confirmación. Al guardar muestra "Guardado" y
vuelve al Perfil.

**Estado (`ChangePasswordUiState`).** `currentPassword`, `newPassword`, `confirmPassword`, `passwordValidation`,
`isLoading`, `isSavedSuccess`, `currentPasswordError` y `generalError`, más los derivados `passwordsMatch` y `canSubmit`.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `CurrentPasswordChanged`, `ConfirmPasswordChanged` | Guardan el texto y borran los errores |
| `NewPasswordChanged` | Guarda y recalcula los requisitos |
| `SubmitClicked` | Si `canSubmit`: `ChangePasswordUseCase` (reautentica y cambia) |
| `SuccessDialogDismissed` | Cierra "Guardado" y vuelve |

**Efectos.** `NavigateBack`.

**Dependencias.** `ChangePasswordUseCase` y `ValidatePasswordUseCase`.

**Compose en esta pantalla.** `PasswordFieldWithChecklist` (igual que el registro) y `SmartFoodResultDialog` cuando
`isSavedSuccess`.

**Notas técnicas.**
- Usa **dos** errores separados: `currentPasswordError` pinta en rojo solo el campo de la contraseña actual
  (`InvalidCredentials`), y `generalError`, cualquier otro.
- Es la única pantalla del Perfil cuya Route vive en el mismo archivo que la Screen
  ([`ChangePasswordRoute`](../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreen.kt#L59)). No tiene `BackHandler`: "atrás" vuelve
  directo al Perfil.
- Tras el éxito espera 2 s y vuelve sola (`SUCCESS_DELAY_MS`).

Archivo: [ChangePasswordViewModel.kt, líneas 94–103](../app/src/main/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordViewModel.kt#L94-L103)

```kotlin
onFailure = { throwable ->
    val error = throwable.toAuthError()
    _uiState.update {
        if (error == AuthError.InvalidCredentials) {
            it.copy(isLoading = false, currentPasswordError = error)   // solo el campo "Contraseña actual"
        } else {
            it.copy(isLoading = false, generalError = error)           // aviso general
        }
    }
}
```

## 14. Preferencias alimentarias

| | |
|---|---|
| Archivos | [`FoodPreferencesScreen.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt) · [`FoodPreferencesContract.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesContract.kt) · [`FoodPreferencesViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesViewModel.kt) · [`FoodPreferencesRoute.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesRoute.kt) · [`PreferenceChip.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/PreferenceChip.kt) |
| Ruta | `Route.FoodPreferences` en [`ProfileNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/profile/ui/ProfileNavGraph.kt#L66) |
| Spec | 008, HU-PER-03; Figma `376:1260`, `593:1976` y `593:1898` |

**Qué hace.** Chips de restricciones (varias), cocinas (varias), picante (una), presupuesto (una) y "Mi supermercado"
(siempre una). El agente de recetas usa las restricciones y la tienda.

**Estado (`FoodPreferencesUiState`).** `saved` (lo que está en Firestore) y `draft` (lo que se ve), `isLoading`,
`isSaving`, `error`, `dialog`, y los derivados `hasChanges = draft != saved` e `isSaveEnabled`.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `RestrictionToggled`, `CuisineToggled` | Agrega o quita del conjunto (`Set.toggle`) |
| `SpiceSelected`, `BudgetSelected` | Elige uno; tocar el elegido lo quita |
| `StoreSelected` | Elige la tienda; siempre queda una |
| `SaveClicked` → `SaveConfirmed` | "¿Desea guardar?" y `SaveFoodPreferencesUseCase` |
| `BackClicked` | Descarta el borrador sin preguntar |

**Dependencias.** `GetFoodPreferencesUseCase` y `SaveFoodPreferencesUseCase` (guarda el mapa `preferences` de
`users/{uid}`, validado por `firestore.rules`).

**Compose en esta pantalla.** [`FlowRow`](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreen.kt#L191): los chips pasan a la línea siguiente
cuando no caben, sin calcular columnas.

**Notas técnicas.** El patrón **borrador / guardado** hace que "Guardar" se habilite solo si algo cambió, comparando
dos `data class` con `!=`, sin llevar banderas a mano.

Archivo: [FoodPreferencesViewModel.kt, líneas 67–73](../app/src/main/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesViewModel.kt#L67-L73)

```kotlin
is FoodPreferencesEvent.SpiceSelected -> edit { preferences ->
    // Solo uno: tocar el elegido lo quita (CA-PER-03-02).
    preferences.copy(spice = if (preferences.spice == event.level) null else event.level)
}
is FoodPreferencesEvent.BudgetSelected -> edit { preferences ->
    preferences.copy(budget = if (preferences.budget == event.budget) null else event.budget)
}
```

---

# Recetas

## 15. Descubrir recetas

| | |
|---|---|
| Archivos | [`RecipesDiscoverScreen.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt) · [`RecipesDiscoverContract.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverContract.kt) · [`RecipesDiscoverViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverViewModel.kt) |
| Ruta | `Route.Recipes` (pestaña) en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L51) |
| Spec | 009, HU-RIA-01; Figma `557:3028` |

**Qué hace.** Búsqueda, tarjeta "¿Qué cocinamos hoy?" (abre la IA), filtros "Para ti", "≤ 30 min" y "Económicas", y
las tarjetas con foto, tiempo, kcal y costo de lo que falta.

**Estado (`RecipesDiscoverUiState`).** `isLoading`, `hasError`, `query`, `filter` y `recipes: List<RecipeCardUi>` (ya
buscadas, filtradas y ordenadas).

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `QueryChanged`, `ClearQuery` | Cambian `query` |
| `FilterSelected` | Cambia `filter` |
| `RecipeClicked(id)` | Efecto `OpenRecipe(id)` |

**Dependencias.** `GetRecipesUseCase` (catálogo de `assets/`), `SearchRecipesUseCase`, `RankRecipesForMeUseCase`
(filtrado y orden), `ObserveInventoryUseCase`, `GetPreferredStoreUseCase` y `SavedStateHandle`.

**Compose en esta pantalla.**
- [`LazyVerticalGrid`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L87) de dos columnas. La búsqueda, la tarjeta de la IA y
  los filtros ocupan toda la fila con `span = { GridItemSpan(maxLineSpan) }`, así todo se desplaza junto.
- Las tarjetas usan `key = { it.id }`.

**Notas técnicas.**
- Igual que la búsqueda del inventario, el estado se calcula con `combine` de cinco flujos y `stateIn(WhileSubscribed)`.
- La tienda empieza en `Store.D1` y se lee del Perfil cada vez que se vuelve a la pantalla
  (`flow { emit(Store.D1); emit(getPreferredStore()) }`).
- "Ver recetas económicas" (desde la IA) deja el filtro en el `savedStateHandle` de esta pantalla. El ViewModel lo
  **observa** (no lo lee una sola vez), porque ya existe al volver, y lo borra al usarlo.

Archivo: [RecipesDiscoverViewModel.kt, líneas 62–65](../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverViewModel.kt#L62-L65)

```kotlin
savedStateHandle.getStateFlow<String?>(FILTER_KEY, null).filterNotNull().collect { value ->
    RecipeFilter.entries.firstOrNull { it.name == value }?.let { filter.value = it }
    savedStateHandle[FILTER_KEY] = null                      // se usa una vez
}
```

## 16. Detalle de receta

| | |
|---|---|
| Archivos | [`RecipeDetailScreen.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt) · [`RecipeDetailContract.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailContract.kt) · [`RecipeDetailViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailViewModel.kt) |
| Ruta | `Route.RecipeDetail(recipeId)` en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L124) |
| Spec | 009, HU-RIA-04; Figma `557:2856` |

**Qué hace.** Foto, tiempos (preparación, cocción, reposo), dificultad, selector de porciones, "Precios de referencia"
de la tienda e ingredientes agrupados, con la cantidad y el costo de lo que falta. Lo que alcanza se marca "En tu
inventario".

**Estado (`RecipeDetailUiState`).** `isLoading`, `hasError`, `recipeId`, `name`, `photo`, los tres tiempos,
`difficulty`, `servings`, `groups` (ingredientes ya escalados) y `store`.

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `DecreaseServings`, `IncreaseServings` | Cambia las porciones entre 1 y 12 y recalcula todo |
| `StartCooking` | Efecto `OpenCookingIntro(recipeId, servings)` |

**Dependencias.** `SavedStateHandle` (`recipeId`), `GetRecipeDetailUseCase`, `BuildRecipeIngredientsUseCase` (escala,
agrupa y calcula el costo), `ObserveInventoryUseCase` y `GetPreferredStoreUseCase`.

**Compose en esta pantalla.** `LazyColumn` con la foto (`RecipePhoto`), las tarjetas de tiempo y los grupos de
ingredientes; [`PortionButton`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L338) para "−" y "+".

**Notas técnicas.**
- Las porciones elegidas son un `servingsOverride: MutableStateFlow<Int?>`: `null` significa "las de la receta".
  Entran al `combine` junto con el detalle, el inventario y la tienda, así las cantidades y costos se recalculan solos.
- `checkNotNull(savedStateHandle.get(RECIPE_ID_KEY))`: si falta el argumento de la ruta, falla al instante con un
  mensaje claro, en lugar de mostrar una receta vacía.

Archivo: [RecipeDetailViewModel.kt, líneas 128–132](../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailViewModel.kt#L128-L132)

```kotlin
private fun changeServings(delta: Int) {
    val recipe = (detail.value as? DetailState.Ready)?.detail?.recipe ?: return
    val current = servingsOverride.value ?: recipe.servings
    servingsOverride.update { (current + delta).coerceIn(MinServings, MaxServings) }   // 1 a 12
}
```

## 17. Sugerencia con IA y Ayuda

| | |
|---|---|
| Archivos | [`AiFlowScreen.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt) · [`AiHelpScreen.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiHelpScreen.kt) · [`AiPlanContract.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiPlanContract.kt) · [`AiPlanViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiPlanViewModel.kt) · [`AiReasonText.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiReasonText.kt) |
| Rutas | `Route.AiPlan` en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L74) y `Route.AiHelp` en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L101) |
| Spec | 009, HU-RIA-02 y CA-RIA-03-01; Figma `633:2948`, `666:3546`, `557:2903`, `557:2515` y `557:2476` |

**Qué hace.** Una sola ruta con cuatro pasos: **Contexto** (inventario detectado, preferencias, presupuesto, porciones,
tiempo y picante) → **Analizando** → **Tu plan de 3 días** o **Sin coincidencias**. "Ayuda" explica cómo decide la IA.

**El caso especial.** `AiHelpScreen` **no tiene ViewModel** (como los documentos legales): solo recibe `onBack` y
`onContactSupport`. `AiPlanViewModel` lo usa únicamente `AiFlowScreen`; Ayuda se abre con el efecto `OpenHelp`.

**Estado (`AiUiState`).**

| Campo | Para qué |
|---|---|
| `step: AiStep` | `Context`, `Loading`, `Plan` o `NoPlan` |
| `detected`, `expiringIds`, `firstExpiring`, `firstExpiringDays` | Inventario emparejado y el aviso "⚠ El pollo vence mañana" |
| `restrictions`, `store` | Del Perfil |
| `budgetCop`, `servings`, `maxMinutes`, `noSpicy`, `sheet` | Lo que elige la persona |
| `plan`, `reasons`, `suggestions` | El resultado del agente |

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `BudgetChanged(cop)` | Redondea a $1.000, entre $2.000 y $100.000 |
| `ServingsClicked`/`Picked`, `TimeClicked`/`Picked`, `SpicyToggled` | Hojas de opciones y picante |
| `GenerateClicked` | `generate()`: corre el agente |
| `CancelClicked` | Cancela el cálculo y vuelve al contexto |
| `AdjustClicked` | Vuelve al contexto con los mismos datos |
| `RecipeClicked`, `EconomicRecipesClicked`, `HelpClicked` | Efectos `OpenRecipe`, `OpenEconomicRecipes` y `OpenHelp` |

**Dependencias.** `GetAiContextUseCase`, `GeneratePlanUseCase` (el agente, en `Dispatchers.Default`) y `Clock`.

**Compose en esta pantalla.**
- Un `when (state.step)` elige el contenido; el título y la acción de la barra también dependen del paso.
- [`BackHandler(enabled = …Loading)`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L93): "atrás"
  cancela solo mientras analiza.
- [`Slider`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L229) para el presupuesto.
- Las razones se convierten en texto **solo** en [`AiReasonText.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiReasonText.kt).

**Notas técnicas.**
- Una sola ruta para los cuatro pasos: el plan **no** viaja entre pantallas, y "Ajustar" conserva todo lo elegido.
- `async` + `delay(1_500)`: el agente y la espera mínima corren **a la vez**. "Analizando" dura como mínimo 1,5 s
  (desviación aprobada) aunque el agente tarde 40 ms, y nunca la suma de ambos.
- `generation?.cancel()` cancela la corrutina, y con ella el cálculo, al tocar "Cancelar".
- "Ver recetas económicas" escribe el filtro en el `savedStateHandle` de `Route.Recipes`
  ([`getBackStackEntry<Route.Recipes>()`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L87)) y vuelve a esa pantalla
  (ficha 15).

Archivo: [AiPlanViewModel.kt, líneas 107–119](../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiPlanViewModel.kt#L107-L119)

```kotlin
generation = viewModelScope.launch {
    val outcome = coroutineScope {
        val result = async {                                   // el agente empieza ya…
            val started = System.nanoTime()
            generatePlan(loaded, state.budgetCop, state.servings, state.maxMinutes, state.noSpicy).also {
                Log.i(TAG, "Plan calculado en ${(System.nanoTime() - started) / 1_000_000} ms")
            }
        }
        delay(MIN_LOADING_MILLIS)                              // …mientras corre la espera mínima de 1,5 s
        result.await()
    }
    // … pasa a AiStep.Plan o AiStep.NoPlan …
}
```

## 18. Cocina guiada

| | |
|---|---|
| Archivos | [`CookingScreens.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt) (`CookingIntroScreen` y `CookingScreen`) · [`CookingContract.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingContract.kt) · [`CookingViewModel.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingViewModel.kt) · [`TimerAlarm.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/TimerAlarm.kt) |
| Rutas | `Route.CookingIntro` en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L146) y `Route.Cooking` en [`RecipesNavGraph.kt`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L164) |
| Spec | 009, HU-RIA-04; Figma `557:2819` y `557:2781` |

**El caso especial.** El archivo se llama `CookingScreens.kt` (en plural) porque tiene **dos** pantallas: "Antes de
empezar" (`CookingIntroScreen`) y la guía paso a paso (`CookingScreen`). Las dos usan `CookingViewModel`, pero **cada
ruta crea su propia instancia** con `hiltViewModel()`, porque el ViewModel vive en la entrada de la pila. Ambas leen
`recipeId` y `servings` de los argumentos de su ruta.

**Estado (`CookingUiState`).**

| Campo | Para qué |
|---|---|
| `name`, `photo`, `beforeStarting`, `steps`, `servings` | La receta |
| `currentStepIndex` | Paso actual |
| `timerSecondsRemaining`, `isTimerRunning`, `isAlarmRinging` | Temporizador y alarma |
| `dialog: CookingDialog?` | `ConfirmFinish`, `Saving`, `Saved(onPhone)` o `Failed(error)` |
| `currentStep`, `isFirstStep`, `isLastStep`, `progressFraction`, `hasTimer` | **Derivados** |

**Eventos → ViewModel.**

| Evento | Qué hace |
|---|---|
| `StartGuideClicked` | Efecto `OpenGuide` |
| `NextStepClicked`, `PreviousStepClicked` | Cambian de paso, detienen el temporizador y la alarma, y cargan el tiempo del paso nuevo |
| `TimerToggled` | Según el caso: detiene la alarma, pausa o inicia |
| `ResetTimerClicked` | Vuelve el temporizador al tiempo del paso |
| `ExitClicked` | Pausa, detiene la alarma y efecto `ExitToDetail` |
| `FinishClicked` → `ConfirmFinishClicked` | "¿Cocinaste esta receta?" y `FinishCookingUseCase` (descuenta el inventario y guarda el registro) |

**Efectos.** `OpenGuide`, `ExitToDetail` (vuelve al detalle con `popBackStack<Route.RecipeDetail>`),
`PlayTimerFinishedAlert` y `StopTimerAlert`.

**Dependencias.** `SavedStateHandle`, `GetRecipeDetailUseCase` y `FinishCookingUseCase`.

**Compose en esta pantalla.**
- [`TimerCard`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L278): la tarjeta se toca para iniciar o pausar.
- En la ruta: [`remember { TimerAlarm(…) }`](../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L174)
  y `DisposableEffect(alarm) { onDispose { alarm.stop() } }`: la alarma se corta sí o sí al salir de la guía.

**Notas técnicas.**
- El sonido **no** está en el ViewModel (no conoce Android): el ViewModel emite `PlayTimerFinishedAlert` y la ruta
  llama a `TimerAlarm`, que reproduce `res/raw/timer_alarm.ogg` con volumen de alarma.
- El temporizador es una corrutina con `delay(1000L)` que se cancela al pausar, al cambiar de paso, al salir y en
  `onCleared()`.
- La alarma se calla sola a los `ALARM_MAX_MILLIS` si nadie la detiene.
- Sin conexión, el cierre muestra "Guardado en tu teléfono" (`Saved(onPhone = true)`).

Archivo: [CookingViewModel.kt, líneas 130–147](../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingViewModel.kt#L130-L147)

```kotlin
private fun startTimer() {
    if (_uiState.value.isTimerRunning || _uiState.value.timerSecondsRemaining <= 0) return
    _uiState.update { it.copy(isTimerRunning = true) }
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
        while (_uiState.value.timerSecondsRemaining > 0 && _uiState.value.isTimerRunning) {
            delay(1000L)
            _uiState.update { current ->
                val newTime = current.timerSecondsRemaining - 1
                if (newTime <= 0) current.copy(timerSecondsRemaining = 0, isTimerRunning = false)
                else current.copy(timerSecondsRemaining = newTime)
            }
        }
        if (_uiState.value.timerSecondsRemaining == 0) startAlarm()   // efecto PlayTimerFinishedAlert
    }
}
```

---

# Comparación de las pantallas

| Pantalla | Estado en vivo (`Flow`) | Cómo arma el estado | Diálogos | Sin conexión | Prueba instrumentada |
|---|---|---|---|---|---|
| Splash | — | `StateFlow` simple | — | — | (en `AppNavigationTest`) |
| Iniciar sesión | — | `MutableStateFlow` + `update` | Reconexión | Diálogo de reconexión | [`LoginScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/auth/ui/login/LoginScreenTest.kt) |
| Registro | — | `MutableStateFlow` + `update` | Reconexión | Diálogo de reconexión | [`RegisterScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/auth/ui/register/RegisterScreenTest.kt) |
| Recuperar contraseña | — | `MutableStateFlow` + `update` | — | Error de red | [`PasswordRecoveryScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/auth/ui/recovery/PasswordRecoveryScreenTest.kt) |
| Documentos legales | — | Sin ViewModel | — | No necesita red | [`LegalDocumentScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreenTest.kt) |
| Mi Inventario | Firestore | `MutableStateFlow` + `collect` | — | Caché y `isPendingSync` | [`InventoryScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreenTest.kt) |
| Buscar | Firestore | `combine` + `stateIn(WhileSubscribed)` | Aviso de voz | Caché | [`InventorySearchScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreenTest.kt) |
| Editor de producto | — | `MutableStateFlow` + `update` | 7 hojas y diálogos | "Guardado en tu teléfono" | [`ProductEditorScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreenTest.kt) |
| Cámara | — | `remember` en la Route | Permiso negado | No necesita red | [`ProductCameraScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreenTest.kt) |
| Perfil | Perfil, inventario, ahorro y tema | 4 `collect` en paralelo | Cerrar sesión, tema | Avisa de cambios sin subir | [`ProfileScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/profile/ui/ProfileScreenTest.kt) |
| Editar información | — (solo `first()`) | `MutableStateFlow` + `update` | 5 diálogos | Error de red | [`EditProfileScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/profile/ui/edit/EditProfileScreenTest.kt) |
| Notificaciones | DataStore | `MutableStateFlow` + `collect` | Permiso negado | No necesita red | [`NotificationSettingsScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/profile/ui/notifications/NotificationSettingsScreenTest.kt) |
| Cambiar contraseña | — | `MutableStateFlow` + `update` | "Guardado" | Error de red | [`ChangePasswordScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/profile/ui/password/ChangePasswordScreenTest.kt) |
| Preferencias | — | Borrador y guardado | "¿Guardar?", "Guardado" | Error de red | [`FoodPreferencesScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/profile/ui/preferences/FoodPreferencesScreenTest.kt) |
| Descubrir | Inventario | `combine` + `stateIn(WhileSubscribed)` | — | Catálogo local | [`RecipesDiscoverScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreenTest.kt) |
| Detalle | Inventario | `combine` + `stateIn(WhileSubscribed)` | — | Catálogo local | [`RecipeDetailScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreenTest.kt) |
| IA | — | `MutableStateFlow` + `update` | Hojas de opciones | El agente corre en el teléfono | [`AiFlowScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreenTest.kt) |
| Cocina guiada | — | `MutableStateFlow` + temporizador | Cierre (4 estados) | "Guardado en tu teléfono" | [`CookingScreenTest.kt`](../app/src/androidTest/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreenTest.kt) |

**Dos formas de armar el estado.**
- **`MutableStateFlow` + `update`:** cuando el estado cambia sobre todo por lo que hace la persona (formularios).
- **`combine` + `stateIn(WhileSubscribed)`:** cuando el estado se **calcula** a partir de varias fuentes en vivo
  (inventario, búsqueda, filtros). Además deja de escuchar Firestore 5 s después de salir de la pantalla.
