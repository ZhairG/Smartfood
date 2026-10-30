# Pantallas de SmartFood, línea por línea

Explicación del código de cada `*Screen.kt` de la app, comentado línea por línea y relacionado con las unidades del
curso [Aspectos básicos de Android con Compose](https://developer.android.com/courses/android-basics-compose/course?hl=es-419).

Esta guía cubre **solo las pantallas** (la interfaz). No explica los ViewModels ni la lógica: cada pantalla recibe un
`state` y avisa con `onEvent`, y aquí se explica qué hace con ellos. Para los conceptos generales de Compose está
[`como-funcionan-las-pantallas.md`](../como-funcionan-las-pantallas.md).

## Cómo leer cada archivo

- **Código comentado:** cada línea lleva un comentario `//` con lo que hace. Si usa un concepto del curso, el comentario
  empieza con su unidad: `[U3] LazyColumn: …`.
- **`Archivo: [Nombre.kt, líneas X–Y]`** encima de cada bloque abre el código real en esas líneas.
- **`// …`** marca lo que se omitió (estilos repetidos, `@Preview`) y lleva su enlace.
- **Conceptos del curso:** debajo de cada pantalla, una tabla explica cada concepto (qué es, cómo funciona y por qué se
  usó ahí). Un concepto se explica a fondo la primera vez que aparece; después se enlaza a esa explicación.
- **Qué le da la Route:** cuando una pantalla necesita APIs de Android (permisos, cámara, "atrás"), esa parte vive en un
  `XRoute.kt` y aquí se resume en 2 o 3 líneas.

## Módulos

| Archivo | Pantallas |
|---|---|
| [01 · Arranque y legal](01-arranque-y-legal.md) | [`SplashScreen`](01-arranque-y-legal.md#splashscreen), [`LegalDocumentScreen`](01-arranque-y-legal.md#legaldocumentscreen) |
| [02 · Autenticación](02-autenticacion.md) | [`LoginScreen`](02-autenticacion.md#loginscreen), [`RegisterScreen`](02-autenticacion.md#registerscreen), [`PasswordRecoveryScreen`](02-autenticacion.md#passwordrecoveryscreen) |
| [03 · Inventario](03-inventario.md) | [`InventoryScreen`](03-inventario.md#inventoryscreen), [`InventorySearchScreen`](03-inventario.md#inventorysearchscreen), [`ProductEditorScreen`](03-inventario.md#producteditorscreen), [`ProductCameraScreen`](03-inventario.md#productcamerascreen) |
| [04 · Perfil](04-perfil.md) | [`ProfileScreen`](04-perfil.md#profilescreen), [`EditProfileScreen`](04-perfil.md#editprofilescreen), [`NotificationSettingsScreen`](04-perfil.md#notificationsettingsscreen), [`ChangePasswordScreen`](04-perfil.md#changepasswordscreen), [`FoodPreferencesScreen`](04-perfil.md#foodpreferencesscreen) |
| [05 · Recetas](05-recetas.md) | [`RecipesDiscoverScreen`](05-recetas.md#recipesdiscoverscreen), [`RecipeDetailScreen`](05-recetas.md#recipedetailscreen), [`AiFlowScreen`](05-recetas.md#aiflowscreen), [`AiHelpScreen`](05-recetas.md#aihelpscreen), [`CookingScreens`](05-recetas.md#cookingscreens) |

## Mapa de conceptos por unidad del curso

Cada concepto enlaza a su explicación. Las unidades 6 (Persistencia de datos) y 7 (WorkManager) no aparecen porque se
usan en la capa de datos, no en las pantallas: DataStore guarda el modo de color y "Recuérdame", y WorkManager sube las
fotos y programa el aviso diario.

### Unidad 1 · Tu primera app para Android

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [`@Composable`](01-arranque-y-legal.md#c-composable) | Crea un diseño básico | 01 · Arranque y legal |
| [`Modifier`](01-arranque-y-legal.md#c-modifier) | Crea un diseño básico | 01 · Arranque y legal |
| [`dp`](01-arranque-y-legal.md#c-dp) | Crea un diseño básico | 01 · Arranque y legal |
| [`painterResource` / `stringResource`](01-arranque-y-legal.md#c-recursos) | Crea un diseño básico | 01 · Arranque y legal |
| [`Box` y `contentAlignment`](01-arranque-y-legal.md#c-box) | Crea un diseño básico | 01 · Arranque y legal |
| [`Arrangement.spacedBy`](01-arranque-y-legal.md#c-arrangement) | Crea un diseño básico | 01 · Arranque y legal |
| [`sp`](01-arranque-y-legal.md#c-sp) | Crea un diseño básico | 01 · Arranque y legal |
| [`@Preview`](01-arranque-y-legal.md#c-preview) | Crea un diseño básico | 01 · Arranque y legal |
| [`Row`, `Arrangement.SpaceBetween`, `Alignment`](02-autenticacion.md#c-row) | Crea un diseño básico | 02 · Autenticación |
| [`Modifier.weight` y `Spacer`](02-autenticacion.md#c-weight) | Crea un diseño básico | 02 · Autenticación |
| [Textos con argumentos y plurales](02-autenticacion.md#c-format) | Crea un diseño básico | 02 · Autenticación |
| [`maxLines` + `TextOverflow.Ellipsis`](03-inventario.md#c-ellipsis) | Crea un diseño básico | 03 · Inventario |
| [Capas con `Box`](03-inventario.md#c-overlay) | Crea un diseño básico | 03 · Inventario |
| [`fillMaxWidth(fracción)`](05-recetas.md#c-fraction) | Crea un diseño básico | 05 · Recetas |

### Unidad 2 · Compila la IU de una app

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [Seguridad de nulos (`?`)](01-arranque-y-legal.md#c-nulos) | Aspectos básicos de Kotlin | 01 · Arranque y legal |
| [Tipos función (lambdas)](01-arranque-y-legal.md#c-lambda) | Aspectos básicos de Kotlin | 01 · Arranque y legal |
| [`remember` + `mutableStateOf`](01-arranque-y-legal.md#c-remember) | Interactuar con la IU y el estado | 01 · Arranque y legal |
| [`LaunchedEffect`](01-arranque-y-legal.md#c-launchedeffect) | Interactuar con la IU y el estado (y U5, corrutinas) | 01 · Arranque y legal |
| [`testTag`](01-arranque-y-legal.md#c-testtag) | Interactuar con la IU y el estado (pruebas) | 01 · Arranque y legal |
| [`const val`](01-arranque-y-legal.md#c-const) | Aspectos básicos de Kotlin | 01 · Arranque y legal |
| [`copy()` de una data class](01-arranque-y-legal.md#c-copy) | Aspectos básicos de Kotlin | 01 · Arranque y legal |
| [`semantics { heading() }`](01-arranque-y-legal.md#c-heading) | Interactuar con la IU y el estado (accesibilidad) | 01 · Arranque y legal |
| [Elevación de estado (`value` + `onValueChange`)](02-autenticacion.md#c-hoisting) | Interactuar con la IU y el estado | 02 · Autenticación |
| [`KeyboardOptions` y `KeyboardActions`](02-autenticacion.md#c-keyboard) | Interactuar con la IU y el estado | 02 · Autenticación |
| [`remember(onEvent) { { … } }`](02-autenticacion.md#c-remember-key) | Interactuar con la IU y el estado | 02 · Autenticación |
| [`?.let`](02-autenticacion.md#c-letnull) | Aspectos básicos de Kotlin | 02 · Autenticación |
| [Botones (`onClick`, `enabled`)](02-autenticacion.md#c-button) | Cómo agregar un botón a una app | 02 · Autenticación |
| [`when` exhaustivo](02-autenticacion.md#c-when) | Aspectos básicos de Kotlin | 02 · Autenticación |
| [`if` como expresión e `if` en la composición](02-autenticacion.md#c-if-expr) | Aspectos básicos de Kotlin | 02 · Autenticación |
| [Smart cast con copia local](03-inventario.md#c-smartcast) | Aspectos básicos de Kotlin | 03 · Inventario |
| [`BasicTextField` + `decorationBox`](03-inventario.md#c-basictextfield) | Interactuar con la IU y el estado | 03 · Inventario |
| [Parámetros con valor por defecto](03-inventario.md#c-default-params) | Aspectos básicos de Kotlin | 03 · Inventario |
| [`it` en las lambdas](03-inventario.md#c-it) | Aspectos básicos de Kotlin | 03 · Inventario |
| [`verticalScroll(rememberScrollState())`](03-inventario.md#c-verticalscroll) | Interactuar con la IU y el estado | 03 · Inventario |
| [`clickable` y `matchParentSize`](03-inventario.md#c-clickable) | Cómo agregar un botón a una app | 03 · Inventario |
| [`liveRegion`](03-inventario.md#c-liveregion) | Interactuar con la IU y el estado (accesibilidad) | 03 · Inventario |
| [Operador Elvis `?:` y referencia `::File`](04-perfil.md#c-elvis) | Aspectos básicos de Kotlin | 04 · Perfil |
| [`takeIf`](04-perfil.md#c-takeif) | Aspectos básicos de Kotlin | 04 · Perfil |
| [`Switch` con estado elevado](04-perfil.md#c-switch) | Interactuar con la IU y el estado | 04 · Perfil |
| [`semantics { disabled() }`](05-recetas.md#c-disabled) | Interactuar con la IU y el estado (accesibilidad) | 05 · Recetas |
| [`Slider`](05-recetas.md#c-slider) | Interactuar con la IU y el estado | 05 · Recetas |
| [`?: return`](05-recetas.md#c-elvis-return) | Aspectos básicos de Kotlin | 05 · Recetas |
| [`onClickLabel` y `stateDescription`](05-recetas.md#c-statedesc) | Interactuar con la IU y el estado (accesibilidad) | 05 · Recetas |

### Unidad 3 · Cómo mostrar listas y usar Material Design

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [`Scaffold`](01-arranque-y-legal.md#c-scaffold) | Cómo compilar apps fabulosas | 01 · Arranque y legal |
| [`LazyColumn`, `item`, `items`](01-arranque-y-legal.md#c-lazycolumn) | Cómo crear una lista desplazable | 01 · Arranque y legal |
| [`contentPadding`](01-arranque-y-legal.md#c-contentpadding) | Cómo crear una lista desplazable | 01 · Arranque y legal |
| [`MaterialTheme.colorScheme` / `typography`](01-arranque-y-legal.md#c-tema) | Cómo compilar apps fabulosas | 01 · Arranque y legal |
| [`CenterAlignedTopAppBar` + `IconButton`](01-arranque-y-legal.md#c-topappbar) | Cómo compilar apps fabulosas | 01 · Arranque y legal |
| [Slots (`topBar = { … }`)](01-arranque-y-legal.md#c-slot) | Cómo compilar apps fabulosas | 01 · Arranque y legal |
| [Funciones de extensión](01-arranque-y-legal.md#c-extension) | Más aspectos básicos de Kotlin | 01 · Arranque y legal |
| [`LocalFocusManager` (CompositionLocal)](02-autenticacion.md#c-compositionlocal) | Cómo compilar apps fabulosas | 02 · Autenticación |
| [Receptor `ColumnScope`](02-autenticacion.md#c-scope) | Más aspectos básicos de Kotlin | 02 · Autenticación |
| [`AnimatedContent`](02-autenticacion.md#c-animatedcontent) | Cómo compilar apps fabulosas | 02 · Autenticación |
| [`key` en `item` e `items`](03-inventario.md#c-key) | Cómo crear una lista desplazable | 03 · Inventario |
| [`FloatingActionButton` dentro de un `Box`](03-inventario.md#c-fab) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`Modifier.blur`](03-inventario.md#c-blur) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`CircularProgressIndicator`](03-inventario.md#c-progress) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`CompositionLocalProvider`](03-inventario.md#c-provider) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`Canvas` y `drawLine`](03-inventario.md#c-canvas) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`Modifier.clip`](03-inventario.md#c-clip) | Cómo compilar apps fabulosas | 03 · Inventario |
| [`as?` (conversión segura)](04-perfil.md#c-ascast) | Más aspectos básicos de Kotlin | 04 · Perfil |
| [`Pair`, `to` y desestructuración](04-perfil.md#c-pair) | Más aspectos básicos de Kotlin | 04 · Perfil |
| [`enum.entries`](04-perfil.md#c-entries) | Más aspectos básicos de Kotlin | 04 · Perfil |
| [Slot `content: @Composable () -> Unit`](04-perfil.md#c-slot-content) | Cómo compilar apps fabulosas | 04 · Perfil |
| [`LazyVerticalGrid` y `GridCells.Fixed`](05-recetas.md#c-grid) | Cómo crear una lista desplazable | 05 · Recetas |
| [`span = { GridItemSpan(maxLineSpan) }`](05-recetas.md#c-span) | Cómo crear una lista desplazable | 05 · Recetas |
| [`Modifier.then`](05-recetas.md#c-then) | Cómo compilar apps fabulosas | 05 · Recetas |
| [`aspectRatio`](05-recetas.md#c-aspect) | Cómo crear una lista desplazable | 05 · Recetas |
| [`Modifier.shadow`](05-recetas.md#c-shadow) | Cómo compilar apps fabulosas | 05 · Recetas |
| [`Modifier.alpha`](05-recetas.md#c-alpha) | Cómo compilar apps fabulosas | 05 · Recetas |
| [`map`, `forEach`, `joinToString`](05-recetas.md#c-colecciones) | Más aspectos básicos de Kotlin | 05 · Recetas |
| [`LocalIsDarkTheme`](05-recetas.md#c-darktheme) | Cómo compilar apps fabulosas | 05 · Recetas |

### Unidad 4 · Navegación y arquitectura de la app

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [Pantalla sin estado y flujo unidireccional (UDF)](02-autenticacion.md#c-udf) | Componentes de la arquitectura | 02 · Autenticación |
| [Los cuatro estados de una pantalla](03-inventario.md#c-estados) | Componentes de la arquitectura | 03 · Inventario |
| [`windowInsetsPadding(WindowInsets.statusBars)`](03-inventario.md#c-insets) | Cómo adaptar tu app a diferentes tamaños de pantalla | 03 · Inventario |
| [`BoxWithConstraints`](03-inventario.md#c-boxwithconstraints) | Cómo adaptar tu app a diferentes tamaños de pantalla | 03 · Inventario |
| [`imePadding()` y `navigationBarsPadding()`](03-inventario.md#c-ime) | Cómo adaptar tu app a diferentes tamaños de pantalla | 03 · Inventario |
| [Diálogos controlados por el estado](03-inventario.md#c-dialogs) | Componentes de la arquitectura | 03 · Inventario |
| [Escalar con `BoxWithConstraints`](04-perfil.md#c-escalado) | Cómo adaptar tu app a diferentes tamaños de pantalla | 04 · Perfil |
| [`FlowRow`](04-perfil.md#c-flowrow) | Cómo adaptar tu app a diferentes tamaños de pantalla | 04 · Perfil |
| [`BackHandler(enabled = …)`](05-recetas.md#c-backhandler) | Navigation en Jetpack Compose | 05 · Recetas |
| [Varios pasos en una sola pantalla](05-recetas.md#c-pasos) | Componentes de la arquitectura | 05 · Recetas |

### Unidad 5 · Cómo conectarse a Internet

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [`delay`](01-arranque-y-legal.md#c-delay) | Obtén datos de Internet (corrutinas) | 01 · Arranque y legal |
| [`AsyncImage` (Coil)](04-perfil.md#c-asyncimage) | Cómo cargar y mostrar imágenes de Internet | 04 · Perfil |

### Unidad 8 · Views y Compose

| Concepto | Ruta de aprendizaje | Dónde se explica |
|---|---|---|
| [Views dentro de Compose (slot `preview`)](03-inventario.md#c-androidview) | Views de Android en Compose | 03 · Inventario |
