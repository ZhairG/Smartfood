# 05 · Recetas

Pantallas: [Descubrir](#recipesdiscoverscreen), [Detalle](#recipedetailscreen), [Sugerencia con IA](#aiflowscreen),
[Ayuda de la IA](#aihelpscreen) y [Cocina guiada](#cookingscreens). Índice general y mapa de conceptos:
[README](README.md). Los conceptos ya explicados se enlazan a los módulos [01](01-arranque-y-legal.md) a
[04](04-perfil.md).

---

## RecipesDiscoverScreen

La pestaña Recetas: búsqueda, la tarjeta "¿Qué cocinamos hoy?" (abre la IA), los filtros y las tarjetas de receta en dos
columnas. Spec 009, HU-RIA-01; Figma `557:3028`.

**Qué recibe.** `state: RecipesDiscoverUiState` (lee `query`, `filter`, `isLoading`, `hasError` y `recipes`), `onEvent`
y `onSuggestRecipes: (() -> Unit)?`, que abre la IA.

### Código comentado

Archivo: [RecipesDiscoverScreen.kt, líneas 75–130](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L75-L130)

```kotlin
fun RecipesDiscoverScreen(
    state: RecipesDiscoverUiState,
    onEvent: (RecipesDiscoverEvent) -> Unit,
    modifier: Modifier = Modifier,
    onSuggestRecipes: (() -> Unit)? = null,                   // [U2] función que puede ser null: sin ella el botón se desactiva
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RecipesHeader()                                       // encabezado verde con el título
        LazyVerticalGrid(                                     // [U3] cuadrícula perezosa
            columns = GridCells.Fixed(CardColumns),           // [U3] número fijo de columnas: 2
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap), // espacio entre columnas
            verticalArrangement = Arrangement.spacedBy(ContentGap) // espacio entre filas
        ) {
            item(key = "buscador", span = { GridItemSpan(maxLineSpan) }) { // [U3] span: ocupa TODAS las columnas de la fila
                SearchField(
                    query = state.query,
                    onQueryChanged = { onEvent(RecipesDiscoverEvent.QueryChanged(it)) },
                    onClearQuery = { onEvent(RecipesDiscoverEvent.ClearQuery) }
                )
            }
            item(key = "tarjeta_ia", span = { GridItemSpan(maxLineSpan) }) { // la tarjeta de la IA, ancho completo
                SuggestCard(onSuggestRecipes = onSuggestRecipes)
            }
            item(key = "filtros", span = { GridItemSpan(maxLineSpan) }) {
                RecipeFilters(
                    selected = state.filter,
                    onSelected = { onEvent(RecipesDiscoverEvent.FilterSelected(it)) }
                )
            }

            when {                                            // [U4] los cuatro estados, dentro de la misma cuadrícula
                state.isLoading -> item(key = "cargando", span = { GridItemSpan(maxLineSpan) }) { Loading() }
                state.hasError -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Message(stringResource(id = R.string.recipes_load_error))
                }

                state.recipes.isEmpty() -> item(key = "vacio", span = { GridItemSpan(maxLineSpan) }) {
                    Message(stringResource(id = R.string.recipes_empty))
                }

                else -> items(items = state.recipes, key = { it.id }) { recipe -> // [U3] una tarjeta por celda (1 columna)
                    RecipeCard(
                        card = recipe,
                        onClick = { onEvent(RecipesDiscoverEvent.RecipeClicked(recipe.id)) }
                    )
                }
            }
        }
    }
}
```

Archivo: [RecipesDiscoverScreen.kt, líneas 160–164](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L160-L164)

```kotlin
        RecipeActionButton(                                   // botón naranja de recetas
            text = stringResource(id = R.string.recipes_suggest),
            onClick = { onSuggestRecipes?.invoke() },         // [U2] ?.invoke(): llama a la función solo si no es null
            enabled = onSuggestRecipes != null,
```

Archivo: [RecipesDiscoverScreen.kt, líneas 292–309](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L292-L309)

```kotlin
    val highlighted = card.expiringIngredient != null          // ¿usa algo que vence pronto?
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = CardElevation, shape = shape) // [U3] sombra de la tarjeta
            .clip(shape)                                      // recorta las esquinas
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(                                            // [U3] then: agrega un Modifier calculado a la cadena
                if (highlighted) Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, shape) else Modifier // borde naranja solo si rescata algo
            )
            .clickable(role = Role.Button, onClick = onClick) // toda la tarjeta se toca; TalkBack la anuncia como botón
    ) {
        RecipePhoto(                                          // foto local de drawable-nodpi
            photo = card.photo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PhotoAspectRatio)                // [U3] alto calculado a partir del ancho
        )
```

`// …` Omitidos: el resto de `SuggestCard` y de `RecipeCard` (textos de nombre, tiempo, kcal y costo), `SearchField`
(igual al del [inventario](03-inventario.md#inventorysearchscreen)), los filtros, las medidas y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-grid"></a>**`LazyVerticalGrid` y `GridCells.Fixed`** | U3 · Cómo crear una lista desplazable | Como `LazyColumn`, pero en cuadrícula: solo compone las celdas visibles. `GridCells.Fixed(2)` fija dos columnas; `GridCells.Adaptive(minSize)` calcularía cuántas caben. | [L87](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L87) |
| <a id="c-span"></a>**`span = { GridItemSpan(maxLineSpan) }`** | U3 · Cómo crear una lista desplazable | Dice **cuántas columnas ocupa** un elemento. `maxLineSpan` es el total de columnas, así que el buscador, la tarjeta de la IA y los filtros ocupan toda la fila. Las recetas usan el valor por defecto: una columna. Todo vive en la misma cuadrícula, así que se desplaza junto. | [L94](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L94) |
| <a id="c-then"></a>**`Modifier.then`** | U3 · Cómo compilar apps fabulosas | Agrega a la cadena otro `Modifier`, que puede ser calculado: el borde naranja o `Modifier` (nada). Evita partir la cadena en dos con un `if`. | [L299](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L299) |
| <a id="c-aspect"></a>**`aspectRatio`** | U3 · Cómo crear una lista desplazable | El alto sale del ancho: todas las fotos tienen la misma proporción, sin importar el ancho de la columna en cada teléfono. | [L308](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L308) |
| <a id="c-shadow"></a>**`Modifier.shadow`** | U3 · Cómo compilar apps fabulosas | Dibuja una sombra con la forma dada. Va **antes** de `clip`: si se recorta primero, la sombra también queda recortada. | [L296](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L296) |
| **Llamar una función nullable (`?.invoke()`)** | U2 · Aspectos básicos de Kotlin | `onSuggestRecipes?.invoke()` llama a la función solo si existe. Junto con `enabled = onSuggestRecipes != null`, el botón queda gris mientras la IA no esté conectada. | [L162](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/discover/RecipesDiscoverScreen.kt#L162) |

---

## RecipeDetailScreen

Foto, tiempos, dificultad, selector de porciones, "Precios de referencia" e ingredientes agrupados, con "Empezar a
cocinar" flotando abajo. Spec 009, HU-RIA-04; Figma `557:2856`.

**Qué recibe.** `state: RecipeDetailUiState` (lee `name`, `isLoading`, `hasError`, `photo`, los tiempos, `difficulty`,
`servings`, `store` y `groups`), `onEvent` y `onBack`.

### Código comentado

Archivo: [RecipeDetailScreen.kt, líneas 120–168](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L120-L168)

```kotlin
            else -> Box(modifier = Modifier.weight(1f)) {     // [U1] Box: la lista y el botón flotante encima
                LazyColumn(                                   // [U3] una sola lista para todo el detalle
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ListBottomPadding) // espacio para el botón flotante
                ) {
                    item(key = "foto") {                      // [U3] cada bloque es un item con key
                        RecipePhoto(
                            photo = state.photo,
                            modifier = Modifier
                                .padding(horizontal = ContentPadding, vertical = PhotoVerticalPadding)
                                .fillMaxWidth()
                                .aspectRatio(PhotoAspectRatio)
                                .clip(RoundedCornerShape(PhotoRadius))
                        )
                    }
                    item(key = "tiempos") {
                        TimeMetrics(                          // tarjetas de preparación, cocción, reposo y dificultad
                            prepMinutes = state.prepMinutes,
                            cookMinutes = state.cookMinutes,
                            restMinutes = state.restMinutes,
                            difficulty = state.difficulty
                        )
                    }
                    item(key = "ingredientes") {
                        IngredientsHeader(                    // título y selector "−  4  +"
                            servings = state.servings,
                            onDecrease = { onEvent(RecipeDetailEvent.DecreaseServings) },
                            onIncrease = { onEvent(RecipeDetailEvent.IncreaseServings) }
                        )
                    }
                    item(key = "precios") {
                        ReferencePrices(store = state.store)  // "Precios de referencia · D1"
                    }
                    item(key = "grupos") {
                        IngredientGroups(groups = state.groups) // ingredientes ya escalados a las porciones
                    }
                }
                RecipeActionButton(                           // "Empezar a cocinar"
                    text = stringResource(id = R.string.recipes_start_cooking),
                    onClick = { onEvent(RecipeDetailEvent.StartCooking) },
                    trailingIcon = R.drawable.ic_figma_play,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)           // abajo a la derecha, encima de la lista
                        .padding(end = CtaEndPadding, bottom = CtaBottomPadding)
                        .width(CtaWidth)
                )
            }
```

Archivo: [RecipeDetailScreen.kt, líneas 338–363](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L338-L363)

```kotlin
private fun PortionButton(
    text: String,                                             // "−" o "+"
    description: String,                                      // "Quitar una porción" para TalkBack
    filled: Boolean,                                          // el "+" va relleno de naranja
    enabled: Boolean,                                         // falso en 1 y en 12 porciones
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .alpha(if (enabled) 1f else DisabledAlpha)        // [U3] transparencia: se ve apagado en los extremos
            .size(PortionButtonSize)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (filled) MaterialTheme.colorScheme.secondary else GreyDivider,
                shape = CircleShape
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick) // [U2] no responde si está desactivado
            .semantics {
                contentDescription = description              // TalkBack lee la descripción, no el símbolo
                if (!enabled) disabled()                      // y avisa que está desactivado
            }
```

`// …` Omitidos: la barra (`SmartFoodTopBar` con el nombre de la receta), los estados de carga y error (un `Box` con
`weight(1f)`), `TimeMetrics`, `IngredientsHeader`, `ReferencePrices`, `IngredientGroups` e `IngredientRow`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| **`LazyColumn` con bloques distintos** | U3 · Cómo crear una lista desplazable | Una `LazyColumn` no es solo para listas de datos iguales: aquí cada `item(key = …)` es un bloque distinto (foto, tiempos, ingredientes). Así hay un solo desplazamiento y lo que no se ve no se compone. | [L125](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L125) |
| <a id="c-alpha"></a>**`Modifier.alpha`** | U3 · Cómo compilar apps fabulosas | Transparencia de todo el elemento. Se aplica en la fase de dibujo: cambiarla no vuelve a medir el botón. | [L348](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L348) |
| <a id="c-disabled"></a>**`semantics { disabled() }`** | U2 · Interactuar con la IU y el estado (accesibilidad) | Un `Box` con `clickable` no le dice a TalkBack que está desactivado; `disabled()` lo anuncia. `contentDescription` reemplaza el "−" por una frase clara. | [L362](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/detail/RecipeDetailScreen.kt#L362) |
| **Imagen local con `painterResource`** | U1 · Crea un diseño básico | Las fotos de las recetas están en `drawable-nodpi` y `RecipePhoto` las busca por nombre. No vienen de Internet, así que no hace falta Coil: el catálogo funciona sin conexión. | [L25](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/components/RecipePhoto.kt#L25) |

---

## AiFlowScreen

Una sola pantalla con cuatro pasos según `state.step`: **Contexto**, **Analizando**, **Tu plan de 3 días** y **Sin
coincidencias**. Spec 009, HU-RIA-02; Figma `633:2948`, `666:3546`, `557:2903` y `557:2515`.

**Qué recibe.** `state: AiUiState` (lee `step`, `sheet`, `budgetCop`, `servings`, `maxMinutes`, `plan`, `reasons`,
`ingredients`, `detected` y más), `onEvent` y `onBack`.

### Código comentado

Archivo: [AiFlowScreen.kt, líneas 86–123](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L86-L123)

```kotlin
fun AiFlowScreen(
    state: AiUiState,
    onEvent: (AiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Atrás mientras analiza cancela el cálculo, como "Cancelar".
    BackHandler(enabled = state.step == AiStep.Loading) { onEvent(AiEvent.CancelClicked) } // [U4] solo intercepta en Loading
    AiSheets(state = state, onEvent = onEvent)                // hojas de porciones y tiempo, según state.sheet
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(
                id = when (state.step) {                      // [U2] when como expresión: un título por paso
                    AiStep.Context -> R.string.recipes_ai_title
                    AiStep.Loading -> R.string.recipes_ai_loading_title
                    AiStep.Plan -> R.string.recipes_ai_plan_title
                    AiStep.NoPlan -> R.string.recipes_ai_no_plan_title
                }
            ),
            onBack = onBack,
            action = when (state.step) {                      // [U3] slot de la acción: distinto según el paso
                AiStep.Context -> ({ BarAction(stringResource(id = R.string.recipes_ai_help)) { onEvent(AiEvent.HelpClicked) } }) // "Ayuda"
                AiStep.Plan -> ({ BarAction(stringResource(id = R.string.recipes_ai_adjust)) { onEvent(AiEvent.AdjustClicked) } }) // "Ajustar"
                else -> null                                  // sin acción en Analizando y Sin coincidencias
            }
        )
        when (state.step) {                                   // [U2] un contenido por paso
            AiStep.Context -> ScrollContent { ContextContent(state, onEvent) }
            AiStep.Loading -> LoadingContent(state, onEvent, Modifier.weight(1f))
            AiStep.Plan -> ScrollContent { PlanContent(state, onEvent) }
            AiStep.NoPlan -> ScrollContent { NoPlanContent(state, onEvent) }
        }
    }
}
```

Archivo: [AiFlowScreen.kt, líneas 137–147](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L137-L147)

```kotlin
private fun ColumnScope.ScrollContent(content: @Composable ColumnScope.() -> Unit) { // [U3] slot con receptor ColumnScope
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentGap),
        modifier = Modifier
            .weight(1f)                                       // posible porque la función es de ColumnScope
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ContentPadding),
        content = content                                     // pasa el slot como contenido del Column
    )
}
```

El presupuesto con un `Slider`:

Archivo: [AiFlowScreen.kt, líneas 229–241](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L229-L241)

```kotlin
            Slider(                                           // [U2] control deslizante de Material
                value = state.budgetCop.toFloat(),            // [U2] elevación de estado: el valor llega del estado
                onValueChange = { onEvent(AiEvent.BudgetChanged(it.roundToInt().toLong())) }, // Float → Long, redondeado
                valueRange = AiUiState.MIN_BUDGET_COP.toFloat()..AiUiState.MAX_BUDGET_COP.toFloat(), // [U2] rango: 2 000..100 000
                steps = BudgetSteps,                          // posiciones intermedias: avanza de a 1 000
                colors = SliderDefaults.colors(
                    thumbColor = GreenCheck,
                    activeTrackColor = GreenCheck,
                    inactiveTrackColor = GreyLight,
                    activeTickColor = Color.Transparent,      // oculta las marcas de cada paso
                    inactiveTickColor = Color.Transparent,
                )
            )
```

El plan: una tarjeta por día y las razones.

Archivo: [AiFlowScreen.kt, líneas 383–419](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L383-L419)

```kotlin
    val plan = state.plan ?: return                           // [U2] Elvis con return: sin plan, no dibuja nada
    Column(
        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(RoundedCornerShape(CardRadius))
            .background(if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else GreenSoft) // [U3] otro fondo en modo oscuro
            .padding(CardPadding)
    ) {
        Text(
            text = stringResource(id = R.string.recipes_ai_ready),
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_ready_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
    }
    plan.days.forEach { day ->                                // [U3] una DayCard por día (son 3: no hace falta Lazy)
        DayCard(day = day, onClick = { onEvent(AiEvent.RecipeClicked(day.recipe.id)) })
    }
    WhiteCard {
        Text(
            text = stringResource(id = R.string.recipes_ai_why),  // "¿Por qué te conviene?"
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        // Una sola línea con "•" entre cada razón, como el Figma (557:2903).
        val reasons = state.reasons.map { it.text(state.ingredients, state.detected) } // [U3] map: cada razón → texto
        Text(
            text = reasons.joinToString(separator = " ") { "• $it" }, // [U3] une todo en un texto: "• a • b • c"
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
    }
```

Archivo: [AiFlowScreen.kt, líneas 352–359](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L352-L359)

```kotlin
        CircularProgressIndicator(                            // [U3] arco girando mientras el agente calcula
            color = GreenCheck,
            trackColor = Color.Transparent,                   // sin el círculo gris de fondo
            strokeWidth = ArcStroke,                          // grosor del arco
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(ArcSize)
        )
```

`// …` Omitidos: `ContextContent` (inventario detectado, preferencias, presupuesto, porciones, tiempo y picante),
`AiSheets`, `NoPlanContent`, `DayCard`, `BarAction`, las medidas y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-backhandler"></a>**`BackHandler(enabled = …)`** | U4 · Navigation en Jetpack Compose | Intercepta el botón o gesto "atrás". Con `enabled = false` es **como si no existiera** y "atrás" hace lo de siempre: volver a la pantalla anterior de la pila. Aquí solo está activo mientras analiza, y entonces "atrás" cancela el cálculo. | [L93](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L93) |
| <a id="c-pasos"></a>**Varios pasos en una sola pantalla** | U4 · Componentes de la arquitectura | En lugar de cuatro rutas, un `when (state.step)` elige el contenido, el título y la acción de la barra. El plan no viaja entre pantallas y "Ajustar" conserva lo elegido. | [L116](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L116) |
| <a id="c-slider"></a>**`Slider`** | U2 · Interactuar con la IU y el estado | Control para elegir un número en un rango. Trabaja con `Float`; `steps` agrega posiciones intermedias para que avance de a $1.000. Como todo control, recibe `value` y avisa con `onValueChange`. | [L229](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L229) |
| <a id="c-colecciones"></a>**`map`, `forEach`, `joinToString`** | U3 · Más aspectos básicos de Kotlin | Funciones de colecciones: `map` transforma cada razón en texto, `forEach` dibuja una tarjeta por día y `joinToString` une los textos con "•". | [L414](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L414) |
| <a id="c-elvis-return"></a>**`?: return`** | U2 · Aspectos básicos de Kotlin | Si `state.plan` es `null`, la función termina ahí. Después de esa línea, `plan` ya no es nullable. | [L383](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L383) |
| <a id="c-darktheme"></a>**`LocalIsDarkTheme`** | U3 · Cómo compilar apps fabulosas | `CompositionLocal` propio de la app con el modo vigente. Se usa cuando un color del Figma (`GreenSoft`) no es un rol del tema y necesita otro valor en modo oscuro. | [L390](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L390) |
| **`ColumnScope` + slot con receptor** | U3 · Más aspectos básicos de Kotlin | `content: @Composable ColumnScope.() -> Unit` permite que el contenido use `weight` y `align`. `ScrollContent` evita repetir la columna desplazable en tres pasos. | [L137](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiFlowScreen.kt#L137) |

---

## AiHelpScreen

"¿Cómo decide la IA?" con sus tres criterios, "Controles y seguridad", el aviso "La IA puede equivocarse" y "Contactar
soporte". **No tiene ViewModel**, como los documentos legales. Spec 009, CA-RIA-03-01; Figma `557:2476`.

**Qué recibe.** `onBack`, `onContactSupport` y `modifier`.

Archivo: [AiHelpScreen.kt, líneas 41–70](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiHelpScreen.kt#L41-L70)

```kotlin
fun AiHelpScreen(
    onBack: () -> Unit,                                       // solo dos acciones: no hay estado
    onContactSupport: () -> Unit,                             // abrir el correo lo hace la navegación
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(title = stringResource(id = R.string.recipes_ai_help_title), onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(ContentGap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())        // [U2] texto corto: basta una Column desplazable
                .padding(ContentPadding)
        ) {
            Text(
                text = stringResource(id = R.string.recipes_ai_help_heading),
                color = MaterialTheme.colorScheme.onSurface,
                style = TitleStyle(),
                modifier = Modifier.semantics { heading() }   // encabezado para TalkBack
            )
            WhiteCard {                                       // tarjeta blanca reutilizada del flujo de la IA
                Criterion(R.string.recipes_ai_help_urgency, R.string.recipes_ai_help_urgency_body)
                Criterion(R.string.recipes_ai_help_usage, R.string.recipes_ai_help_usage_body)
                Criterion(R.string.recipes_ai_help_budget, R.string.recipes_ai_help_budget_body)
            }
```

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| **Pantalla sin estado ni ViewModel** | U4 · Componentes de la arquitectura | Si una pantalla no tiene datos que cambien ni lógica, no necesita ViewModel: recibe solo sus acciones. | [L41](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/ai/AiHelpScreen.kt#L41) |

---

## CookingScreens

`CookingScreens.kt` tiene **dos** pantallas: "Antes de empezar" (`CookingIntroScreen`) y la guía paso a paso
(`CookingScreen`). Aquí se comenta la guía. Spec 009, HU-RIA-04; Figma `557:2781`.

**Qué recibe `CookingScreen`.** `state: CookingUiState` (lee `dialog`, `currentStepIndex`, `steps`, `currentStep`,
`isLoading`, `hasError`, `progressFraction`, `photo`, `hasTimer`, `timerSecondsRemaining`, `isTimerRunning`,
`isAlarmRinging`, `isFirstStep` e `isLastStep`), `onEvent` y `onBack`.

### Código comentado

Archivo: [CookingScreens.kt, líneas 163–186](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L163-L186)

```kotlin
fun CookingScreen(
    state: CookingUiState,
    onEvent: (CookingEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CookingDialogs(dialog = state.dialog, onEvent = onEvent)  // "¿Cocinaste esta receta?", guardando y resultado
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.recipes_cooking_progress, state.currentStepIndex + 1, state.steps.size), // "Paso 2 de 5"
            onBack = onBack,
            action = {
                Text(                                         // "Salir" como texto tocable
                    text = stringResource(id = R.string.recipes_exit),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = CardTitleStyle(),
                    modifier = Modifier.clickable(role = Role.Button) { onEvent(CookingEvent.ExitClicked) }
                )
            }
        )
```

Archivo: [CookingScreens.kt, líneas 199–248](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L199-L248)

```kotlin
                StepProgress(fraction = state.progressFraction) // barra de avance: 0,4 = 2 de 5 pasos
                Text(
                    text = stringResource(id = R.string.recipes_step_label, state.currentStepIndex + 1),
                    color = GreenCheck,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                RecipePhoto(
                    photo = state.photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(StepPhotoRatio)
                        .clip(RoundedCornerShape(CardRadius))
                )
                Text(
                    text = step.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TitleStyle(),
                    modifier = Modifier.semantics { heading() }
                )
                Text(text = step.text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = BodyStyle())
                if (state.hasTimer) {                         // la tarjeta del temporizador solo en pasos con tiempo
                    TimerCard(
                        secondsRemaining = state.timerSecondsRemaining,
                        running = state.isTimerRunning,
                        ringing = state.isAlarmRinging,
                        onToggle = { onEvent(CookingEvent.TimerToggled) },
                        onReset = { onEvent(CookingEvent.ResetTimerClicked) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ActionsGap), modifier = Modifier.fillMaxWidth()) {
                    OutlineActionButton(                      // "Anterior": botón blanco con borde
                        text = stringResource(id = R.string.recipes_previous),
                        enabled = !state.isFirstStep,         // desactivado en el primer paso
                        onClick = { onEvent(CookingEvent.PreviousStepClicked) },
                        modifier = Modifier.weight(1f)        // los dos botones con el mismo ancho
                    )
                    if (state.isLastStep) {                   // en el último paso, "Terminar" en vez de "Siguiente"
                        RecipeActionButton(
                            text = stringResource(id = R.string.recipes_finish),
                            onClick = { onEvent(CookingEvent.FinishClicked) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        RecipeActionButton(
                            text = stringResource(id = R.string.recipes_next),
                            onClick = { onEvent(CookingEvent.NextStepClicked) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
```

Archivo: [CookingScreens.kt, líneas 256–270](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L256-L270)

```kotlin
private fun StepProgress(fraction: Float) {
    Box(                                                      // [U1] fondo gris de la barra
        modifier = Modifier
            .fillMaxWidth()
            .height(ProgressHeight)
            .background(GreyLight)
    ) {
        Box(                                                  // la parte naranja, encima
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))      // [U1] fillMaxWidth(fracción): ocupa ese porcentaje del ancho
                .height(ProgressHeight)
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
}
```

La tarjeta del temporizador, accesible:

Archivo: [CookingScreens.kt, líneas 287–310](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L287-L310)

```kotlin
    val toggleLabel = stringResource(                         // qué hará el toque, según el estado
        id = when {
            ringing -> R.string.recipes_timer_stop_alarm      // "Detener alarma"
            running -> R.string.recipes_timer_pause           // "Pausar"
            else -> R.string.recipes_timer_start              // "Iniciar"
        }
    )
    val stateLabel = stringResource(                          // en qué estado está
        id = when {
            ringing -> R.string.recipes_timer_ringing
            running -> R.string.recipes_timer_running
            else -> R.string.recipes_timer_paused
        }
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(RoundedCornerShape(CardRadius))
            .background(container)                            // amarillo (YellowSoft) o surfaceVariant en oscuro
            .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle) // TalkBack: "doble toque para Pausar"
            .semantics { stateDescription = stateLabel }      // TalkBack: "En marcha" o "En pausa"
            .padding(CardPadding)
```

`// …` Omitidos: `CookingIntroScreen` (foto, "Prepara antes de iniciar" y "Comenzar guía"), los estados de carga y
error, el texto del temporizador (`minutos / 60` y `% 60`), `CookingDialogs`, los estilos y el `@Preview`.

### Conceptos del curso en esta pantalla

| Concepto | Unidad · ruta | Explicación | Dónde |
|---|---|---|---|
| <a id="c-fraction"></a>**`fillMaxWidth(fracción)`** | U1 · Crea un diseño básico | `fillMaxWidth(0.4f)` ocupa el 40 % del ancho. Una barra de progreso son dos `Box` superpuestos: el gris completo y el naranja con la fracción del estado. `coerceIn(0f, 1f)` evita valores fuera de rango. | [L265](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L265) |
| <a id="c-statedesc"></a>**`onClickLabel` y `stateDescription`** | U2 · Interactuar con la IU y el estado (accesibilidad) | La tarjeta cambia de función (iniciar, pausar, detener la alarma). `onClickLabel` le dice a TalkBack **qué hará** el toque, y `stateDescription`, **en qué estado está**. | [L309](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L309) |
| **Botón según el estado** | U2 · Cómo agregar un botón a una app | `if (state.isLastStep)` cambia "Siguiente" por "Terminar" y `enabled = !state.isFirstStep` desactiva "Anterior". Los dos se derivan de `currentStepIndex`. | [L235](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L235) |
| **Textos con varios argumentos** | U1 · Crea un diseño básico | `stringResource(id, paso, total)` llena "Paso %1$d de %2$d". El orden de los argumentos sigue el del `strings.xml`. | [L176](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/cooking/CookingScreens.kt#L176) |

**Qué le da la navegación.** En [`RecipesNavGraph.kt`](../../app/src/main/java/com/smartfood/app/feature/recipes/ui/RecipesNavGraph.kt#L174)
la ruta crea la alarma con `remember`, la apaga al salir con `DisposableEffect(alarm) { onDispose { alarm.stop() } }` y
la hace sonar cuando la lógica avisa que el temporizador llegó a 0. La pantalla no sabe reproducir sonido.
