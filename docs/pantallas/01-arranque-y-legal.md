# 01 · Arranque y documentos legales

Pantallas: [Splash](#splashscreen) y [Documentos legales](#legaldocumentscreen).
Índice general y mapa de conceptos: [README](README.md).

Cómo leer el código: cada línea lleva un comentario `//` con lo que hace. Cuando usa un concepto del curso, el
comentario empieza con su unidad, por ejemplo `[U3]`. Debajo de cada pantalla, la tabla **Conceptos del curso**
explica cada concepto a fondo. `// …` marca líneas omitidas (estilos repetidos o `@Preview`).

---

## SplashScreen

Muestra el logo mientras la app decide si hay sesión guardada. Cuando pasaron al menos 2 s **y** ya se sabe el
destino, navega a "Mi Inventario" o a "Iniciar sesión". Spec 001, HU-03 (CA-03-04).

**Qué recibe**

| Parámetro | Tipo | Para qué |
|---|---|---|
| `destination` | `SplashDestination?` | `HOME` o `LOGIN`; `null` mientras se consulta la sesión |
| `onNavigate` | `(SplashDestination) -> Unit` | Se llama una vez con el destino final |
| `modifier` | `Modifier` | Ajustes desde quien la llama |

### Código comentado

Archivo: [SplashScreen.kt, líneas 44–59](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L44-L59)

```kotlin
@Composable                                                   // [U1] marca la función como composable: describe interfaz
fun SplashScreen(
    destination: SplashDestination?,                          // [U2] tipo con "?": puede ser null (seguridad de nulos)
    onNavigate: (SplashDestination) -> Unit,                  // [U2] tipo función: avisa hacia arriba (elevación de estado)
    modifier: Modifier = Modifier                             // [U1] Modifier con valor por defecto, siempre el último
) {
    SystemBarEffect(isLightBackground = true)                 // íconos oscuros en la barra de estado: el fondo es claro

    var minimumTimeElapsed by remember { mutableStateOf(false) }  // [U2] estado local que sobrevive a la recomposición
    LaunchedEffect(Unit) {                                    // [U2] efecto: corrutina que corre UNA vez al entrar
        delay(MIN_SPLASH_DURATION_MS)                         // espera 2 000 ms sin bloquear la interfaz
        minimumTimeElapsed = true                             // cambia el estado → Compose recompone
    }
    LaunchedEffect(minimumTimeElapsed, destination) {         // [U2] se relanza cada vez que cambia una de sus claves
        if (minimumTimeElapsed && destination != null) onNavigate(destination)  // navega solo con las dos condiciones
    }
```

Archivo: [SplashScreen.kt, líneas 61–83](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L61-L83)

```kotlin
    Scaffold(                                                 // [U3] armazón de Material: fondo y espacios del sistema
        modifier = modifier.fillMaxSize(),                    // [U1] el modifier recibido va primero; ocupa toda la pantalla
        containerColor = GreenLight,                          // color de fondo del Scaffold
        contentWindowInsets = WindowInsets(0.dp)              // no reserva espacio para las barras: el logo queda centrado
    ) { paddingValues ->                                      // [U3] el contenido recibe el relleno calculado por el Scaffold
        Box(                                                  // [U1] Box: apila hijos uno encima de otro
            modifier = Modifier
                .fillMaxSize()                                // [U1] ocupa todo el espacio disponible
                .padding(paddingValues)                       // aplica el relleno del Scaffold
                .background(GreenLight),                      // pinta el fondo
            contentAlignment = Alignment.Center,              // [U1] centra al hijo en el Box
        ) {
            Icon(                                             // [U1] dibuja una imagen vectorial o un recurso
                painter = painterResource(id = R.drawable.logo_smartfood_circle),  // [U1] lee el logo de res/drawable
                contentDescription = stringResource(id = R.string.cd_smartfood_logo), // [U1] texto para TalkBack, desde strings.xml
                tint = Color.Unspecified,                     // sin tinte: conserva los colores originales del logo
                modifier = Modifier
                    .size(width = 128.dp, height = 135.dp)    // [U1] tamaño en dp, independiente de la densidad
                    .testTag("splash_logo")                   // [U2] etiqueta para encontrarlo en las pruebas
            )
        }
    }
}
```

Archivo: [SplashScreen.kt, línea 94](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L94)

```kotlin
const val MIN_SPLASH_DURATION_MS = 2000L                     // [U2] constante en tiempo de compilación; L = Long
```

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-composable"></a>**`@Composable`** | U1 · Crea un diseño básico | Una función composable **describe** una parte de la interfaz; no devuelve nada, lo que "emite" se dibuja. Solo se puede llamar desde otro composable. Compose la vuelve a ejecutar (recomposición) cuando cambia algo que leyó. | [L44](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L44) |
| <a id="c-modifier"></a>**`Modifier`** | U1 · Crea un diseño básico | Cadena de ajustes (tamaño, relleno, fondo, clics). Cada uno envuelve al anterior, así que **el orden importa**: `padding` antes de `background` deja el margen sin pintar. Por convención, cada composable recibe `modifier: Modifier = Modifier` y lo aplica a su contenedor exterior. | [L62](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L62) |
| <a id="c-dp"></a>**`dp`** | U1 · Crea un diseño básico | Píxeles independientes de la densidad: `px = dp × (dpi / 160)`. El logo mide lo mismo **físicamente** en un teléfono barato y en uno de gama alta. Para textos se usa `sp`, que además respeta el tamaño de letra del usuario. | [L78](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L78) |
| <a id="c-recursos"></a>**`painterResource` / `stringResource`** | U1 · Crea un diseño básico | Leen recursos de `res/`: imágenes de `drawable` y textos de `strings.xml`. Los textos nunca se escriben en el código, para poder traducir la app. | [L74](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L74) |
| <a id="c-box"></a>**`Box`** y `contentAlignment` | U1 · Crea un diseño básico | `Box` pone a sus hijos uno **encima** del otro; `contentAlignment` decide dónde quedan. `Column` los apila hacia abajo y `Row`, de lado. | [L66](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L66) |
| <a id="c-nulos"></a>**Seguridad de nulos (`?`)** | U2 · Aspectos básicos de Kotlin | `SplashDestination?` puede ser `null`. Kotlin obliga a comprobarlo (`destination != null`) antes de usarlo, y así evita el `NullPointerException`. | [L46](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L46) |
| <a id="c-lambda"></a>**Tipos función (lambdas)** | U2 · Aspectos básicos de Kotlin | `(SplashDestination) -> Unit` es una función que recibe un destino y no devuelve nada. La pantalla no sabe navegar: **avisa** con `onNavigate` y quien la usa decide. | [L47](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L47) |
| <a id="c-remember"></a>**`remember` + `mutableStateOf`** | U2 · Interactuar con la IU y el estado | `mutableStateOf` crea un valor **observable**: si cambia, Compose recompone a quien lo lee. `remember` lo guarda entre recomposiciones; sin `remember` volvería a `false` cada vez. `by` permite escribir `minimumTimeElapsed = true` en lugar de `.value = true`. | [L52](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L52) |
| <a id="c-launchedeffect"></a>**`LaunchedEffect`** | U2 · Interactuar con la IU y el estado (y U5, corrutinas) | Lanza una **corrutina** cuando el composable entra en pantalla y la cancela cuando sale. Con `Unit` como clave corre una sola vez; con claves (`minimumTimeElapsed, destination`) se cancela y se relanza cada vez que una cambia. Es la forma correcta de hacer trabajo "hacia afuera" (esperar, navegar), porque un composable se puede ejecutar muchas veces. | [L57](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L57) |
| <a id="c-delay"></a>**`delay`** | U5 · Obtén datos de Internet (corrutinas) | Suspende la corrutina el tiempo indicado **sin bloquear** el hilo principal: la pantalla sigue dibujándose. | [L54](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L54) |
| <a id="c-scaffold"></a>**`Scaffold`** | U3 · Cómo compilar apps fabulosas | Armazón de Material 3 con espacios para la barra superior, la inferior y el botón flotante. Le pasa al contenido el relleno (`paddingValues`) para no quedar debajo de esas barras. | [L61](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L61) |
| <a id="c-testtag"></a>**`testTag`** | U2 · Interactuar con la IU y el estado (pruebas) | Etiqueta invisible para encontrar el elemento en una prueba de UI (`onNodeWithTag("splash_logo")`). | [L79](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L79) |
| <a id="c-const"></a>**`const val`** | U2 · Aspectos básicos de Kotlin | Constante que el compilador reemplaza por su valor. `2000L`: la `L` la hace `Long`, el tipo que espera `delay`. | [L94](../../app/src/main/java/com/smartfood/app/feature/splash/SplashScreen.kt#L94) |

**Decisión de diseño.** El tiempo mínimo vive en la **pantalla** y no en la lógica: si la sesión se resuelve en
200 ms, el logo igual se ve 2 s. Las dos condiciones se combinan en un solo `LaunchedEffect` con dos claves.

---

## LegalDocumentScreen

Muestra los Términos y condiciones o la Política de privacidad: barra con "Volver", aviso de vista previa con la
versión y las secciones desplazables. **No tiene ViewModel**: no tiene estado ni eventos propios. Spec 005, HU-LEG-01.

**Qué recibe**

| Parámetro | Tipo | Para qué |
|---|---|---|
| `type` | `LegalDocumentType` | `TERMS` o `PRIVACY` |
| `onBack` | `() -> Unit` | La flecha "Volver" |
| `modifier` | `Modifier` | Ajustes desde quien la llama |

### Código comentado

Archivo: [LegalDocumentScreen.kt, líneas 56–124](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L56-L124)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)                      // acepta usar una API de Material 3 marcada experimental
@Composable                                                   // [U1] composable
fun LegalDocumentScreen(
    type: LegalDocumentType,                                  // [U2] enum: el documento a mostrar
    onBack: () -> Unit,                                       // [U2] lambda sin parámetros para "Volver"
    modifier: Modifier = Modifier
) {
    val content = legalContent(type)                          // [U2] when sobre el enum → título, intro y secciones
    val spacing = MaterialTheme.spacing                       // [U3] espaciados propios del tema (CompositionLocal)

    SystemBarEffect(isLightBackground = !LocalIsDarkTheme.current) // íconos de la barra según el modo claro u oscuro

    Scaffold(                                                 // [U3] armazón de Material
        modifier = modifier.fillMaxSize(),
        // En este design system `background` es el verde del encabezado de marca; el panel claro es `surface`.
        containerColor = MaterialTheme.colorScheme.surface,   // [U3] color por ROL del tema, no un Color fijo
        topBar = {                                            // [U3] slot: el Scaffold recibe la barra como composable
            CenterAlignedTopAppBar(                           // [U3] barra superior de Material con el título centrado
                title = {
                    Text(
                        text = stringResource(id = content.title),   // [U1] texto de strings_legal.xml
                        style = MaterialTheme.typography.titleLarge, // [U3] tipografía del tema (Poppins)
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {            // [U2] botón con área de toque de 48 dp; avisa con onBack
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, // flecha que se voltea en idiomas de derecha a izquierda
                            contentDescription = stringResource(id = R.string.legal_back) // [U1] TalkBack: "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,      // [U3] colores por rol
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->                                       // relleno para no quedar debajo de la barra superior
        LazyColumn(                                           // [U3] lista perezosa: solo compone lo visible
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(                   // [U3] margen DENTRO del área que se desplaza
                start = spacing.large,
                end = spacing.large,
                top = innerPadding.calculateTopPadding() + spacing.small,        // debajo de la barra + 8 dp
                bottom = innerPadding.calculateBottomPadding() + spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.large), // [U3] mismo espacio entre todos los elementos
            horizontalAlignment = Alignment.CenterHorizontally         // [U1] centra cada elemento
        ) {
            if (LegalVersion.IS_PREVIEW) {                    // [U2] if dentro del DSL: el aviso solo existe en vista previa
                item { PreviewNotice(modifier = Modifier.textColumn()) } // [U3] item: UN elemento de la lista
            }
            item {
                Text(
                    text = stringResource(id = content.intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.textColumn()          // función de extensión propia (ver abajo)
                )
            }
            items(content.sections) { section ->              // [U3] items: un elemento por cada sección de la lista
                LegalSectionBlock(section = section, modifier = Modifier.textColumn())
            }
        }
    }
}
```

Archivo: [LegalDocumentScreen.kt, líneas 127–130](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L127-L130)

```kotlin
private val MaxTextWidth = 600.dp                             // [U1] ancho máximo del texto

private fun Modifier.textColumn(): Modifier = widthIn(max = MaxTextWidth).fillMaxWidth()  // [U3] función de extensión de Modifier
```

Archivo: [LegalDocumentScreen.kt, líneas 157–172](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L157-L172)

```kotlin
private fun LegalSectionBlock(section: LegalSection, modifier: Modifier = Modifier) {  // [U1] composable privado y reutilizable
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) { // [U1] Column: título arriba, cuerpo abajo
        Text(
            text = stringResource(id = section.title),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, lineHeight = 25.5.sp), // [U1] sp: escala con la letra del usuario
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }       // TalkBack lo anuncia como encabezado (CA-LEG-01-06)
        )
        Text(
            text = stringResource(id = section.body),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal), // [U2] copy(): estilo con un cambio
            // onSurfaceVariant es gris claro, pensado para subtítulos; en párrafos largos el contraste es bajo.
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)  // [U3] color del tema con 80 % de opacidad
        )
    }
}
```

`// …` Omitido: `PreviewNotice` (una píldora con dos `Text`) y los dos `@Preview` (claro y oscuro):
[LegalDocumentScreen.kt, líneas 134–188](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L134-L188).

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-lazycolumn"></a>**`LazyColumn`, `item`, `items`** | U3 · Cómo crear una lista desplazable | `LazyColumn` compone y dibuja **solo** lo visible, más algunos elementos que precarga. Su contenido es un DSL: `item { }` agrega un elemento y `items(lista) { }`, uno por cada dato. Aquí cada sección del documento es un elemento, así un documento largo no se compone completo de una vez. | [L97](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L97) |
| <a id="c-contentpadding"></a>**`contentPadding`** | U3 · Cómo crear una lista desplazable | Margen **dentro** del área desplazable: el primer y el último elemento se separan del borde, pero la lista se desplaza hasta el borde. Un `Modifier.padding` recortaría el área de desplazamiento. | [L99](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L99) |
| <a id="c-arrangement"></a>**`Arrangement.spacedBy`** | U1 · Crea un diseño básico | El mismo espacio entre todos los hijos, sin un `Spacer` entre cada uno. | [L105](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L105) |
| <a id="c-tema"></a>**`MaterialTheme.colorScheme` / `typography`** | U3 · Cómo compilar apps fabulosas | Los colores y fuentes se leen por **rol** (`surface`, `primary`, `titleLarge`). En modo oscuro el tema entrega otros valores y la pantalla no cambia nada. | [L71](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L71) |
| <a id="c-topappbar"></a>**`CenterAlignedTopAppBar`** + `IconButton` | U3 · Cómo compilar apps fabulosas | Barra superior de Material 3 con un slot para el título y otro para el ícono de navegación. `IconButton` garantiza un área de toque de 48 dp aunque el ícono sea de 24. | [L73](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L73) |
| <a id="c-slot"></a>**Slots (`topBar = { … }`)** | U3 · Cómo compilar apps fabulosas | Un parámetro que recibe **un composable**. El `Scaffold` decide dónde va la barra y la pantalla decide qué barra es. | [L72](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L72) |
| <a id="c-extension"></a>**Funciones de extensión** | U3 · Más aspectos básicos de Kotlin | `Modifier.textColumn()` agrega una función a una clase que no es nuestra. Así la combinación "ancho máximo + ocupar el ancho" se escribe una vez y se usa en tres lugares. | [L130](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L130) |
| <a id="c-sp"></a>**`sp`** | U1 · Crea un diseño básico | Como `dp`, pero para textos: además escala con el tamaño de letra que la persona elige en Ajustes. | [L161](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L161) |
| <a id="c-copy"></a>**`copy()` de una data class** | U2 · Aspectos básicos de Kotlin | `TextStyle` y `Color` son clases inmutables: `copy(fontSize = 17.sp)` crea una copia con un solo cambio, sin tocar el estilo original del tema. | [L161](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L161) |
| <a id="c-heading"></a>**`semantics { heading() }`** | U2 · Interactuar con la IU y el estado (accesibilidad) | Marca el texto como encabezado en el árbol de semántica: TalkBack lo anuncia así y deja saltar entre secciones. Las pruebas de UI usan ese mismo árbol. | [L163](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L163) |
| <a id="c-preview"></a>**`@Preview`** | U1 · Crea un diseño básico | Android Studio dibuja la pantalla sin ejecutar la app. Aquí hay dos: modo claro (Términos) y oscuro (Privacidad), posibles porque la pantalla recibe todo por parámetros. | [L174](../../app/src/main/java/com/smartfood/app/feature/legal/ui/LegalDocumentScreen.kt#L174) |

**Decisión de diseño.** El texto vive en `strings_legal.xml` y `legalContent(type)` es el único lugar que relaciona el
tipo con sus recursos. La pantalla se registra en la raíz de la navegación para abrirla desde el registro, sin sesión.
