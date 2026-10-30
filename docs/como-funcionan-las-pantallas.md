# Las pantallas de SmartFood, unidad por unidad

Explicación del código de las pantallas (`*Screen.kt`) y de lo que las rodea, ordenada según las unidades del curso
[Aspectos básicos de Android con Compose](https://developer.android.com/courses/android-basics-compose/course?hl=es-419).

Cada sección tiene tres partes:

- **Concepto:** qué es, en dos o tres frases.
- **Código real:** copiado de la app y explicado línea por línea. `// …` marca las partes que se omitieron.
- **Decisión:** por qué se hizo así en SmartFood.

| Unidad del curso | Qué se ve aquí | Archivos |
|---|---|---|
| 1. Tu primera app para Android | `setContent`, composables, `Text`, `Column`, `Modifier`, recursos, `@Preview` | `MainActivity.kt`, `LoginScreen.kt` |
| 2. Compila la IU de una app | Kotlin (data class, sealed, `when`, lambdas), estado, recomposición, elevación de estado, campos de texto, pruebas | `LoginContract.kt`, `LoginScreen.kt`, `LoginScreenTest.kt` |
| 3. Listas y Material Design | Genéricos y extensiones, `LazyColumn`, tema de Material 3, animaciones | `InventoryScreen.kt`, `InventoryItemRow.kt`, `Theme.kt`, `InventorySkeleton.kt` |
| 4. Navegación y arquitectura | `ViewModel`, `StateFlow`, UDF, Navigation Compose, tamaños de pantalla | `LoginViewModel.kt`, `AuthNavGraph.kt`, `AppNavHost.kt`, `ProductEditorScreen.kt` |
| 5. Conectarse a Internet | Corrutinas, repositorio, inyección de dependencias, imágenes de Internet | `ProductEditorRoute.kt`, `InventoryItemRow.kt` |
| 6. Persistencia de datos | DataStore | `ThemeViewModel.kt`, `MainActivity.kt` |
| 7. WorkManager | Trabajo en segundo plano | `SmartfoodApp.kt` |
| 8. Views y Compose | Una View de Android dentro de Compose | `ProductCameraRoute.kt` |

---

# Antes de empezar: cómo funciona Compose por dentro

Estas cuatro ideas explican casi todas las notas técnicas del documento.

| Idea | En pocas palabras |
|---|---|
| **Tres fases por cuadro** | *Composición* (qué hay en pantalla), *diseño* (medir y ubicar) y *dibujo* (pintar). Leer un estado en una fase más tardía evita repetir las anteriores |
| **Estado observable** | Compose anota qué composable leyó cada estado. Cuando el estado cambia, solo se vuelve a ejecutar lo que lo leyó (*recomposición*) |
| **Saltar lo que no cambió** | Si los parámetros de un composable son iguales a los de la vez anterior, Compose no lo ejecuta (*skipping*). Por eso el estado es inmutable y se cambia con `copy()` |
| **Efectos con ciclo de vida** | Lo que sale de la interfaz (red, navegación) va en `LaunchedEffect` o en el ViewModel, nunca directo en el composable, porque un composable se puede ejecutar muchas veces |

**Debug frente a release.** El APK de debug no pasa por R8 e incluye información para depurar, así que es más lento. El de release (`isMinifyEnabled = true` e `isShrinkResources = true` en `app/build.gradle.kts`) es más pequeño y rápido. Las animaciones y la fluidez se miden **siempre** con el APK release en un teléfono real, como dice `AGENTS.md`.

---

# Unidad 1: Tu primera app para Android

## 1.1 La actividad y `setContent`

**Concepto.** Una app de Compose tiene una sola `Activity`. En `onCreate`, `setContent { }` reemplaza el XML: todo lo
que va dentro son composables.

```kotlin
// MainActivity.kt
@AndroidEntryPoint                                                     // 1
class MainActivity : ComponentActivity() {                             // 2

    private val themeViewModel: ThemeViewModel by viewModels()         // 3

    override fun onCreate(savedInstanceState: Bundle?) {               // 4
        super.onCreate(savedInstanceState)                             // 5
        enableEdgeToEdge(                                              // 6
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {                                                   // 7
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()  // 8
            SmartfoodTheme(themeMode = themeMode) {                    // 9
                AppNavHost()                                           // 10
            }
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Permite que Hilt inyecte dependencias en esta actividad (unidad 5) |
| 2 | `ComponentActivity` es la actividad base de Compose. El curso usa la misma |
| 3 | `by viewModels()` crea el ViewModel o recupera el que ya existe si se gira la pantalla (unidad 4) |
| 4–5 | El ciclo de vida normal de una actividad |
| 6 | **Borde a borde:** la app dibuja detrás de la barra de estado y de navegación. `light(...)` con fondo transparente deja los íconos de esas barras en oscuro |
| 7 | Desde aquí todo es Compose |
| 8 | Lee el modo de color guardado (claro u oscuro) como estado de Compose (unidades 4 y 6) |
| 9 | Aplica el tema de la app a todo lo que va adentro (unidad 3) |
| 10 | La navegación: decide qué pantalla se ve (unidad 4) |

**Decisión.** El tema envuelve a `AppNavHost` una sola vez. Así ninguna pantalla tiene que aplicarlo, y cambiar el
modo en el Perfil cambia toda la app al instante.

> **Notas técnicas**
>
> - **Concepto profundo:** `setContent` crea un `ComposeView` y lo pone como contenido de la ventana. Desde ahí Compose mantiene un árbol de nodos propio, sin XML ni `findViewById`. Una sola actividad para toda la app significa un solo árbol y una sola ventana: cambiar de pantalla es cambiar qué composables están en ese árbol, no abrir otra actividad.
> - **Ciclo de vida:** Al girar el teléfono o cambiar el idioma, Android **destruye y vuelve a crear** la actividad. El ViewModel sobrevive porque vive en el `ViewModelStore`, que no se destruye en ese cambio. Por eso el estado va en el ViewModel y no en la actividad.
> - **Hardware:** Con `targetSdk` 35 o superior (la app usa 37), Android 15 obliga a dibujar borde a borde. `enableEdgeToEdge` lo hace igual en todas las versiones desde `minSdk` 24, y cada pantalla aplica los *insets* para no quedar debajo de la cámara frontal, la barra de estado o la barra de gestos.
> - **Optimización:** `collectAsStateWithLifecycle` deja de escuchar el flujo cuando la app pasa a segundo plano. Con la app minimizada, el modo de color no hace trabajo inútil.
> - **Decisión técnica:** `@AndroidEntryPoint` hace que Hilt genere el código de inyección **al compilar**. No usa reflexión al ejecutar, lo que es más rápido al arrancar y no se rompe con R8 en el build release.

## 1.2 Composables, `Text`, `Column`, `Spacer` y `Modifier`

**Concepto.** Un composable es una función con `@Composable` que **describe** la interfaz. No devuelve nada: lo que
"emite" se dibuja. `Column` apila hacia abajo, `Row` de lado y `Box` encima. `Modifier` cambia tamaño, margen,
fondo y clics.

```kotlin
// LoginScreen.kt
@Composable                                                            // 1
private fun LoginWelcomeHeader(modifier: Modifier = Modifier) {        // 2
    Column(modifier = modifier) {                                      // 3
        Text(
            text = stringResource(id = R.string.login_welcome_title),  // 4
            style = LocalFormMetrics.current.titleStyle,               // 5
            color = MaterialTheme.colorScheme.primary                  // 6
        )
        Spacer(Modifier.height(3.dp))                                  // 7
        Text(
            text = stringResource(id = R.string.login_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,                // 8
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Marca la función como composable: solo se puede llamar desde otro composable |
| 2 | `private`: solo la usa `LoginScreen`. `modifier: Modifier = Modifier` deja que quien la llame la ajuste; el valor por defecto es "sin cambios" |
| 3 | Apila los dos textos. El `modifier` recibido se aplica al contenedor exterior, como recomienda Compose |
| 4 | `stringResource` lee `res/values/strings.xml`. El texto nunca se escribe en el código |
| 5 | El estilo del título sale de `LocalFormMetrics`: cambia de tamaño según el alto del teléfono (unidad 4) |
| 6 | El color sale del tema (`primary`), nunca de un `Color(0xFF…)` fijo |
| 7 | `Spacer` deja espacio vacío. `3.dp` es una medida del Figma |
| 8 | Tipografía del tema (Poppins), definida en `Type.kt` |

**Decisión.** Los textos van en `strings.xml` y los colores en el tema por dos razones: la app se puede traducir y
el modo oscuro funciona sin tocar cada pantalla.

> **Notas técnicas**
>
> - **Concepto profundo:** Compose dibuja cada cuadro en **tres fases**: *composición* (qué elementos hay), *diseño* (medir y ubicar) y *dibujo* (pintar). Un composable es la descripción que alimenta la primera fase. Compose guarda el resultado en una estructura interna (la *slot table*) y, cuando algo cambia, vuelve a ejecutar solo los composables afectados.
> - **El orden del `Modifier` importa:** Cada modificador envuelve al anterior. `Modifier.padding(16.dp).background(color)` pinta el fondo **dentro** del margen; `Modifier.background(color).padding(16.dp)` lo pinta también debajo del margen. Por eso en la app el `modifier` recibido va primero y después los propios.
> - **Hardware: `dp`:** Un `dp` es independiente de la densidad: `px = dp × (dpi / 160)`. `3.dp` son 3 px en una pantalla de 160 dpi y unos 8 px en una de 420 dpi. Así el diseño mide lo mismo **físicamente** en un teléfono barato y en uno de gama alta.
> - **Optimización:** La fuente Poppins va empaquetada en `res/font` y no se descarga: el texto se mide con la fuente correcta desde el primer cuadro, sin saltos de tamaño ni dependencia de la red.
> - **Decisión técnica:** Los colores se leen por **rol** (`colorScheme.primary`). Cambiar de modo claro a oscuro solo cambia el esquema que provee el tema, y los composables que leen ese rol se recomponen solos.

## 1.3 `@Preview`

```kotlin
@Preview(showBackground = true, showSystemUi = true)                   // 1
@Composable
private fun LoginScreenPreview() {
    SmartfoodTheme {                                                   // 2
        LoginScreen(
            state = LoginUiState(),                                    // 3
            onEvent = {},                                              // 4
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Android Studio dibuja la pantalla sin ejecutar la app. `showSystemUi` agrega el marco del teléfono |
| 2 | Envuelve en el tema para que los colores y fuentes sean los reales |
| 3 | Un estado inicial vacío, escrito a mano |
| 4 | Lambdas vacías: en la vista previa no se navega |

**Decisión.** La vista previa solo funciona porque `LoginScreen` **no tiene ViewModel**: recibe todo por parámetros.
Es la razón de separar Screen y ViewModel (unidad 4).

> **Notas técnicas**
>
> - **Concepto profundo:** La vista previa no corre en un teléfono: Android Studio la dibuja en la JVM con una versión simulada del sistema (*layoutlib*). Por eso no puede usar Firebase, la cámara ni Hilt, y solo funciona con pantallas que reciben todo por parámetros.
> - **Optimización:** Las funciones `@Preview` son `private` y nadie las llama. En el build release, **R8 las elimina**: no ocupan espacio en el APK.

---

# Unidad 2: Compila la IU de una app

## 2.1 Kotlin: `data class`, `sealed interface`, valores por defecto y propiedades calculadas

```kotlin
// LoginContract.kt
@Immutable                                                             // 1
data class LoginUiState(                                               // 2
    val email: String = "",                                            // 3
    val password: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val error: AuthError? = null                                       // 4
) {
    val isOffline: Boolean get() = error == AuthError.Network          // 5

    val passwordHasError: Boolean
        get() = error.marksEmpty(AuthField.PASSWORD) || error == AuthError.InvalidCredentials  // 6
}

sealed interface LoginEvent {                                          // 7
    data class EmailChanged(val email: String) : LoginEvent            // 8
    data class PasswordChanged(val password: String) : LoginEvent
    data class RememberMeChanged(val isChecked: Boolean) : LoginEvent
    data object LoginClicked : LoginEvent                              // 9
    data object RetryConnectionClicked : LoginEvent
}

sealed interface LoginEffect {                                         // 10
    data object NavigateToHome : LoginEffect
}
```

| Línea | Qué hace |
|---|---|
| 1 | Le promete a Compose que el objeto no cambia después de creado, así puede saltarse redibujos innecesarios |
| 2 | `data class` genera `equals`, `toString` y `copy()`. Con `copy(email = "a")` se crea un estado nuevo cambiando un solo campo |
| 3 | Todos los campos tienen valor por defecto: `LoginUiState()` es el estado inicial |
| 4 | `AuthError?` puede ser `null` (sin error). Es la seguridad de nulos de Kotlin |
| 5 | Propiedad **calculada**: no se guarda, se deriva del error en cada lectura |
| 6 | El campo de contraseña se pone en rojo **solo** según el error. Así es imposible que el error diga una cosa y el campo otra |
| 7 | `sealed`: los casos posibles están todos en este archivo, y un `when` sobre `LoginEvent` obliga a manejarlos todos |
| 8 | Un evento con datos: lo que escribió el usuario |
| 9 | `data object`: un evento sin datos, con una sola instancia |
| 10 | Los efectos ocurren **una vez**, como navegar (unidad 4) |

**Decisión.** El estado, los eventos y los efectos están en un archivo `XContract.kt` aparte. Así cualquier persona ve
de un vistazo qué muestra la pantalla y qué puede pasar en ella, sin leer el ViewModel.

> **Notas técnicas**
>
> - **Concepto profundo: estabilidad:** Compose puede **saltarse** (*skip*) un composable si sus parámetros no cambiaron. Para saberlo, el compilador clasifica cada tipo como *estable* o *inestable*. Una `data class` con solo `val` de tipos simples es estable. Una con `List<…>` se considera inestable, porque `List` es una interfaz que podría ser mutable por dentro. `@Immutable` es una promesa al compilador de que la clase nunca cambia, y le permite tratarla como estable.
> - **Skipping fuerte:** Desde Kotlin 2.0.20 (la app usa 2.2.10), el compilador de Compose activa el *strong skipping* por defecto: también se saltan los composables con parámetros inestables si reciben **la misma instancia** (`===`), y los estables se comparan con `equals`. Por eso `copy()` es la forma correcta de cambiar el estado: crea una instancia nueva solo cuando algo cambió.
> - **Memoria:** Cada `copy()` crea un objeto pequeño. El recolector de basura de ART está pensado para muchos objetos de vida corta, así que crear un estado por cada letra escrita no se nota ni en teléfonos de gama baja.
> - **Decisión técnica:** Los campos en rojo son propiedades **calculadas** (`get()`), no guardadas. Calcular `passwordHasError` cuesta unas pocas comparaciones, mucho menos que el riesgo de que dos campos guardados se contradigan. No hace falta `derivedStateOf`: ese sirve cuando el estado cambia muchas más veces que el resultado (por ejemplo, la posición del scroll), y aquí no es el caso.
> - **`sealed` en tiempo de compilación:** El `when` exhaustivo sobre una `sealed interface` no cuesta nada al ejecutar: es una verificación del compilador. Si alguien agrega `LoginEvent.GoogleClicked` y olvida manejarlo, la app no compila.

## 2.2 Estado, recomposición y elevación de estado

**Concepto.**
- **Estado:** cualquier valor que, si cambia, cambia lo que se ve.
- **Recomposición:** cuando el estado cambia, Compose vuelve a ejecutar los composables que lo leen.
- **Elevación de estado** (*state hoisting*): el composable no guarda el estado. Lo recibe por parámetro (`value`) y
  avisa los cambios con una lambda (`onValueChange`).

```kotlin
// LoginScreen.kt
@Composable
private fun LoginFormFields(
    state: LoginUiState,                                               // 1
    onEvent: (LoginEvent) -> Unit,                                     // 2
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current                       // 3

    Column(modifier = modifier) {
        SmartFoodTextField(
            label = stringResource(id = R.string.login_email_label),
            contentType = EmailAccountContentType,                     // 4
            value = state.email,                                       // 5
            onValueChange = remember(onEvent) { { email -> onEvent(LoginEvent.EmailChanged(email)) } },  // 6
            placeholder = stringResource(id = R.string.login_email_placeholder),
            enabled = !state.isLoading,                                // 7
            isError = state.emailHasError,                             // 8
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,          // 9
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,                     // 10
                imeAction = ImeAction.Next                             // 11
            ),
        )
        // … campo de contraseña …
        SmartFoodPasswordField(
            // …
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {                                             // 12
                    focusManager.clearFocus()
                    onEvent(LoginEvent.LoginClicked)
                }
            ),
        )
        AuthErrorMessage(
            message = state.error.inlineError?.let { stringResource(id = it.messageRes()) },  // 13
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | El estado **llega** por parámetro: este composable no tiene estado propio |
| 2 | Tipo función: recibe un `LoginEvent` y no devuelve nada (`Unit`). Así avisa hacia arriba |
| 3 | `CompositionLocal` que da el control del foco del teclado |
| 4 | Le dice al autocompletado del teléfono que es un correo de cuenta |
| 5 | El campo **muestra** lo que dice el estado. Si el estado no cambia, el campo no cambia |
| 6 | Cada letra genera `EmailChanged`. `remember(onEvent)` guarda la misma lambda mientras `onEvent` no cambie, así no se crea una nueva en cada recomposición |
| 7 | Mientras inicia sesión, el campo se bloquea |
| 8 | Rojo si el estado dice que hay error (sección 2.1, línea 6) |
| 9–10 | Teclado de correo, sin mayúsculas ni autocorrector |
| 11 | El botón del teclado dice "Siguiente" y pasa al campo de contraseña |
| 12 | En la contraseña, el botón "Listo" cierra el teclado e inicia sesión |
| 13 | `?.let`: si hay un error para mostrar debajo de los campos, se convierte en texto; si no, `null` y no se muestra nada |

**El ciclo completo cuando el usuario escribe "a":**
1. `SmartFoodTextField` llama `onValueChange("a")`.
2. Se ejecuta `onEvent(LoginEvent.EmailChanged("a"))`.
3. El ViewModel hace `_uiState.update { it.copy(email = "a", error = null) }` (unidad 4).
4. Llega un `state` nuevo y Compose **recompone** solo lo que leyó `state.email`.
5. El campo muestra "a".

**Decisión.** El estado vive en el ViewModel, no en un `remember { mutableStateOf("") }` dentro de la pantalla. Así
sobrevive al giro de pantalla y la lógica se puede probar sin la interfaz.

> **Notas técnicas**
>
> - **Concepto profundo: el sistema de *snapshots*:** El estado de Compose (`mutableStateOf`, y un `StateFlow` convertido con `collectAsStateWithLifecycle`) se apoya en un sistema de *snapshots*. Cuando un composable **lee** un estado durante la composición, Compose anota esa lectura. Cuando ese estado cambia, invalida solo el **ámbito de recomposición** más cercano que lo leyó (normalmente, el composable o la lambda que lo contiene), no toda la pantalla.
> - **Qué se recompone al escribir una letra:** Cambia `state.email`, así que se recomponen los composables que reciben un `state` distinto. `LoginWelcomeHeader` no recibe `state`, así que se salta. Gracias a eso, escribir en el formulario no redibuja el encabezado.
> - **Campo de texto con estado en el ViewModel:** La documentación de Compose advierte que si el texto se actualiza de forma **asíncrona** (por ejemplo, después de un `delay` o de ir a la red), el cursor puede saltar. Aquí `MutableStateFlow.update` es síncrono y ocurre en el hilo principal, en el mismo cuadro, así que no pasa.
> - **Optimización: `remember(onEvent) { { … } }`:** Era útil antes del *strong skipping*, cuando una lambda nueva en cada recomposición impedía saltarse el componente que la recibía. Con Kotlin 2.0.20 o superior, el compilador **ya recuerda las lambdas automáticamente**, así que hoy es redundante. No hace daño, pero en código nuevo no hace falta.
> - **Hardware: el teclado:** `KeyboardOptions` es una **sugerencia** al teclado (IME). Gboard, el teclado de Samsung o SwiftKey pueden ignorar algunas opciones, como el autocorrector. La validación real del correo nunca depende del teclado: está en el caso de uso.

## 2.3 Cuándo **sí** se usa `remember { mutableStateOf() }`

Para estado que **solo importa a la interfaz** y no a la lógica:

```kotlin
// InventoryNavGraph.kt (destino del editor)
var leaveToList by remember { mutableStateOf(false) }                  // 1
```

| Parte | Qué hace |
|---|---|
| `mutableStateOf(false)` | Un valor observable: si cambia, recompone a quien lo lee |
| `remember { }` | Lo guarda entre recomposiciones; sin `remember` volvería a `false` cada vez |
| `var … by` | Delegado: se usa `leaveToList = true` en vez de `leaveToList.value = true` |

Aquí solo recuerda si hay que volver a la lista al salir del editor. Es una decisión de navegación, no de negocio.

> **Notas técnicas**
>
> - **Concepto profundo: memoización posicional:** `remember` guarda el valor en la *slot table*, en la **posición** donde se llamó. Por eso, si se llama dentro de un `if` y la condición cambia, el valor se pierde: esa posición se descarta.
> - **`remember` frente a `rememberSaveable`:** `remember` se pierde al girar el teléfono. `rememberSaveable` guarda el valor en el `Bundle` de la actividad y sobrevive al giro **y** a que Android cierre la app en segundo plano. La app usa `rememberSaveable` para mostrar u ocultar la contraseña (`Inputs.kt`, línea 284): si giras el teléfono, la contraseña sigue visible u oculta como la dejaste.
> - **Decisión técnica:** `leaveToList` usa `remember` porque solo vive durante un toque. Si se perdiera en un giro, lo peor que pasa es volver a la pantalla anterior en lugar de a la lista.

## 2.4 Un botón con estado de carga

```kotlin
// core/designsystem/components/Buttons.kt
@Composable
fun SmartFoodPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,                                        // 1
    loadingText: String = text,
    // …
) {
    // …
    val displayText = if (isLoading) {                                 // 2
        val infiniteTransition = rememberInfiniteTransition(label = "dots")
        val dotCount by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 4f,
            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "dots_count"
        )
        "$loadingText${".".repeat(dotCount.toInt() % 4)}"              // 3
    } else {
        text
    }
    // …
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,                           // 4
            // …
        )
}
```

| Línea | Qué hace |
|---|---|
| 1 | Parámetros con valor por defecto: la mayoría de las pantallas solo pasan `text` y `onClick` |
| 2 | `if` como **expresión**: devuelve el texto que se muestra |
| 3 | Plantilla de texto: "Iniciando sesión", luego ".", "..", "..." en bucle (animación, unidad 3) |
| 4 | Mientras carga no se puede tocar, para no enviar el formulario dos veces |

**Decisión.** La animación se crea **solo** dentro del `if (isLoading)`. Si estuviera fuera, el botón se redibujaría
60 veces por segundo aunque no estuviera cargando.

> **Notas técnicas**
>
> - **Concepto profundo:** Una animación infinita pide un valor nuevo **en cada cuadro** que entrega la pantalla (sincronizado con el *vsync* a través de `Choreographer`). Como el texto lee `dotCount` durante la composición, el botón se recompone en cada cuadro mientras carga.
> - **Hardware: presupuesto por cuadro:** A 60 Hz hay 16,7 ms para cada cuadro; a 90 Hz, 11,1 ms; a 120 Hz, 8,3 ms. Recomponer un botón cabe de sobra, pero muchas animaciones a la vez en un teléfono de gama baja no. Por eso la animación existe **solo** mientras `isLoading` es `true`.
> - **Doble protección:** El botón se desactiva mientras carga (`enabled && !isLoading`) **y** el ViewModel ignora un segundo envío (`if (state.isLoading) return`). En un teléfono lento, dos toques rápidos pueden llegar antes de que la pantalla se redibuje; la segunda barrera evita dos inicios de sesión.
> - **Sombra:** `figmaShadow` dibuja la sombra en la **fase de dibujo**. No afecta la medición ni la composición, solo el pintado.

## 2.5 Pruebas automatizadas de la pantalla

```kotlin
// androidTest/.../LoginScreenTest.kt
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()                          // 1

    private fun mostrar(state: LoginUiState = LoginUiState(), onEvent: (LoginEvent) -> Unit = {}) {
        composeTestRule.setContent {                                   // 2
            SmartfoodTheme {
                LoginScreen(state = state, onEvent = onEvent, onNavigateToRegister = {}, onNavigateToForgotPassword = {})
            }
        }
    }

    @Test
    fun ca0203_credencialesIncorrectas_muestraMensaje() {              // 3
        mostrar(LoginUiState(error = AuthError.InvalidCredentials))    // 4
        composeTestRule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()  // 5
    }

    @Test
    fun pantallaLogin_cuandoSeHaceClicEnIniciarSesion_emiteEventoLoginClicked() {
        var lastEvent: LoginEvent? = null
        mostrar(onEvent = { lastEvent = it })                          // 6
        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()  // 7
        assertEquals(LoginEvent.LoginClicked, lastEvent)               // 8
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Regla de JUnit que monta composables en un emulador o teléfono |
| 2 | Dibuja la pantalla real con el tema |
| 3 | El nombre cita el criterio de aceptación `CA-02-03` |
| 4 | Un estado **inventado**: no hace falta Firebase ni ViewModel |
| 5 | Busca el texto en pantalla y comprueba que se ve |
| 6 | En lugar del ViewModel, una lambda que guarda el último evento |
| 7 | Toca el botón |
| 8 | Comprueba que salió el evento correcto |

**Decisión.** Las pantallas se prueban **sin estado**, con estados escritos a mano. La lógica se prueba aparte en el
ViewModel, con pruebas unitarias que corren en el computador.

> **Notas técnicas**
>
> - **Concepto profundo: el árbol de semántica:** `onNodeWithText` no busca píxeles. Busca en el **árbol de semántica**, el mismo que usa TalkBack. Cuando una prueba no encuentra un botón, muchas veces significa que TalkBack tampoco lo anuncia bien: accesibilidad y pruebas usan la misma información.
> - **Sincronización:** La regla de Compose espera a que no haya recomposiciones pendientes antes de cada verificación, así que la prueba no necesita `Thread.sleep`. Con animaciones o esperas, las pruebas avanzan el reloj con `mainClock`, como pide `AGENTS.md` para el cambio de perfil de `FormMetrics`.
> - **Hardware:** Las pruebas instrumentadas corren en un emulador o teléfono real (ART y GPU reales). Las unitarias corren en la JVM del computador, sin dibujar nada; por eso un componente que se cae al dibujarse pasa las unitarias (Issue #60) y hay que correr las instrumentadas de las pantallas que se tocan.

---

# Unidad 3: Cómo mostrar listas y usar Material Design

## 3.1 Kotlin: genéricos, extensiones y funciones de orden superior

```kotlin
// core/navigation/ObserveEffects.kt
@Composable
fun <T> ObserveEffects(effects: Flow<T>, onEffect: (T) -> Unit) {      // 1
    val lifecycleOwner = LocalLifecycleOwner.current                   // 2
    val currentOnEffect by rememberUpdatedState(onEffect)              // 3

    LaunchedEffect(effects, lifecycleOwner) {                          // 4
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {    // 5
            effects.collect { currentOnEffect(it) }                    // 6
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | **Genérico** `<T>`: sirve para `LoginEffect`, `InventoryEffect` o cualquier otro. Recibe una función (`onEffect`): es de **orden superior** |
| 2 | El dueño del ciclo de vida de la pantalla actual |
| 3 | Guarda siempre la versión más nueva de `onEffect` sin reiniciar el efecto |
| 4 | `LaunchedEffect` lanza una corrutina atada al composable: se cancela cuando la pantalla sale |
| 5 | Solo escucha mientras la pantalla está visible (`STARTED`). Si la app pasa a segundo plano, deja de escuchar |
| 6 | Cada efecto que llega se entrega una vez |

**Extensión** (agrega una propiedad a un `enum` sin modificarlo):

```kotlin
// InventoryItemRow.kt
val FoodCategory.titleRes: Int                                         // 1
    get() = when (this) {                                              // 2
        FoodCategory.FRUITS -> R.string.category_fruits
        FoodCategory.DAIRY -> R.string.category_dairy
        // …
        FoodCategory.OTHER -> R.string.category_other
    }
```

| Línea | Qué hace |
|---|---|
| 1 | Propiedad de extensión: ahora `item.category.titleRes` existe |
| 2 | `when` sin `else`: si se agrega una categoría nueva, el código no compila hasta darle su texto |

**Decisión.** `FoodCategory` vive en `domain`, que no conoce Android ni `R.string`. La extensión vive en `ui`, así el
dominio sigue siendo Kotlin puro.

> **Notas técnicas**
>
> - **Concepto profundo: efectos secundarios:** Un composable se puede ejecutar muchas veces, así que no debe hacer trabajo "hacia afuera" (red, navegación) directamente. `LaunchedEffect` es la puerta para eso: lanza una corrutina **cuando el composable entra** en la composición, la cancela cuando sale y la reinicia si cambian sus claves (`effects`, `lifecycleOwner`).
> - **`rememberUpdatedState`:** Si `onEffect` fuera una clave del `LaunchedEffect`, cada recomposición con una lambda nueva reiniciaría la escucha. `rememberUpdatedState` guarda la versión más nueva sin reiniciar nada.
> - **Qué pasa con un efecto con la app minimizada:** `repeatOnLifecycle(STARTED)` deja de recoger. El efecto queda guardado en el `Channel` (`BUFFERED` guarda hasta 64) y se entrega al volver. Así no se intenta navegar con la app en segundo plano.
> - **Costo cero:** Los genéricos de Kotlin se borran al compilar (*type erasure*) y las propiedades de extensión se compilan como funciones estáticas. Ninguno de los dos cuesta nada al ejecutar.

## 3.2 Una lista desplazable: `LazyColumn`

**Concepto.** `LazyColumn` dibuja **solo las filas visibles**. Una `Column` con 200 productos los dibujaría todos.

```kotlin
// InventoryScreen.kt
@Composable
private fun InventoryList(
    sections: List<InventorySection>,
    onEvent: (InventoryEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),                             // 1
        contentPadding = PaddingValues(                                // 2
            start = MaterialTheme.spacing.extraLarge,
            end = MaterialTheme.spacing.extraLarge,
            top = MaterialTheme.spacing.large,
            bottom = FloatingActionButtonSpace                         // 3
        )
    ) {
        sections.forEachIndexed { index, section ->                    // 4
            item(key = "section_${section.status.name}") {             // 5
                ExpirySectionHeader(
                    status = section.status,
                    modifier = if (index > 0) Modifier.padding(top = SectionGap) else Modifier  // 6
                )
            }
            items(items = section.items, key = { item -> item.id }) { item ->  // 7
                InventoryItemRow(
                    item = item,
                    status = section.status,
                    onEditClicked = { id -> onEvent(InventoryEvent.EditClicked(id)) }  // 8
                )
            }
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Ocupa todo el espacio disponible |
| 2 | Margen **dentro** del área que se desplaza: el contenido no queda pegado al borde, pero el desplazamiento llega hasta el borde |
| 3 | Espacio al final para que el botón "+" no tape la última fila |
| 4 | Recorre las secciones ("Vencido", "Vence pronto"…) con su posición |
| 5 | `item` agrega **una** fila: el encabezado. La `key` lo identifica de forma única |
| 6 | La primera sección no lleva margen arriba; las siguientes, sí |
| 7 | `items` agrega una fila por producto. `key = item.id`: si un producto cambia de sección, Compose lo **mueve** en lugar de redibujar toda la lista |
| 8 | La fila avisa con el id del producto y la pantalla lo convierte en evento |

**Decisión.** Encabezados y filas van en la **misma** `LazyColumn` (con `item` + `items`) y no en una lista por
sección. Así hay un solo desplazamiento para todo el inventario.

> **Notas técnicas**
>
> - **Concepto profundo:** `LazyColumn` compone, mide y dibuja **solo** las filas visibles, más algunas que precarga en la dirección del scroll (*prefetch*). Cuando una fila sale de la pantalla, su composición se descarta o se **reutiliza** para una fila nueva.
> - **Optimización pendiente: `contentType`:** La lista mezcla encabezados y filas de producto. Si se agrega `contentType` a `item` (encabezado) y a `items` (producto), Compose solo reutiliza una fila de producto para otra fila de producto, y el scroll es más fluido en teléfonos de gama baja. Hoy no lo usa.
> - **La `key` y la posición del scroll:** Con `key`, si llega un producto nuevo arriba, Compose sabe que las demás filas son las mismas y **conserva la posición del scroll**. Sin `key`, las filas se identificarían por posición y la lista saltaría.
> - **Hardware:** En gama baja, los tirones al deslizar casi siempre vienen de componer filas nuevas durante el movimiento. Por eso el rendimiento se mide con el **APK release**, no con el de debug: `AGENTS.md` registra que en debug Compose pierde unos 300 ms al iniciar cada pantalla. Las librerías de Compose traen sus propios perfiles de compilación (*baseline profiles*); la app no agrega uno para su propio código, y sería una mejora posible.

## 3.3 Una fila: `Row`, `weight`, `clickable`, textos que no se desbordan

```kotlin
// InventoryItemRow.kt (resumen)
Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(RowHeight)                                             // 1
        .clickable { onEditClicked(item.id) },                         // 2
    verticalAlignment = Alignment.CenterVertically                     // 3
) {
    ItemThumbnail(item = item, status = status)                        // 4
    Spacer(modifier = Modifier.width(ThumbnailToTextGap))
    Column(modifier = Modifier.weight(1f)) {                           // 5
        // …
        Text(
            text = item.name,
            maxLines = 1,                                              // 6
            overflow = TextOverflow.Ellipsis,                          // 7
            modifier = Modifier.weight(1f, fill = false)               // 8
        )
        QuantityText(item = item)
        // …
        if (item.isPendingSync) PendingSyncIcon()                      // 9
        IconButton(onClick = { onEditClicked(item.id) }, modifier = Modifier.size(EditButtonSize)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_edit),  // 10
                contentDescription = stringResource(id = R.string.cd_edit_button, item.name),  // 11
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(EditIconSize)
            )
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Alto fijo del Figma, para que todas las filas midan igual |
| 2 | Toda la fila se puede tocar, no solo el lápiz |
| 3 | Centra verticalmente la miniatura y el texto |
| 4 | La foto o, si no tiene, un bloque del color del estado |
| 5 | `weight(1f)`: el texto ocupa **todo lo que sobra** después de la miniatura |
| 6–7 | El nombre usa una sola línea y, si no cabe, termina en "…" |
| 8 | `fill = false`: el nombre ocupa solo lo que necesita, y así la cantidad queda pegada a él y nunca se recorta |
| 9 | Si el cambio no se ha subido a Firebase, muestra el ícono de pendiente |
| 10 | Ícono exportado del Figma (`ic_figma_*`), no de Material |
| 11 | Descripción para TalkBack: "Editar Leche", con el nombre del producto |

> **Notas técnicas**
>
> - **Concepto profundo: una sola medición:** Compose **prohíbe medir dos veces** al mismo hijo. `weight` funciona así: `Row` mide primero los hijos sin peso (miniatura, espacio) y reparte lo que sobra entre los que tienen peso. En las Views clásicas, un `LinearLayout` con pesos anidados medía varias veces y el costo crecía con la profundidad; en Compose no.
> - **Tamaño mínimo de toque:** Los componentes de Material 3 como `IconButton` aseguran un área de toque de al menos 48 × 48 dp aunque el ícono sea más pequeño, la medida mínima que recomienda Android para que un dedo acierte.
> - **Accesibilidad:** La miniatura anuncia el estado ("Vence pronto") en `contentDescription`, porque el color no puede ser la única señal: una persona daltónica puede no distinguir el rojo del amarillo.

## 3.4 Material Design 3: el tema

**Concepto.** `MaterialTheme` reparte colores (`colorScheme`), tipografía (`typography`) y formas a todos los
composables de adentro. Los componentes leen **roles** (`primary`, `surface`, `onSurface`), no colores fijos.

```kotlin
// core/designsystem/theme/Theme.kt
private val LightColorScheme = lightColorScheme(                       // 1
    primary = GreenDark,                                               // 2
    onPrimary = White,                                                 // 3
    secondary = OrangeMain,
    surface = GreenLight,
    onSurface = Black,
    error = ErrorRed,
    // …
)

@Composable
fun SmartfoodTheme(
    themeMode: ThemeMode = ThemeMode.Default,                          // 4
    dynamicColor: Boolean = false,                                     // 5
    content: @Composable () -> Unit                                    // 6
) {
    val darkTheme = themeMode.isDark()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { /* colores del fondo de pantalla */ }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(                                          // 7
        LocalSpacing provides Spacing(),
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)  // 8
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Crea el esquema de color claro de Material 3 |
| 2 | Al **rol** `primary` se le asigna el token `GreenDark` (definido en `Color.kt`) |
| 3 | `onPrimary`: el color del texto que va **encima** de `primary` |
| 4 | Por defecto la app es clara, aunque el teléfono esté en modo oscuro (spec 002) |
| 5 | Color dinámico de Android 12+ apagado: la app conserva los colores de su marca |
| 6 | El contenido es una lambda composable (patrón *slot*) |
| 7 | `CompositionLocalProvider` deja disponibles los espaciados y el modo oscuro para todos los hijos sin pasarlos por parámetro |
| 8 | Aplica colores y tipografía a todo lo de adentro |

**Espaciados propios en el tema:**

```kotlin
// Spacing.kt
data class Spacing(val small: Dp = 8.dp, val medium: Dp = 16.dp, val large: Dp = 24.dp, /* … */)
val LocalSpacing = compositionLocalOf { Spacing() }                    // 1

val MaterialTheme.spacing: Spacing                                     // 2
    @Composable
    @ReadOnlyComposable                                                // 3
    get() = LocalSpacing.current
```

| Línea | Qué hace |
|---|---|
| 1 | Un valor que se comparte por el árbol de composables |
| 2 | Extensión: permite escribir `MaterialTheme.spacing.medium`, igual que `MaterialTheme.colorScheme` |
| 3 | Indica que el getter solo **lee**, lo que permite a Compose optimizarlo |

**Decisión.** Para parecerse al Figma se cambia **qué token usa cada rol** en `LightColorScheme` y nunca el valor de
un token. Así un solo cambio arregla todas las pantallas que usan ese rol.

> **Notas técnicas**
>
> - **Concepto profundo: `CompositionLocal`:** Es una forma de pasar un valor a **todos** los descendientes sin escribirlo como parámetro en cada nivel. `MaterialTheme.colorScheme` es en realidad la lectura de un `CompositionLocal`.
> - **`staticCompositionLocalOf` frente a `compositionLocalOf`:** `LocalIsDarkTheme` es **static**: Compose no anota quién lo lee y, cuando cambia, recompone todo lo que está debajo del proveedor. `LocalSpacing` es **dinámico**: anota cada lectura y solo recompone a quien lo leyó. Static conviene para valores que casi nunca cambian (el modo de color cambia solo cuando el usuario lo elige).
> - **Optimización:** `CompositionLocalProvider(LocalSpacing provides Spacing())` crea un `Spacing` nuevo en cada recomposición del tema, pero `compositionLocalOf` compara con `equals` por defecto. Como `Spacing` es una `data class` con los mismos valores, no invalida nada.
> - **Hardware: modo oscuro y batería:** En pantallas OLED/AMOLED, cada píxel oscuro **gasta menos energía**, porque ese píxel se apaga o casi. En pantallas LCD no hay ahorro: la luz de fondo está siempre encendida.
> - **Decisión técnica:** El color dinámico (Android 12+, a partir del fondo de pantalla) está apagado para que la app conserve los colores de su marca y del Figma.

## 3.5 Animaciones

**Concepto.** `rememberInfiniteTransition` + `animateFloat` producen un número que cambia solo, en bucle. Cada
cambio recompone a quien lo lee.

```kotlin
// InventorySkeleton.kt — brillo del esqueleto de carga
@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)   // 1
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    val progress by rememberInfiniteTransition(label = "skeleton").animateFloat(  // 2
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShimmerMillis, easing = LinearEasing), RepeatMode.Restart),  // 3
        label = "skeleton_shimmer"
    )
    val start = -ShimmerWidth + (ShimmerTravel + ShimmerWidth) * progress  // 4
    return Brush.linearGradient(                                       // 5
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + ShimmerWidth, 0f)
    )
}
```

| Línea | Qué hace |
|---|---|
| 1 | Colores del tema, para que el esqueleto también se vea bien en modo oscuro |
| 2 | `progress` va de 0 a 1 una y otra vez |
| 3 | `tween`: dura `ShimmerMillis` a velocidad constante (`LinearEasing`) y vuelve a empezar |
| 4 | Convierte el progreso en una posición horizontal |
| 5 | Un degradado oscuro → claro → oscuro que se mueve: el "brillo" |

**Decisión.** En lugar de un círculo girando, se muestra un **esqueleto** con la forma de la lista. La persona ve
dónde aparecerán los productos y la espera se siente más corta.

> **Notas técnicas**
>
> - **Concepto profundo: en qué fase se lee la animación:** Un valor animado se puede leer en la composición, en el diseño o en el dibujo. Leerlo en la **composición** (como aquí, al crear el `Brush`) obliga a recomponer, medir y dibujar en cada cuadro. Leerlo solo en el **dibujo** (dentro de `drawBehind` o `graphicsLayer { }`) se salta las dos primeras fases.
> - **Optimización pendiente:** `shimmerBrush()` devuelve un valor, así que no tiene un ámbito de recomposición propio: el cambio de `progress` invalida a `InventorySkeleton` completo en cada cuadro. Leer `progress` dentro de un `Modifier.drawWithCache` o `drawBehind` dejaría la composición quieta. Hoy el costo es pequeño, porque el esqueleto se ve poco tiempo y tiene pocos bloques.
> - **Ejemplo de la técnica correcta en la app:** `AuthScreenScaffold` oculta el formulario mientras elige el perfil de medidas con `graphicsLayer { alpha = if (settled) 1f else 0f }`. El `alpha` se aplica en la capa de dibujo: cambiarlo no vuelve a medir el formulario.
> - **Hardware:** Las animaciones de Compose se ajustan a la frecuencia de la pantalla. En un teléfono de 120 Hz el brillo se ve más suave, pero el presupuesto por cuadro baja a 8,3 ms.

---

# Unidad 4: Navegación y arquitectura de la app

## 4.1 `ViewModel`, `StateFlow` y flujo unidireccional

**Concepto.**
- El **ViewModel** guarda el estado y sobrevive al giro de pantalla.
- `StateFlow` es un flujo que siempre tiene un valor actual.
- **Flujo unidireccional (UDF):** el estado baja del ViewModel a la pantalla y los eventos suben de la pantalla al
  ViewModel.

```kotlin
// LoginViewModel.kt
@HiltViewModel                                                         // 1
class LoginViewModel @Inject constructor(                              // 2
    private val login: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())            // 3
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()      // 4

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)      // 5
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()          // 6

    fun onEvent(event: LoginEvent) {                                   // 7
        when (event) {
            is LoginEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, error = null) }  // 8
            is LoginEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password, error = null) }
            is LoginEvent.RememberMeChanged -> _uiState.update { it.copy(rememberMe = event.isChecked) }
            LoginEvent.LoginClicked, LoginEvent.RetryConnectionClicked -> submit()  // 9
        }
    }

    private fun submit() {
        val state = _uiState.value                                     // 10
        if (state.isLoading) return                                    // 11

        _uiState.update { it.copy(isLoading = true, error = it.error.takeIf { e -> e == AuthError.Network }) }  // 12

        viewModelScope.launch {                                        // 13
            login(state.email, state.password, state.rememberMe)       // 14
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _effects.send(LoginEffect.NavigateToHome)          // 15
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.toAuthError()) }  // 16
                }
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1–2 | Hilt crea el ViewModel y le entrega el `LoginUseCase` (unidad 5) |
| 3 | Estado **privado y modificable**: solo el ViewModel lo cambia. El `_` del nombre es la convención para lo privado |
| 4 | Versión **pública de solo lectura**: la pantalla no puede cambiar el estado por su cuenta |
| 5 | `Channel` para los efectos. `BUFFERED` los guarda si en ese momento nadie escucha (por ejemplo, durante un giro) |
| 6 | Cada efecto se entrega **una sola vez** a quien lo recoge |
| 7 | Una única puerta de entrada para todo lo que hace el usuario |
| 8 | `update { it.copy(...) }` cambia el estado de forma segura aunque haya varias corrutinas. Borra el error porque el usuario está corrigiendo |
| 9 | Dos eventos con la misma acción, en una sola rama |
| 10 | Toma una foto del estado actual |
| 11 | Si ya está cargando, ignora el toque repetido |
| 12 | Activa la carga. Si el error anterior era de red, lo conserva para que el diálogo de "sin conexión" siga visible mientras reintenta |
| 13 | Corrutina que se cancela sola si se cierra la pantalla (unidad 5) |
| 14 | Llama al caso de uso, que valida y habla con Firebase. Devuelve `Result` |
| 15 | Éxito: **efecto** de navegar, no un campo del estado |
| 16 | Fallo: el error tipado queda en el estado y la pantalla lo pinta |

