# 03 · Inventario

Pantallas: [Mi Inventario](#inventoryscreen), [Buscar](#inventorysearchscreen), [Añadir o editar producto](#producteditorscreen)
y [Cámara](#productcamerascreen). Índice general y mapa de conceptos: [README](README.md). Los conceptos ya explicados se
enlazan a [01](01-arranque-y-legal.md) y [02](02-autenticacion.md).

---

## InventoryScreen

La pestaña "Mi Inventario": barra con la lupa, los productos agrupados por vencimiento, los estados de carga, vacío y
error, y el botón "+". Spec 006, HU-INV-01; Figma `476:3095`. La barra inferior la pone `MainTabScaffold` desde la
navegación.

**Qué recibe.** `state: InventoryUiState` (lee `isLoading`, `error`, `isEmpty` y `sections`), `onEvent` (`SearchClicked`,
`AddClicked`, `EditClicked(id)`, `RetryClicked`) y `modifier`.

### Código comentado

Archivo: [InventoryScreen.kt, líneas 67–100](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L67-L100)

```kotlin
fun InventoryScreen(
    state: InventoryUiState,
    onEvent: (InventoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {               // [U1] barra arriba y contenido debajo
        InventoryTopBar(onSearchClicked = { onEvent(InventoryEvent.SearchClicked) }) // solo avisa: no navega
        Box(modifier = Modifier.weight(1f)) {                 // [U1] weight: el Box ocupa todo el alto restante
            val error = state.error                           // [U2] copia local: permite el smart cast de abajo
            when {                                            // [U2] when sin argumento: la primera condición verdadera gana
                // Skeleton Screen en lugar de un indicador suelto (#45).
                state.isLoading -> InventorySkeleton()        // 1. cargando: esqueleto con brillo animado
                error != null -> ErrorState(error = error, onRetry = { onEvent(InventoryEvent.RetryClicked) }) // 2. error
                state.isEmpty -> EmptyState(onAddClicked = { onEvent(InventoryEvent.AddClicked) })            // 3. vacío
                else -> InventoryList(sections = state.sections, onEvent = onEvent)                          // 4. contenido
            }
            FloatingActionButton(                             // [U3] botón flotante de Material
                onClick = { onEvent(InventoryEvent.AddClicked) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)               // [U1] align del BoxScope: abajo a la derecha, ENCIMA de la lista
                    .padding(MaterialTheme.spacing.large),
                containerColor = MaterialTheme.colorScheme.primary,   // [U3] colores por rol
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape                           // [U3] forma circular
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_add),          // ícono exportado del Figma
                    contentDescription = stringResource(id = R.string.cd_add_button), // TalkBack: "Agregar"
                    modifier = Modifier.size(FigmaIconSize)
                )
            }
        }
    }
}
```

Archivo: [InventoryScreen.kt, líneas 107–132](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L107-L132)

```kotlin
private fun InventoryTopBar(
    onSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)    // fondo verde oscuro
            .windowInsetsPadding(WindowInsets.statusBars)     // deja libre el alto de la barra de estado (borde a borde)
            .height(TopBarHeight)
    ) {
        Text(
            text = stringResource(id = R.string.inventory_title),
            color = MaterialTheme.colorScheme.onPrimary,      // [U3] "on" + rol: color legible sobre primary
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = TopBarTitleSize,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,                                     // una sola línea…
            overflow = TextOverflow.Ellipsis,                 // …y "…" si no cabe
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TopBarTitleSidePadding) // margen para no cruzarse con la lupa
                .semantics { heading() }                      // encabezado para TalkBack
        )
```

Archivo: [InventoryScreen.kt, líneas 154–183](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L154-L183)

```kotlin
private fun InventoryList(
    sections: List<InventorySection>,
    onEvent: (InventoryEvent) -> Unit,
) {
    LazyColumn(                                               // [U3] lista perezosa
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(                       // [U3] margen dentro del área desplazable
            start = MaterialTheme.spacing.extraLarge,
            end = MaterialTheme.spacing.extraLarge,
            top = MaterialTheme.spacing.large,
            bottom = FloatingActionButtonSpace                // espacio para que el "+" no tape la última fila
        )
    ) {
        sections.forEachIndexed { index, section ->           // [U3] recorre las secciones con su posición
            item(key = "section_${section.status.name}") {    // [U3] item con key: el encabezado de la sección
                // Figma 476:3095: unos 42 px entre el final de una sección y el encabezado de la siguiente.
                ExpirySectionHeader(
                    status = section.status,
                    modifier = if (index > 0) Modifier.padding(top = SectionGap) else Modifier // la primera no lleva margen
                )
            }
            items(items = section.items, key = { item -> item.id }) { item -> // [U3] una fila por producto, key = id
                InventoryItemRow(
                    item = item,
                    status = section.status,
                    onEditClicked = { id -> onEvent(InventoryEvent.EditClicked(id)) } // la fila avisa con el id
                )
            }
        }
    }
}
```

`// …` Omitidos: el ícono de la lupa de `InventoryTopBar`, `EmptyState` y `ErrorState` (un `Column` centrado con dos
`Text` y un botón), las medidas y los `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-estados"></a>**Los cuatro estados de una pantalla** | U4 · Componentes de la arquitectura | Una pantalla con datos siempre está en uno de cuatro estados: **cargando, error, vacío o contenido**. El `when` sin argumento evalúa las condiciones en orden y dibuja solo la rama que gana, así la pantalla nunca queda en blanco. | [L76](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L76) |
| <a id="c-smartcast"></a>**Smart cast con copia local** | U2 · Aspectos básicos de Kotlin | `state.error` es una propiedad de otro objeto y Kotlin no puede garantizar que siga siendo no nula al usarla. Al copiarla a `val error`, después de `error != null` el compilador la trata como `InventoryError`. | [L75](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L75) |
| <a id="c-key"></a>**`key` en `item` e `items`** | U3 · Cómo crear una lista desplazable | La `key` identifica cada elemento. Si llega un producto nuevo arriba, o uno cambia de sección, Compose **mueve** su fila en lugar de redibujar todo, y la lista conserva la posición del scroll. Sin `key`, las filas se identifican por posición. | [L175](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L175) |
| <a id="c-fab"></a>**`FloatingActionButton` dentro de un `Box`** | U3 · Cómo compilar apps fabulosas | Botón redondo para la acción principal. Va en un `Box` junto con la lista: con `align(Alignment.BottomEnd)` queda **encima** de ella, abajo a la derecha. | [L83](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L83) |
| <a id="c-insets"></a>**`windowInsetsPadding(WindowInsets.statusBars)`** | U4 · Cómo adaptar tu app a diferentes tamaños de pantalla | Con la app borde a borde, la barra verde se dibuja **detrás** de la barra de estado. Este relleno empuja el título hacia abajo justo lo que mide la barra de estado en ese teléfono. | [L115](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L115) |
| <a id="c-ellipsis"></a>**`maxLines` + `TextOverflow.Ellipsis`** | U1 · Crea un diseño básico | Limita el texto a una línea y termina con "…" si no cabe, para que un título largo no rompa la barra. | [L127](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L127) |
| **`forEachIndexed` dentro del DSL** | U3 · Más aspectos básicos de Kotlin | Dentro de `LazyColumn` se puede usar Kotlin normal (bucles, `if`) para decidir **qué elementos** agregar. | [L167](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/list/InventoryScreen.kt#L167) |

---

## InventorySearchScreen

Busca por texto y por categoría a la vez, con la tarjeta "Usa tu voz" y el aviso de voz encima. Spec 006, HU-INV-06 y
HU-INV-07; Figma `476:3190`.

**Qué recibe.** `state: InventorySearchUiState` (lee `voice`, `query`, `isVoiceAvailable`, `selectedCategory`,
`isLoading`, `isEmptyResult` y `results`), `onEvent`, `showPermissionRationale` (aviso de micrófono negado),
`onOpenSettings` y `onDismissPermissionRationale`.

### Código comentado

Archivo: [InventorySearchScreen.kt, líneas 75–160](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L75-L160)

```kotlin
fun InventorySearchScreen(
    state: InventorySearchUiState,
    onEvent: (InventorySearchEvent) -> Unit,
    modifier: Modifier = Modifier,
    showPermissionRationale: Boolean = false,                 // [U2] parámetros con valor por defecto
    onOpenSettings: () -> Unit = {},
    onDismissPermissionRationale: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {                  // [U1] Box: el aviso de voz se dibuja ENCIMA del contenido
        // Con el aviso de voz abierto, la búsqueda se desenfoca detrás (Figma 476:4507; API 31+).
        val blurRadius = if (state.voice == VoiceUiState.Hidden) 0.dp else VoiceBlur // [U2] if como expresión
        Column(modifier = Modifier.fillMaxSize().blur(blurRadius)) { // desenfoca todo el contenido (Android 12+)
            SmartFoodTopBar(                                  // barra interna con la flecha atrás
                title = stringResource(id = R.string.inventory_title),
                onBack = { onEvent(InventorySearchEvent.BackClicked) }
            )

            // Figma 476:3190: la barra, la tarjeta de voz y los filtros van separados unos 24 dp.
            Column(
                verticalArrangement = Arrangement.spacedBy(BlockGap), // [U1] mismo espacio entre los bloques
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = SidePadding)
                    .padding(top = MaterialTheme.spacing.medium)
            ) {
                SearchField(                                  // barra de texto (ver abajo)
                    query = state.query,
                    onQueryChanged = { onEvent(InventorySearchEvent.QueryChanged(it)) }, // it: el único parámetro
                    onClearQuery = { onEvent(InventorySearchEvent.ClearQuery) }
                )

                if (state.isVoiceAvailable) {                 // sin reconocimiento de voz en el teléfono, no hay tarjeta
                    VoiceSearchCard(onClick = { onEvent(InventorySearchEvent.VoiceClicked) })
                }

                CategoryChips(                                // filtros "Todos", "Frutas", "Lácteos"…
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { onEvent(InventorySearchEvent.CategorySelected(it)) }
                )

                // Todo el ancho: así el texto de "sin resultados" queda centrado, como en el Figma (476:3551).
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {                                    // cargando, sin resultados o lista
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // [U3] indicador de carga

                        // Figma 476:3551: Poppins Bold de 15 px en verde oscuro, centrado en el espacio libre.
                        state.isEmptyResult -> Text(
                            text = stringResource(id = R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = MaterialTheme.spacing.medium)
                        )

                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {        // [U3] resultados
                            items(items = state.results, key = { it.item.id }) { result -> // [U3] key = id del producto
                                SearchResultRow(
                                    result = result,
                                    onClick = { id -> onEvent(InventorySearchEvent.ResultClicked(id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        VoiceSearchOverlay(                                   // aviso "Habla ahora", "…" o el error; Hidden no dibuja nada
            state = state.voice,
            onRetry = { onEvent(InventorySearchEvent.VoiceRetry) },
            onDismiss = { onEvent(InventorySearchEvent.VoiceDismissed) }
        )

        if (showPermissionRationale) {                        // aviso de micrófono negado, con "Abrir ajustes"
            PermissionRationale(
                icon = Icons.Outlined.Mic,                    // ícono de Material
                title = stringResource(id = R.string.search_voice_permission_title),
                message = stringResource(id = R.string.search_voice_permission_body),
                onOpenSettings = onOpenSettings,
                onDismiss = onDismissPermissionRationale
            )
        }
    }
}
```

Archivo: [InventorySearchScreen.kt, líneas 170–215](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L170-L215)

```kotlin
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val placeholder = stringResource(id = R.string.inventory_search_placeholder)
    // Figma 476:3203: Poppins Regular de 14 px.
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

    // Figma 476:3190: píldora gris sin borde, con la lupa a la izquierda y la "x" al escribir (CA-INV-06-07).
    BasicTextField(                                           // [U2] campo de texto SIN diseño: lo dibujamos nosotros
        value = query,                                        // [U2] elevación de estado
        onValueChange = onQueryChanged,
        singleLine = true,                                    // una sola línea: Enter no agrega saltos
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), // color del cursor
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },  // TalkBack lee "Buscar en tu inventario"
        decorationBox = { innerTextField ->                   // [U3] slot: todo lo que rodea al texto editable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SearchBarHeight)          // alto mínimo de 48 dp
                    // Figma 476:3200: relleno #F3F4F6 con borde #E5E7EB de 1 px y radio 24.
                    .background(color = SurfaceLight, shape = CircleShape)
                    .border(width = SearchBarBorder, color = GreyDivider, shape = CircleShape)
                    .padding(horizontal = MaterialTheme.spacing.medium)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_search_bar),
                    contentDescription = null,                // decorativa: TalkBack la salta
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SearchIconSize)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)                           // el texto ocupa todo lo que sobra
                        .padding(horizontal = MaterialTheme.spacing.small)
                ) {
                    if (query.isEmpty()) {                    // placeholder solo con la barra vacía
                        Text(text = placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()                          // aquí se dibuja el texto editable
```

`// …` Omitidos: la "x" para borrar (un `IconButton` que aparece con `if (query.isNotEmpty())`), las medidas y los
`@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-basictextfield"></a>**`BasicTextField` + `decorationBox`** | U2 · Interactuar con la IU y el estado | `TextField` de Material trae su propio diseño; `BasicTextField` solo maneja el texto. Con `decorationBox` se dibuja alrededor lo que pide el Figma (píldora, lupa, "x"), y `innerTextField()` indica dónde va el texto editable. | [L180](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L180) |
| <a id="c-default-params"></a>**Parámetros con valor por defecto** | U2 · Aspectos básicos de Kotlin | `showPermissionRationale: Boolean = false`: quien llama puede omitirlo. Así las pruebas y el `@Preview` llaman la pantalla solo con `state` y `onEvent`. | [L79](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L79) |
| <a id="c-it"></a>**`it` en las lambdas** | U2 · Aspectos básicos de Kotlin | En una lambda con **un** parámetro, Kotlin lo llama `it` si no se le pone nombre: `{ onEvent(QueryChanged(it)) }`. | [L102](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L102) |
| <a id="c-overlay"></a>**Capas con `Box`** | U1 · Crea un diseño básico | El contenido, el aviso de voz y el aviso de permiso son hijos del mismo `Box`: el último se dibuja encima. Cuando el aviso de voz está oculto (`Hidden`), no dibuja nada. | [L144](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L144) |
| <a id="c-blur"></a>**`Modifier.blur`** | U3 · Cómo compilar apps fabulosas | Desenfoca lo que está detrás del aviso de voz. Solo funciona desde Android 12; en versiones anteriores no tiene efecto y la app se ve igual de bien. | [L86](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L86) |
| <a id="c-progress"></a>**`CircularProgressIndicator`** | U3 · Cómo compilar apps fabulosas | Indicador de carga de Material. Aquí se usa en lugar del esqueleto, porque la búsqueda carga casi siempre al instante. | [L118](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchScreen.kt#L118) |

**Qué le da la Route.** [`InventorySearchRoute.kt`](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/search/InventorySearchRoute.kt) pide el permiso `RECORD_AUDIO` con
`rememberLauncherForActivityResult(RequestPermission())`, guarda en un `remember` si hay que mostrar el aviso de
permiso negado y abre los ajustes de la app.

---

## ProductEditorScreen

Formulario para añadir o editar un producto: foto, nombre, vencimiento, categoría, cantidad con unidad y precio. Spec
006, HU-INV-02 a HU-INV-05; Figma `476:3664` (añadir) y `476:3689` (editar).

**Qué recibe.** `state: ProductEditorUiState` (lee `mode`, `isSaving`, `isLoading`, `errorField`, `missingFields`,
`error`, `photo`, los campos, `isSaveEnabled`, `savingAction` y `dialog`) y `onEvent`.

### Código comentado

Archivo: [ProductEditorScreen.kt, líneas 87–145](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L87-L145)

```kotlin
fun ProductEditorScreen(
    state: ProductEditorUiState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    val isEdit = state.mode == EditorMode.EDIT                // [U2] el mismo formulario sirve para añadir y editar

    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Tocar fuera de un campo le quita el enfoque y cierra el teclado.
            .clearFocusOnTap(focusManager)                    // Modifier propio (extensión)
            // Figma 476:3664: fondo verde claro (#F0FDF4), que en el tema es `surface`; `background` es el verde
            // oscuro de autenticación.
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = if (isEdit) R.string.editor_title_edit else R.string.editor_title_add), // título según el modo
            onBack = { onEvent(ProductEditorEvent.BackClicked) },
            action = if (isEdit) {                            // [U3] slot opcional: solo al editar hay "Guardar" arriba
                {
                    TextButton(                               // [U3] botón de solo texto de Material
                        onClick = { onEvent(ProductEditorEvent.SaveClicked) },
                        enabled = !state.isSaving
                    ) {
                        Text(
                            text = stringResource(id = R.string.editor_save),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                null                                          // al añadir, la barra no tiene acción
            }
        )

        // El perfil de medidas se toma con el alto sin el teclado, para que los campos no cambien de tamaño al
        // abrirlo. El teclado se aplica por dentro, fuera del desplazable; Compose mantiene visible el campo
        // enfocado (spec 002).
        BoxWithConstraints(                                   // [U4] conoce el tamaño disponible (maxHeight)
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()                      // deja libre la barra de navegación del sistema
        ) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) { // [U3] medidas según el alto
                Box(modifier = Modifier.fillMaxSize().imePadding()) { // [U4] se achica cuando se abre el teclado
                    if (state.isLoading) {                    // cargando el producto a editar
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        EditorForm(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }

    EditorDialogs(state = state, onEvent = onEvent)           // hojas y diálogos, fuera del Column
}
```

Archivo: [ProductEditorScreen.kt, líneas 162–172](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L162-L172)

```kotlin
        verticalArrangement = Arrangement.spacedBy(metrics.fieldSpacing),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())            // [U2] el formulario se desplaza; recuerda la posición
            .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
    ) {
        EditorFields(state = state, onEvent = onEvent, errorField = errorField, missing = missing)
        // Figma 476:3664: el botón queda debajo de los campos, con el aviso de error encima (476:3688).
        Spacer(modifier = Modifier.height(metrics.sectionSpacing))
        EditorBottom(state = state, onEvent = onEvent, errorText = errorText)
    }
```

Un campo del formulario (el nombre):

Archivo: [ProductEditorScreen.kt, líneas 190–197](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L190-L197)

```kotlin
    EditorLineTextField(                                      // campo con línea inferior, como el Figma
        value = state.name,
        onValueChange = { onEvent(ProductEditorEvent.NameChanged(it)) },
        placeholder = stringResource(id = R.string.editor_name_placeholder),
        accessibilityLabel = stringResource(id = R.string.editor_name_label), // lo que lee TalkBack
        icon = { FieldIcon(R.drawable.ic_figma_cart) },       // [U3] slot del ícono
        isError = errorField == InventoryField.NAME || InventoryField.NAME in missing // [U2] in: ¿está en el conjunto?
    )
```

El botón gris que **sí** responde al toque:

Archivo: [ProductEditorScreen.kt, líneas 258–279](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L258-L279)

```kotlin
    if (state.mode == EditorMode.ADD) {
        // El botón se ve gris mientras falte algo, pero al tocarlo muestra el aviso (CA-INV-02-02).
        Box {                                                 // [U1] el botón y una capa transparente encima
            SmartFoodPrimaryButton(
                text = stringResource(id = R.string.editor_add_button),
                onClick = { onEvent(ProductEditorEvent.SaveClicked) },
                enabled = state.isSaveEnabled,                // gris si faltan nombre, fecha o categoría
                isLoading = state.isSaving,
                // Figma 476:4229: mientras guarda, el botón anima "Registrando…".
                loadingText = stringResource(id = R.string.editor_saving),
                leadingIcon = Icons.Rounded.Add
            )
            if (!state.isSaveEnabled && !state.isSaving) {   // un botón deshabilitado no recibe toques…
                Box(
                    modifier = Modifier
                        .matchParentSize()                    // …así que esta capa del mismo tamaño los recibe
                        .clickable(interactionSource = null, indication = null) { // sin efecto visual de toque
                            onEvent(ProductEditorEvent.SaveClicked)  // y avisa: la lógica marca los campos que faltan
                        }
                )
            }
        }
```

Los diálogos dependen del estado:

Archivo: [ProductEditorScreen.kt, líneas 293–301](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L293-L301)

```kotlin
    val dismiss = { onEvent(ProductEditorEvent.DialogDismissed) } // [U2] lambda guardada en una variable y reutilizada
    // Mientras se guarda, un diálogo deja claro que la app trabaja (CA-INV-02-12, T056).
    state.savingAction?.let { action -> SmartFoodLoadingDialog(text = stringResource(id = action.textRes)) } // solo si guarda
    when (state.dialog) {                                     // [U2] when sobre una sealed interface: un diálogo por caso
        EditorDialog.PhotoSourceSheet -> PhotoSourceSheet(    // hoja "Galería" / "Cámara"
            onGallery = { onEvent(ProductEditorEvent.GalleryChosen) },
            onCamera = { onEvent(ProductEditorEvent.CameraChosen) },
            onDismiss = dismiss
        )
```

`// …` Omitidos: los demás campos de `EditorFields` (fecha, categoría, cantidad, unidad y precio, todos con el mismo
patrón), los demás casos de `EditorDialogs` (hojas de categoría y unidad, calendario, confirmaciones; el último es
`null -> Unit`) y los `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-boxwithconstraints"></a>**`BoxWithConstraints`** | U4 · Cómo adaptar tu app a diferentes tamaños de pantalla | Da el tamaño disponible (`maxWidth`, `maxHeight`) para decidir el diseño. El curso usa clases de tamaño de ventana (`Compact`, `Medium`, `Expanded`) por **ancho**; la app aplica la misma idea por **alto**, porque el problema de los formularios son los teléfonos bajos. Es más costoso que un `Box`: compone su contenido en la fase de diseño. | [L127](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L127) |
| <a id="c-provider"></a>**`CompositionLocalProvider`** | U3 · Cómo compilar apps fabulosas | **Provee** un valor de `CompositionLocal` a todo lo que va adentro. Todos los campos leen `LocalFormMetrics.current` y reciben las medidas del perfil elegido, sin pasarlas por parámetro. | [L132](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L132) |
| <a id="c-ime"></a>**`imePadding()` y `navigationBarsPadding()`** | U4 · Cómo adaptar tu app a diferentes tamaños de pantalla | Rellenos que miden lo que ocupan el teclado (IME) y la barra de navegación. `imePadding` va **dentro** del `BoxWithConstraints`: el perfil de medidas se calcula sin el teclado, y los campos no cambian de tamaño al abrirlo. | [L133](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L133) |
| <a id="c-verticalscroll"></a>**`verticalScroll(rememberScrollState())`** | U2 · Interactuar con la IU y el estado | Hace desplazable una `Column` normal. `rememberScrollState()` recuerda hasta dónde se bajó. Sirve para formularios cortos; para listas largas se usa `LazyColumn`. | [L165](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L165) |
| <a id="c-clickable"></a>**`clickable` y `matchParentSize`** | U2 · Cómo agregar un botón a una app | Cualquier composable puede recibir toques con `clickable`. `matchParentSize` toma el tamaño del `Box` padre **después** de medir al botón, así la capa lo cubre exacto. | [L274](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L274) |
| <a id="c-dialogs"></a>**Diálogos controlados por el estado** | U4 · Componentes de la arquitectura | El diálogo abierto es parte del estado (`dialog: EditorDialog?`). La pantalla solo dibuja el que corresponde; cerrar es un evento. Si fuera un `remember` local, la lógica no podría cerrarlo al guardar. | [L296](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L296) |
| **Slot opcional (`action = … else null`)** | U3 · Cómo compilar apps fabulosas | `SmartFoodTopBar` recibe `action: (@Composable () -> Unit)?`. Con `null` la barra no dibuja nada a la derecha. | [L106](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorScreen.kt#L106) |

**Qué le da la Route.** [`ProductEditorRoute.kt`](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/editor/ProductEditorRoute.kt) abre el selector de fotos (`PickVisualMedia`), pide el permiso de
notificaciones, intercepta "atrás" con `BackHandler` para descartar los cambios y recibe la foto de la cámara por el
`savedStateHandle` con `LaunchedEffect(capturedPhoto)`.

---

## ProductCameraScreen

Visor de la cámara con el recuadro de encuadre y el botón de captura. Si el permiso está negado, explica para qué se
usa y ofrece abrir los ajustes. **No tiene ViewModel.** Spec 006, HU-INV-05; Figma `476:5959`.

**Qué recibe**

| Parámetro | Para qué |
|---|---|
| `permission: CameraPermission` | `GRANTED`, `DENIED` o `UNKNOWN` |
| `preview: @Composable () -> Unit` | La vista previa de la cámara, como **slot** |
| `isCapturing`, `captureFailed` | Botón deshabilitado mientras toma la foto; aviso si falló |
| `onCapture`, `onBack`, `onOpenSettings` | Acciones |

### Código comentado

Archivo: [ProductCameraScreen.kt, líneas 80–125](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L80-L125)

```kotlin
fun ProductCameraScreen(
    permission: CameraPermission,                             // [U2] enum con los tres estados del permiso
    preview: @Composable () -> Unit,                          // [U8] slot: aquí llega la View de CameraX
    isCapturing: Boolean,
    captureFailed: Boolean,
    onCapture: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(title = stringResource(id = R.string.camera_title), onBack = onBack)
        Box(                                                  // [U1] visor: vista previa + recuadro + botón, en capas
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(MaterialTheme.spacing.medium)
                .clip(ViewfinderShape)                        // recorta todo con esquinas redondeadas de 27 dp
                .background(MaterialTheme.colorScheme.outline.copy(alpha = ViewfinderEmptyAlpha))
        ) {
            when (permission) {                               // [U2] when exhaustivo sobre el enum
                CameraPermission.GRANTED -> {
                    preview()                                 // [U8] dibuja la vista previa que llegó como slot
                    ViewfinderFrame(modifier = Modifier.fillMaxSize()) // las 4 esquinas del encuadre, encima
                    CaptureControls(
                        isCapturing = isCapturing,
                        captureFailed = captureFailed,
                        onCapture = onCapture,
                        modifier = Modifier.align(Alignment.BottomCenter) // abajo y al centro
                    )
                }
                CameraPermission.DENIED -> CameraPermissionRationale(
                    onOpenSettings = onOpenSettings,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
                CameraPermission.UNKNOWN -> Unit              // todavía se está pidiendo: no dibuja nada
            }
        }
    }
}
```

Archivo: [ProductCameraScreen.kt, líneas 129–141](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L129-L141)

```kotlin
private fun ViewfinderFrame(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {                             // [U3] dibujo libre: líneas, círculos, formas
        val side = size.width * FrameWidthFraction            // lado del cuadro: 69 % del ancho
        val left = (size.width - side) / 2                    // centrado horizontal
        val top = size.height * FrameTopFraction              // a 17 % del alto
        val corner = FrameCornerLength.toPx()                 // dp → px dentro del Canvas
        val stroke = FrameStroke.toPx()
        val right = left + side
        val bottom = top + side
        fun line(from: Offset, to: Offset) = drawLine(color, from, to, strokeWidth = stroke) // función local reutilizable
        // Superior izquierda, superior derecha, inferior izquierda e inferior derecha.
        line(Offset(left, top), Offset(left + corner, top)); line(Offset(left, top), Offset(left, top + corner))
```

Archivo: [ProductCameraScreen.kt, líneas 159–174](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L159-L174)

```kotlin
        if (captureFailed) {                                  // aviso solo si la última captura falló
            Text(
                text = stringResource(id = R.string.camera_capture_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(MaterialTheme.spacing.inputRadius))
                    .padding(MaterialTheme.spacing.small)
                    .semantics { liveRegion = LiveRegionMode.Polite } // TalkBack lo anuncia apenas aparece
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
        }
        FilledIconButton(                                     // [U3] botón de ícono con fondo, de Material
            onClick = onCapture,
            enabled = !isCapturing,                           // no se puede tomar otra foto mientras guarda la anterior
```

`// …` Omitidos: el resto de `CaptureControls` (forma, colores e ícono de cámara), `CameraPermissionRationale`
(ícono, título, texto y dos botones), las medidas y los dos `@Preview` (con permiso y sin permiso).

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-androidview"></a>**Views dentro de Compose (slot `preview`)** | U8 · Views de Android en Compose | CameraX ofrece su vista previa como una View clásica (`PreviewView`). La Route la envuelve en `AndroidView` y se la pasa a la pantalla como **slot**. La pantalla no conoce CameraX: las pruebas y el `@Preview` le pasan un `Box` gris. | [L82](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L82) |
| <a id="c-canvas"></a>**`Canvas` y `drawLine`** | U3 · Cómo compilar apps fabulosas | `Canvas` da un área para dibujar a mano. Adentro, `size` es el tamaño en píxeles y `toPx()` convierte `dp` a píxeles según la densidad del teléfono. | [L131](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L131) |
| <a id="c-clip"></a>**`Modifier.clip`** | U3 · Cómo compilar apps fabulosas | Recorta todo lo de adentro con una forma. Aquí, la vista previa y el recuadro quedan con las esquinas redondeadas del Figma. | [L102](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L102) |
| <a id="c-liveregion"></a>**`liveRegion`** | U2 · Interactuar con la IU y el estado (accesibilidad) | Un texto que **aparece** no se anuncia solo. Con `liveRegion = Polite`, TalkBack lo lee cuando termina lo que estaba diciendo: la persona se entera de que la foto falló. | [L168](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L168) |
| **Función local** | U2 · Aspectos básicos de Kotlin | `fun line(...)` existe solo dentro de `Canvas`: evita repetir `drawLine(color, …, strokeWidth = stroke)` ocho veces. | [L139](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraScreen.kt#L139) |

**Qué le da la Route.** [`ProductCameraRoute.kt`](../../app/src/main/java/com/smartfood/app/feature/inventory/ui/camera/ProductCameraRoute.kt) pide el permiso al abrir (`LaunchedEffect(Unit)`), lo vuelve a
revisar al regresar de los ajustes (`LifecycleResumeEffect`), crea el `LifecycleCameraController` con `remember` y
envuelve el `PreviewView` en `AndroidView`.