**¿Por qué la navegación es un efecto y no un `val goHome: Boolean` del estado?** Si fuera estado, al volver a la
pantalla o girarla, `goHome` seguiría en `true` y navegaría otra vez. Un efecto ocurre una vez y desaparece.

**¿Por qué no hay validaciones aquí?** Están **solo** en `LoginUseCase` (capa `domain`). El ViewModel traduce eventos
a estado; así la regla se prueba una vez y no se repite.

> **Notas técnicas**
>
> - **Concepto profundo: qué sobrevive y qué no:** El ViewModel sobrevive al **giro de pantalla**, pero no a que Android **cierre el proceso** para liberar memoria con la app en segundo plano. En ese caso, el `LoginUiState` vuelve vacío al regresar. Para que algo sobreviva al cierre del proceso hay que guardarlo en `SavedStateHandle`; el editor de productos lo recibe para leer el id del producto que se edita (`ProductEditorViewModel`, línea 51).
> - **`StateFlow` descarta valores repetidos:** Si `update` produce un estado **igual** (`equals`) al anterior, `StateFlow` no emite y la pantalla no se recompone. Además es *conflated*: si el estado cambia tres veces antes de que la pantalla lo lea, la pantalla recibe solo el último.
> - **Seguridad entre hilos:** `_uiState.update { }` usa una operación atómica (*compare-and-set*) en un bucle: si dos corrutinas lo cambian a la vez, ninguna pisa a la otra. Leer `value`, copiarlo y asignarlo en dos pasos sí podría perder un cambio.
> - **`Channel` frente a `SharedFlow`:** Un `Channel` entrega cada efecto a **un solo** receptor y lo guarda si nadie escucha. Un `SharedFlow` sin *replay* lo perdería si la pantalla no está escuchando en ese instante (por ejemplo, durante un giro). Por eso los efectos de navegación van por `Channel`.
> - **Hilo:** `viewModelScope` corre en `Dispatchers.Main.immediate`. Las llamadas a Firebase no bloquean el hilo principal porque son funciones `suspend`: esperan la respuesta sin ocupar el hilo.

## 4.2 Un flujo que se observa: el inventario en tiempo real

```kotlin
// InventoryViewModel.kt
private var observeJob: Job? = null                                    // 1

init { observeInventory() }                                            // 2

private fun observeInventory() {
    observeJob?.cancel()                                               // 3
    observeJob = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }    // 4
        observeInventoryUseCase()                                      // 5
            .catch { cause ->                                          // 6
                _uiState.update { it.copy(isLoading = false, error = cause.toInventoryError()) }
            }
            .collect { sections ->                                     // 7
                _uiState.update { it.copy(isLoading = false, sections = sections, error = null) }
            }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Guarda la tarea de observación para poder cancelarla |
| 2 | Empieza a observar apenas se crea el ViewModel |
| 3 | Al reintentar, cancela la observación anterior para no tener dos a la vez |
| 4 | Muestra el esqueleto de carga |
| 5 | Un `Flow` que emite cada vez que Firestore cambia |
| 6 | Si falla, guarda el error y la pantalla muestra "Reintentar" |
| 7 | Cada emisión actualiza la lista **sin recargar**: si se agrega un producto en otro teléfono, aparece solo |

> **Notas técnicas**
>
> - **Concepto profundo:** `observeInventoryUseCase()` termina en un *listener* de Firestore (`snapshots()`), que mantiene una conexión abierta y recibe cada cambio del servidor. También emite desde la caché local, por eso la lista aparece aunque no haya red.
> - **Optimización pendiente:** La observación corre en `viewModelScope`, así que sigue activa mientras el ViewModel exista, **aunque la app esté minimizada**. `InventorySearchViewModel` ya usa el patrón recomendado: `stateIn(started = SharingStarted.WhileSubscribed(StopTimeoutMs))`, que detiene la escucha poco después de que la pantalla deja de mirar y la reanuda al volver. Aplicarlo a la lista ahorraría red y batería en segundo plano.
> - **Hardware:** Cuando el teléfono entra en reposo profundo (*Doze*), Android limita la red de las apps en segundo plano. Al salir del reposo, Firestore se reconecta solo y entrega los cambios acumulados.

## 4.3 La pantalla elige qué mostrar según el estado

```kotlin
// InventoryScreen.kt
@Composable
fun InventoryScreen(state: InventoryUiState, onEvent: (InventoryEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        InventoryTopBar(onSearchClicked = { onEvent(InventoryEvent.SearchClicked) })  // 1
        Box(modifier = Modifier.weight(1f)) {                          // 2
            val error = state.error                                    // 3
            when {
                state.isLoading -> InventorySkeleton()                 // 4
                error != null -> ErrorState(error = error, onRetry = { onEvent(InventoryEvent.RetryClicked) })
                state.isEmpty -> EmptyState(onAddClicked = { onEvent(InventoryEvent.AddClicked) })
                else -> InventoryList(sections = state.sections, onEvent = onEvent)
            }
            FloatingActionButton(                                      // 5
                onClick = { onEvent(InventoryEvent.AddClicked) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(MaterialTheme.spacing.large),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) { Icon(painter = painterResource(id = R.drawable.ic_figma_add), contentDescription = stringResource(id = R.string.cd_add_button)) }
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | La barra superior solo avisa "tocaron la lupa"; navegar lo decide el ViewModel |
| 2 | `weight(1f)` dentro de `Column`: el `Box` ocupa todo el alto debajo de la barra |
| 3 | Se copia a una variable local para que Kotlin sepa que no es `null` dentro del `when` (*smart cast*) |
| 4 | Los cuatro estados de una pantalla con datos: **carga, error, vacío y contenido**. Nunca queda en blanco |
| 5 | `Box` permite poner el botón "+" **encima** de la lista, abajo a la derecha |

**Decisión.** `InventoryScreen` no sabe de dónde vienen los datos ni cómo se navega. Por eso se prueba con estados
escritos a mano: uno de carga, uno de error, uno vacío y uno con productos.

> **Notas técnicas**
>
> - **Concepto profundo:** En el `when`, solo la rama activa existe en la composición. Al pasar de "cargando" a "contenido", el esqueleto se **descarta**: se liberan su animación y su memoria, y la lista se compone desde cero.
> - **Consecuencia:** Cualquier estado local dentro de una rama (un `remember`) se pierde al cambiar de rama. Por eso el estado importante está en el ViewModel y no en las ramas.

## 4.4 Navigation Compose: el cableado de una pantalla

```kotlin
// AuthNavGraph.kt
fun NavGraphBuilder.authGraph(navController: NavHostController) {      // 1
    navigation<Route.AuthGraph>(startDestination = Route.Login) {      // 2
        composable<Route.Login> {                                      // 3
            val viewModel: LoginViewModel = hiltViewModel()            // 4
            val state by viewModel.uiState.collectAsStateWithLifecycle()  // 5
            val autofill = LocalAutofillManager.current

            ObserveEffects(viewModel.effects) { effect ->              // 6
                when (effect) {
                    LoginEffect.NavigateToHome -> {
                        autofill?.commit()                             // 7
                        navController.navigateToMainGraph()            // 8
                    }
                }
            }

            LoginScreen(                                               // 9
                state = state,
                onEvent = viewModel::onEvent,                          // 10
                onNavigateToRegister = { navController.navigate(Route.Register) },  // 11
                onNavigateToForgotPassword = { navController.navigate(Route.PasswordRecovery) }
            )
        }
        // … Register, PasswordRecovery …
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Función de extensión de `NavGraphBuilder`: cada feature declara sus pantallas en su propio archivo |
| 2 | Grafo **anidado** de autenticación, que empieza en Login |
| 3 | Registra la pantalla con una ruta **tipada** (`Route.Login`), no con un `String` |
| 4 | Crea el ViewModel atado a esta entrada de la pila: si se sale de Login, se destruye |
| 5 | Convierte el `StateFlow` en estado de Compose y deja de escuchar cuando la pantalla no está visible |
| 6 | Escucha los efectos de un solo uso (sección 3.1) |
| 7 | El teléfono ofrece guardar la contraseña |
| 8 | Entra a la app y limpia la pila (sección 4.5) |
| 9 | Dibuja la pantalla sin estado con lo que dice el ViewModel |
| 10 | Referencia a función: equivale a `{ event -> viewModel.onEvent(event) }` |
| 11 | La navegación simple (sin lógica) se hace directo desde la ruta |

**Decisión: patrón Route/Screen.** Cada pantalla tiene dos partes:
- **La ruta** (aquí, el bloque `composable<Route.Login> { }`) conoce el ViewModel, Hilt y la navegación.
- **La Screen** solo conoce `state` y `onEvent`.

Así la Screen se puede ver en `@Preview` y probar sin nada de lo demás.

> **Notas técnicas**
>
> - **Concepto profundo: un ViewModel por entrada de la pila:** `hiltViewModel()` guarda el ViewModel en el `ViewModelStore` de esa **entrada de la pila** (`NavBackStackEntry`). Si abres Login, vas a Registro y vuelves, el `LoginViewModel` es el mismo y conserva lo escrito. Si Login sale de la pila (con `popUpTo`), se destruye y libera memoria.
> - **Optimización:** La referencia `viewModel::onEvent` es estable: el *strong skipping* la recuerda, así que `LoginScreen` se puede saltar cuando su `state` no cambia.

## 4.5 Rutas tipadas, el `NavHost` y la pila

```kotlin
// Routes.kt
@Serializable                                                          // 1
sealed interface Route {
    @Serializable data object Login : Route { /* … */ }                // 2
    @Serializable data class ProductEditor(val itemId: String? = null) : Route { /* … */ }  // 3
    @Serializable data class RecipeDetail(val recipeId: String) : Route { /* … */ }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Kotlin Serialization convierte la ruta en texto para la pila de navegación |
| 2 | Una pantalla sin argumentos |
| 3 | Una pantalla **con argumentos**: `navigate(Route.ProductEditor(itemId = "abc"))`. El compilador revisa el tipo; `null` significa "producto nuevo" |

```kotlin
// AppNavHost.kt
NavHost(
    navController = navController,
    startDestination = Route.Splash,                                   // 1
    enterTransition = NavTransitions.enter,                            // 2
    exitTransition = NavTransitions.exit,
    popEnterTransition = NavTransitions.popEnter,
    popExitTransition = NavTransitions.popExit
) {
    composable<Route.Splash> { /* decide Login o Home */ }
    authGraph(navController)                                           // 3
    mainGraph(navController)
    legalDestinations(navController)
}

fun NavHostController.navigateToMainGraph() {
    navigate(Route.MainGraph) {
        popUpTo(graph.id) { inclusive = true }                         // 4
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | La app siempre arranca en Splash, que revisa si hay sesión |
| 2 | Las transiciones se declaran **una vez** para todas las pantallas |
| 3 | Cada feature aporta su grafo |
| 4 | Borra **toda** la pila: después de entrar, "atrás" cierra la app en vez de volver al login |

**Transiciones (también de la unidad 3, animación):**

```kotlin
// NavTransitions.kt
private val pushSpec = tween<IntOffset>(durationMillis = PUSH_DURATION_MS, easing = FastOutSlowInEasing)  // 1

val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(towards = SlideDirection.Left, animationSpec = pushSpec)  // 2
}
```

| Línea | Qué hace |
|---|---|
| 1 | 600 ms con aceleración suave al inicio y al final (`FastOutSlowInEasing`), la curva del prototipo (RNF-06) |
| 2 | La pantalla nueva entra desde la derecha ("empuje") |

> **Notas técnicas**
>
> - **Concepto profundo: rutas tipadas:** El plugin de Kotlin Serialization genera **al compilar** el código que convierte `Route.ProductEditor(itemId = "abc")` en texto y de vuelta, sin reflexión. Aun así, `AGENTS.md` pide revisar los cambios de serialización contra `app/proguard-rules.pro`, porque R8 en modo completo puede quitar clases que solo se usan de forma indirecta.
> - **Memoria de la pila:** Cada pantalla en la pila conserva su ViewModel y su estado guardado. `popUpTo(graph.id) { inclusive = true }`, al entrar a la app, libera todo lo de autenticación.
> - **Hardware: la transición:** Durante los 600 ms del empuje, **las dos pantallas** están compuestas y se dibujan a la vez. Es el momento más pesado de la navegación; por eso en debug se nota lento y en release no.

## 4.6 Devolver un resultado a la pantalla anterior

```kotlin
// InventoryNavGraph.kt (editor)
onBackWithResult = { result ->
    navController.previousBackStackEntry?.savedStateHandle?.set(EDITOR_RESULT_KEY, result.encode())  // 1
    navController.popBackStack()                                       // 2
}
```

| Línea | Qué hace |
|---|---|
| 1 | Deja el resultado ("Guardado", "Eliminado") en la pantalla **anterior** de la pila |
| 2 | Vuelve. La lista lee ese resultado con `EditorResultHost` y muestra el mensaje encima |

> **Notas técnicas**
>
> - **Concepto profundo:** `SavedStateHandle` solo acepta valores que caben en un `Bundle` (texto, números, `Parcelable`). Por eso el resultado se convierte a texto con `result.encode()` antes de guardarlo.
> - **Ventaja:** Como vive en el `SavedStateHandle`, el resultado sobrevive incluso a que Android cierre el proceso entre que el editor guarda y la lista lo muestra.

## 4.7 Adaptar la app a distintos tamaños de pantalla

**Concepto.** El curso usa *window size classes* (`Compact`, `Medium`, `Expanded`) según el **ancho**. SmartFood
aplica la misma idea según el **alto**, porque el problema de los formularios son los teléfonos bajos.

```kotlin
// ProductEditorScreen.kt
BoxWithConstraints(                                                    // 1
    modifier = Modifier.weight(1f).navigationBarsPadding()             // 2
) {
    CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {  // 3
        Box(modifier = Modifier.fillMaxSize().imePadding()) {          // 4
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EditorForm(state = state, onEvent = onEvent)
            }
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | `BoxWithConstraints` da el tamaño disponible (`maxHeight`, `maxWidth`) para decidir el diseño |
| 2 | Deja espacio para la barra de navegación del sistema (borde a borde) |
| 3 | Elige el perfil `Compact`, `Medium` o `Regular` según el alto, y lo entrega a todos los campos de adentro |
| 4 | `imePadding()`: cuando se abre el teclado, el contenido se recorta para no quedar debajo de él |

```kotlin
// FormMetrics.kt
fun formMetricsFor(availableHeight: Dp): FormMetrics = when {
    availableHeight >= FormMetrics.RegularMinHeight -> FormMetrics.Regular
    availableHeight >= FormMetrics.MediumMinHeight -> FormMetrics.Medium
    else -> FormMetrics.Compact
}
```

**Decisión.** El perfil se calcula **sin** el teclado (`imePadding` va adentro). Si se calculara con el teclado
abierto, los campos cambiarían de tamaño cada vez que aparece.

**Desplazamiento del formulario:**

```kotlin
// ProductEditorScreen.kt
Column(
    verticalArrangement = Arrangement.spacedBy(metrics.fieldSpacing),  // 1
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())                         // 2
        .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
) { /* campos */ }
```

| Línea | Qué hace |
|---|---|
| 1 | Mismo espacio entre todos los campos, sin un `Spacer` entre cada uno |
| 2 | La columna se desplaza. `rememberScrollState()` recuerda hasta dónde se bajó |

> **Notas técnicas**
>
> - **Concepto profundo: `BoxWithConstraints`:** Por dentro usa `SubcomposeLayout`: **pospone la composición** de su contenido hasta la fase de diseño, cuando ya conoce el tamaño. Es más costoso que un `Box` normal, así que se usa solo donde la medida decide qué componer (aquí, el perfil de `FormMetrics`).
> - **El teclado sin recomponer:** Cuando el teclado se abre, `WindowInsets.ime` cambia **en cada cuadro** de su animación. En `AuthScreenScaffold`, el encabezado de marca lee ese valor dentro de `Modifier.layout { }`, es decir, en la **fase de diseño**: se encoge cuadro a cuadro sin recomponer. Si lo leyera en la composición, se recompondría en cada cuadro de la animación del teclado.
> - **Hardware:** El alto útil cambia mucho entre teléfonos: pantallas bajas, cámaras dentro de la pantalla y una barra de navegación de 3 botones (más alta) frente a la de gestos (más delgada). Por eso el perfil se elige con el alto **sin** las barras del sistema.
> - **Decisión técnica:** `AGENTS.md` prohíbe el scroll programático (`scrollTo`). El margen del teclado se aplica fuera del `verticalScroll`, y Compose mantiene visible el campo enfocado por su cuenta.

## 4.8 Diálogos controlados por el estado

```kotlin
// ProductEditorScreen.kt
when (state.dialog) {                                                  // 1
    EditorDialog.CategorySheet -> OptionSheet(
        title = stringResource(id = R.string.editor_category_sheet_title),
        options = CategoryOptions,
        selected = state.category,
        label = { stringResource(id = it.titleRes) },
        onConfirm = { onEvent(ProductEditorEvent.CategoryPicked(it)) },  // 2
        onDismiss = dismiss                                            // 3
    )
    EditorDialog.ConfirmDelete -> SmartFoodConfirmDialog(/* … */)
    // …
    null -> Unit                                                       // 4
}
```

| Línea | Qué hace |
|---|---|
| 1 | El diálogo abierto es **parte del estado** (`dialog: EditorDialog?`) |
| 2 | Elegir una opción es un evento; el ViewModel guarda la categoría y cierra el diálogo |
| 3 | Tocar fuera o "atrás" envía `DialogDismissed` |
| 4 | Sin diálogo, no se dibuja nada |

**Decisión.** Si el diálogo fuera un `remember { mutableStateOf(false) }` local, el ViewModel no podría cerrarlo al
guardar y un giro de pantalla lo cerraría solo.

> **Notas técnicas**
>
> - **Concepto profundo:** Un `Dialog` de Compose abre una **ventana nueva** del sistema, con su propia composición encima de la pantalla. Por eso se dibuja sobre todo, incluida la barra inferior.
> - **Hardware y versión de Android:** El fondo desenfocado del diálogo sin conexión (`Modifier.blur`) solo funciona desde **Android 12** (API 31). En versiones anteriores, que la app soporta desde `minSdk` 24, el desenfoque no tiene efecto y queda solo el oscurecimiento propio del diálogo (comentario en `AuthScreenScaffold.kt`).

---

# Unidad 5: Cómo conectarse a Internet

## 5.1 Corrutinas en la interfaz

```kotlin
// ProductEditorRoute.kt
val scope = rememberCoroutineScope()                                   // 1
val context = LocalContext.current

val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->  // 2
    if (uri != null) {
        scope.launch {                                                 // 3
            val file = withContext(Dispatchers.IO) { copyPickedPhoto(context, uri) }  // 4
            file?.let { viewModel.onEvent(ProductEditorEvent.PhotoCaptured(it.path)) }  // 5
        }
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Un alcance de corrutinas atado a este composable: se cancela si la pantalla sale |
| 2 | Abre el selector de fotos de Android. No pide permisos y solo da acceso a la foto elegida |
| 3 | Lanza trabajo sin congelar la pantalla |
| 4 | `Dispatchers.IO`: copiar un archivo es trabajo de disco, así que va fuera del hilo principal |
| 5 | Ya en el hilo principal, avisa al ViewModel con la ruta de la foto |

| Herramienta | Dónde se usa | Para qué |
|---|---|---|
| `viewModelScope.launch` | ViewModels | Lógica que debe sobrevivir a recomposiciones |
| `rememberCoroutineScope()` | Rutas | Trabajo que inicia un toque del usuario |
| `LaunchedEffect(clave)` | Rutas y `ObserveEffects` | Trabajo que inicia la pantalla y se reinicia si cambia la clave |
| `withContext(Dispatchers.IO)` | Archivos y fotos | Disco o red fuera del hilo principal |

> **Notas técnicas**
>
> - **Concepto profundo: concurrencia estructurada:** Toda corrutina vive dentro de un alcance (`scope`). Si el alcance se cancela, se cancelan todas sus corrutinas hijas. Al salir del editor, `rememberCoroutineScope` se cancela y con él la copia de la foto en curso: no queda trabajo huérfano.
> - **Hilos:** `Dispatchers.Main` es **un solo hilo**, el que dibuja: si se bloquea unos 5 segundos, Android muestra "La app no responde" (ANR). `Dispatchers.IO` tiene un grupo de hasta 64 hilos para disco y red, que pasan la mayor parte del tiempo esperando. `Dispatchers.Default` tiene tantos hilos como núcleos del procesador y es para cálculo; ahí corre el agente de recetas.
> - **Hardware:** Copiar una foto de varios MB desde la galería tarda más en un teléfono con almacenamiento lento (eMMC, común en gama baja) que en uno con UFS. En `IO`, esa diferencia nunca congela la pantalla.
> - **Permisos según la versión:** `PickVisualMedia` usa el selector de fotos del sistema y no pide permiso de almacenamiento; en teléfonos sin ese selector, la librería abre el selector de documentos de Android. El permiso de notificaciones solo existe desde Android 13, por eso el código revisa `Build.VERSION.SDK_INT >= TIRAMISU`.

## 5.2 Repositorio e inyección de dependencias

**Concepto.** El curso arma un `AppContainer` a mano para entregar el repositorio al ViewModel. SmartFood hace lo
mismo con **Hilt**:

| En el curso | En SmartFood |
|---|---|
| `class AppContainer { val repo = … }` | `@Module` + `@Binds` (`InventoryDataModule`) |
| `ViewModelProvider.Factory` a mano | `@HiltViewModel` + `@Inject constructor` |
| `viewModel(factory = …)` | `hiltViewModel()` |
| `Application` que crea el contenedor | `@HiltAndroidApp class SmartfoodApp` |

**Decisión.** Con casi 20 pantallas, escribir una *factory* por ViewModel sería mucho código repetido. Hilt la
genera.

> **Notas técnicas**
>
> - **Concepto profundo:** Hilt genera las *factories* al compilar. Al arrancar no escanea clases ni usa reflexión, así que el costo de inicio es casi el mismo que escribirlas a mano.
> - **Memoria:** Lo que es caro de crear (`FirebaseFirestore`, `FirebaseAuth`, el `DataStore`) se provee como `@Singleton` en `CoreDataModule`: una sola instancia en toda la app.
> - **Pruebas:** Como el ViewModel recibe sus dependencias por el constructor, las pruebas unitarias le pasan *fakes* (`FakeInventoryRepository`) sin Hilt ni Firebase.

## 5.3 Imágenes de Internet con Coil

```kotlin
// InventoryItemRow.kt
val photoModel: Any? = item.localPhotoPath?.let { File(it) } ?: item.photo?.url  // 1
if (photoModel == null) {
    Box(modifier = Modifier.size(ThumbnailHeight).background(color = status.color, shape = TrailingThumbnailShape))  // 2
} else {
    AsyncImage(                                                        // 3
        model = photoModel,
        contentDescription = stringResource(id = R.string.cd_item_photo, item.name),
        contentScale = ContentScale.Crop,                              // 4
        modifier = Modifier.size(ThumbnailHeight).clip(TrailingThumbnailShape)  // 5
    )
}
```

| Línea | Qué hace |
|---|---|
| 1 | Operador Elvis `?:`: usa la foto guardada en el teléfono si existe y, si no, la URL de Firebase Storage |
| 2 | Sin foto: un bloque del color del estado de vencimiento |
| 3 | `AsyncImage` (Coil 3) descarga la imagen en segundo plano, la guarda en caché y la muestra |
| 4 | Recorta la imagen para llenar el cuadro sin deformarla |
| 5 | Le da la forma redondeada del Figma |

**Decisión.** Primero se muestra la foto **local**. Así una foto tomada sin conexión se ve de inmediato, aunque
todavía no se haya subido.

> **Notas técnicas**
>
> - **Concepto profundo: la caché de Coil:** Coil tiene dos cachés: en **memoria** (las imágenes ya decodificadas que se vieron hace poco) y en **disco** (los archivos descargados). Volver a la lista no descarga las fotos otra vez.
> - **Optimización: tamaño de decodificación:** Coil decodifica la imagen al tamaño del composable que la muestra, no al tamaño original. Una miniatura de 60 dp ocupa poca memoria aunque la foto original sea mucho más grande.
> - **Hardware: memoria de una foto:** Una foto de 12 MP (4000 × 3000) decodificada ocupa unos 48 MB (4 bytes por píxel). Por eso la app comprime las fotos **antes de subirlas**: lado mayor de 1080 px y calidad JPEG 80 (`PhotoScaling.kt`). Se ahorran memoria, datos móviles y espacio en Storage, que solo acepta archivos de menos de 2 MB (`storage.rules`).

---

# Unidad 6: Persistencia de datos

**Concepto.** DataStore guarda pares clave-valor en el teléfono, con lecturas como `Flow`.

| Qué se guarda con DataStore | Archivo |
|---|---|
| "Recuérdame" del inicio de sesión | `feature/auth/data/local/DataStoreSessionPreferences.kt` |
| Modo claro u oscuro | `core/preferences/DataStoreThemePreferences.kt` |
| Ajustes de notificaciones | `feature/profile/data/local/DataStoreNotificationSettings.kt` |
| Preferencias del inventario | `feature/inventory/data/local/DataStoreInventoryPreferences.kt` |

Cómo llega a la pantalla el modo guardado:

```kotlin
// ThemeViewModel.kt
private val _themeMode = MutableStateFlow(ThemeMode.Default)           // 1
val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

init {
    viewModelScope.launch {
        preferences.themeMode.collect { mode -> _themeMode.value = mode }  // 2
    }
}
```

| Línea | Qué hace |
|---|---|
| 1 | Empieza en el modo por defecto (claro) mientras DataStore lee el modo guardado |
| 2 | Cuando DataStore lee el modo guardado (o el usuario lo cambia), el tema se actualiza. `MainActivity` lo lee con `collectAsStateWithLifecycle()` (sección 1.1) |

**Decisión: ¿por qué no Room?** Los datos del inventario viven en **Firestore**, que ya guarda una copia en el
teléfono y funciona sin conexión. Room duplicaría esa copia. DataStore queda para los ajustes pequeños.

> **Notas técnicas**
>
> - **Concepto profundo: DataStore frente a SharedPreferences:** DataStore lee y escribe con corrutinas, fuera del hilo principal, y cada escritura es una **transacción atómica**: se guarda todo o nada. `SharedPreferences` podía bloquear el hilo principal al guardar y causar ANR.
> - **Regla de uso:** Solo puede existir **una instancia** de DataStore por archivo; dos instancias sobre el mismo archivo fallan. Por eso se crea una sola vez en `CoreDataModule` y se inyecta.
> - **La caché de Firestore:** Firestore guarda su copia local en una base de datos interna del teléfono (SQLite en Android). Es la "persistencia" del inventario y la razón de que no haga falta Room.

---

# Unidad 7: WorkManager

Las pantallas no usan WorkManager directamente, pero sí lo que hace:
- `PhotoUploadWorker` sube las fotos cuando vuelve la red.
- `ExpiryReminderWorker` envía el aviso diario de vencimientos.

`SmartfoodApp` implementa `Configuration.Provider` para que WorkManager cree los *workers* con Hilt
(`HiltWorkerFactory`).

**Decisión.** Guardar un producto con foto **no espera** a que la foto suba. La pantalla vuelve enseguida con
"Guardado" y WorkManager termina la subida aunque la app se cierre.

> **Notas técnicas**
>
> - **Concepto profundo:** WorkManager se apoya en el `JobScheduler` del sistema. Las tareas sobreviven a que se cierre la app y a que se reinicie el teléfono, y se ejecutan cuando se cumplen sus condiciones (por ejemplo, "hay red").
> - **Hardware y batería:** Android agrupa las tareas en segundo plano y las puede **retrasar** en reposo profundo (*Doze*) o con ahorro de batería. Por eso el aviso de vencimientos es "diario" y no "a las 8:00 en punto".
> - **Pruebas:** `CustomTestRunner` inicia un WorkManager de prueba (`SynchronousExecutor`): ninguna prueba instrumentada encola trabajos en el WorkManager real del teléfono.

---

# Unidad 8: Views y Compose

**Concepto.** `AndroidView` mete una View clásica de Android dentro de Compose. Se usa cuando no existe un
composable equivalente.

```kotlin
// ProductCameraRoute.kt
val controller = remember {                                            // 1
    LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
}
LaunchedEffect(permission) {                                           // 2
    if (permission == CameraPermission.GRANTED) controller.bindToLifecycle(lifecycleOwner)
}

ProductCameraScreen(
    permission = permission,
    preview = {                                                        // 3
        AndroidView(
            factory = { viewContext ->                                 // 4
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    },
    // …
)
```

| Línea | Qué hace |
|---|---|
| 1 | Crea el controlador de CameraX **una vez** (`remember`) y solo para tomar fotos |
| 2 | Cuando hay permiso, conecta la cámara al ciclo de vida: se apaga sola al salir |
| 3 | La Screen recibe la vista previa como **slot**, así se puede probar sin cámara real |
| 4 | `factory` crea la View de CameraX una sola vez y Compose la ubica como cualquier otro elemento |

**Decisión.** La vista previa usa `PreviewView`, la View de CameraX (`androidx.camera.view`). Como llega por
parámetro, `ProductCameraScreen` sigue sin estado y sus pruebas pueden pasarle un `Box` en lugar de la cámara.

> **Notas técnicas**
>
> - **Concepto profundo: el costo de mezclar:** Una View dentro de Compose tiene su propia medición y su propio dibujo, separados del árbol de Compose. Funciona bien, pero cada `AndroidView` suma una capa más, así que se usa solo donde no hay alternativa.
> - **Hardware: la cámara es un recurso exclusivo:** Solo una app puede usar la cámara a la vez. `bindToLifecycle` la **suelta** cuando la pantalla se detiene, para que otra app la pueda abrir y para no gastar batería con el sensor encendido.
> - **Decisión técnica:** `setEnabledUseCases(IMAGE_CAPTURE)` activa solo la captura de fotos, no el video ni el análisis de imágenes, y así usa menos memoria y procesador.

---

# Resumen: las reglas de toda pantalla de SmartFood

| Regla | Unidad | Por qué |
|---|---|---|
| La Screen no tiene estado: recibe `state` y `onEvent` | 2 y 4 | Se puede previsualizar y probar sin Firebase |
| Contract aparte: `UiState`, `Event` y `Effect` | 2 y 4 | El contrato de la pantalla se lee de un vistazo |
| Los campos en rojo se derivan del `error` | 2 | Una sola fuente de verdad |
| Navegación por `Effect` con `Channel`, recogida con `ObserveEffects` | 4 | Navega una vez, aunque se gire la pantalla |
| Validaciones solo en los casos de uso | 4 | No se repiten en el ViewModel |
| Textos en `strings.xml` y colores del tema | 1 y 3 | Traducción y modo oscuro sin tocar las pantallas |
| Componentes `SmartFood*` y medidas de `LocalFormMetrics` | 3 y 4 | Fidelidad con el Figma en todos los teléfonos |
| `LazyColumn` con `key` en las listas | 3 | Rendimiento y animaciones correctas |
| Carga, error, vacío y contenido en un `when` | 4 | La pantalla nunca queda en blanco |
| Rutas tipadas y una transición declarada una vez | 4 | El compilador revisa los argumentos; todas las pantallas animan igual |


## Rendimiento y hardware, en una tabla

| Situación | Qué hace la app | Dónde |
|---|---|---|
| Teléfono de 120 Hz (8,3 ms por cuadro) | Las animaciones solo existen mientras hacen falta; el `alpha` se cambia en la capa de dibujo | `Buttons.kt`, `AuthScreenScaffold.kt` |
| Se abre el teclado | El encabezado se encoge en la fase de diseño, sin recomponer | `AuthScreenScaffold.kt` |
| Pantalla baja o con barra de 3 botones | Perfil `Compact`, `Medium` o `Regular` según el alto útil | `FormMetrics.kt` |
| Fotos de 12 MP | Se reducen a 1080 px y JPEG 80 antes de subirlas; Coil decodifica al tamaño de la miniatura | `PhotoScaling.kt`, `InventoryItemRow.kt` |
| Pantalla OLED | El modo oscuro ahorra batería | `Theme.kt` |
| Android 11 o anterior | Sin desenfoque en el diálogo sin conexión; queda el oscurecimiento | `AuthScreenScaffold.kt` |
| Android 13 o superior | Pide el permiso de notificaciones tras el primer guardado | `ProductEditorRoute.kt` |
| Sin red | Firestore guarda en el teléfono y WorkManager sube las fotos después | `FirestoreInventoryRepository.kt`, `PhotoUploadWorker.kt` |
| La app pasa a segundo plano | `collectAsStateWithLifecycle` y `repeatOnLifecycle` dejan de escuchar; la cámara se suelta | `ObserveEffects.kt`, `ProductCameraRoute.kt` |

## Mejoras de optimización encontradas al escribir esta guía

| Mejora | Beneficio | Esfuerzo |
|---|---|---|
| `contentType` en la `LazyColumn` del inventario | Scroll más fluido en gama baja | Bajo |
| Leer el brillo del esqueleto en la fase de dibujo | El esqueleto no se recompone en cada cuadro | Bajo |
| `stateIn(WhileSubscribed)` en `InventoryViewModel`, como en la búsqueda | Menos red y batería con la app minimizada | Medio |
| Quitar `remember(onEvent) { { … } }` en código nuevo | Código más simple; el compilador ya lo hace | Bajo |
| Un *baseline profile* para el código de la app | Arranque y primeras pantallas más rápidos | Medio |

Son mejoras, no errores: la app funciona bien sin ellas. Cualquiera de estos cambios de código pasaría por su Issue y por el modo plan.
