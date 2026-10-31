# Plan técnico 010: Recetas guardadas

> Cómo se cumple la [spec](./spec.md). Escrito para seguirlo paso a paso (también con OpenCode + DeepSeek): cada
> sección dice qué archivo se crea, qué código existente se copia como modelo y qué no se toca.
> Ruta base: `app/src/main/java/com/smartfood/app/` (abreviada `…/`).

## 1. Arquitectura

Todo vive dentro de `feature/recipes`, porque usa el catálogo, las tarjetas y el detalle de la spec 009:

```
feature/recipes/
├── domain/
│   ├── model/        SavedRecipes.kt: FavoriteSource, Favorite, RecipeCategory, OwnRecipe, OwnRecipeDraft,
│   │                 OwnRecipeField, OwnRecipeError, SavedFilter, SavedSort, SavedRecipeCard
│   ├── repository/   FavoritesRepository, OwnRecipesRepository
│   └── usecase/      ToggleFavorite, ObserveFavoriteIds, ObserveSavedRecipes, ValidateOwnRecipe, SaveOwnRecipe,
│                     GetOwnRecipe, DeleteOwnRecipe, RestoreOwnRecipe, CountSavedRecipes
├── data/
│   ├── repository/   FirestoreFavoritesRepository, FirestoreOwnRecipesRepository, OwnRecipeDocument (mapeo Map ↔ modelo)
│   └── di/           RecipesDataModule (se agregan los dos @Binds)
└── ui/
    ├── saved/        SavedRecipesContract/ViewModel/Screen (pestaña Guardadas), SavedRecipeCard, SortMenu,
    │                 DeletedRecipeBanner
    ├── own/          OwnRecipeEditorContract/ViewModel/Screen, IngredientSheet, OwnRecipeSavedScreen,
    │                 DeleteOwnRecipeScreen, OwnRecipeDetailScreen (+ ViewModel), OwnRecipeErrorText.kt
    └── components/   FavoriteButton.kt (♡ que comparten Recetas, Guardadas y el detalle)
```

Modelos a copiar:

| Para… | Copiar el patrón de… |
|---|---|
| Escribir sin conexión (`write {}` con espera de 3 s, `isPendingSync` con metadatos) | `feature/inventory/data/repository/FirestoreInventoryRepository.kt` |
| Validación en un caso de uso y errores tipados por campo | `ValidateItemUseCase`, `InventoryError`, `InventoryField`, `ui/InventoryErrorText.kt` |
| Editor con borrador, guardar y descartar | `ProductEditorContract/ViewModel/Screen` |
| Búsqueda sin tildes | `SearchInventoryUseCase.normalize` |
| Chips | `inventory/ui/components/CategoryChips.kt` |
| Hojas de selección (categoría, ingrediente, orden) | `inventory/ui/components/OptionSheet.kt` |
| Aviso de resultado sobre la lista (eliminada / deshacer) | `inventory/ui/editor/EditorResultHost.kt` (`savedStateHandle`) |
| Pantalla de éxito con ✓ | la de "¡Listo!" del flujo de la IA o `RegisterSuccess` |
| Botones naranja / blanco con borde | `RecipeActionButton`, `OutlineActionButton` |
| Tarjeta de receta | `RecipeCard` privada de `ui/discover/RecipesDiscoverScreen.kt`: se mueve a `ui/components/RecipeCard.kt` con el ♡ |

**No se toca:** `domain/agent/*`, `ui/cooking/*`, `ui/ai/*` (salvo que la tarjeta compartida muestre el ♡),
`AssetsRecipeCatalog`, `assets/*.json`.

## 2. Modelo de datos

### 2.1 Firestore

```
users/{uid}/favorites/{recipeId}     // recipeId = id del catálogo o de una receta propia
  source:   "catalog" | "own"
  savedAt:  timestamp (serverTimestamp)

users/{uid}/recipes/{recipeId}       // id = firestore.collection(...).document().id
  name:            string (2..60, sin espacios al inicio ni al final)
  nameNormalized:  string (para buscar; SearchInventoryUseCase.normalize)
  category:        string (uno de RecipeCategory.name)
  ingredients:     list<map{ ingredientId: string, quantity: number > 0 }> (0..20)
  cookMinutes:     int (1..240)
  preparation:     string (1..2000)
  createdAt:       timestamp
  updatedAt:       timestamp
```

Se escribe con `Map` (sin reflexión, R8 no cambia) y sin transacciones, para que funcione sin conexión.

### 2.2 Modelos de dominio (`…/feature/recipes/domain/model/SavedRecipes.kt`)

```kotlin
enum class FavoriteSource { CATALOG, OWN }
data class Favorite(val recipeId: String, val source: FavoriteSource, val savedAtMillis: Long, val isPendingSync: Boolean = false)

enum class RecipeCategory { AREPAS, ARROCES, CARNES, POLLO, PESCADOS, PASTAS, SOPAS, ENSALADAS, DESAYUNOS, POSTRES, OTRAS }

data class OwnRecipe(
    val id: String,
    val name: String,
    val category: RecipeCategory,
    val ingredients: List<RecipeIngredient>,   // el de Recipe.kt (ingredientId, quantity)
    val cookMinutes: Int,
    val preparation: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isPendingSync: Boolean = false,
) { companion object { const val SERVINGS = 2 } }

/** Lo que escribió el usuario en el formulario, sin validar. */
data class OwnRecipeDraft(
    val name: String = "",
    val category: RecipeCategory? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val cookMinutes: String = "",
    val preparation: String = "",
)

enum class OwnRecipeField { NAME, CATEGORY, INGREDIENTS, COOK_MINUTES, PREPARATION }

sealed interface OwnRecipeError {
    data class Invalid(val field: OwnRecipeField) : OwnRecipeError
    data object NotFound : OwnRecipeError
    data object Network : OwnRecipeError
    data object Unknown : OwnRecipeError
}
class OwnRecipeException(val error: OwnRecipeError) : Exception()

enum class SavedFilter { FAVORITES, OWN, RECENT }
enum class SavedSort { RECENT, NAME }

/** Una tarjeta de Guardadas, ya armada para la UI. */
data class SavedRecipeCard(
    val id: String,
    val name: String,
    val photo: String?,          // null en las recetas propias → ícono de libro
    val servings: Int,
    val minutes: Int,
    val isFavorite: Boolean,
    val isOwn: Boolean,
    val lastChangeMillis: Long,  // para "Recientes" y "Más recientes" (spec §7)
    val isPendingSync: Boolean,
)
```

## 3. Contratos

```kotlin
// …/feature/recipes/domain/repository/FavoritesRepository.kt
interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Favorite>>
    suspend fun setFavorite(recipeId: String, source: FavoriteSource, favorite: Boolean): Result<WriteOutcome>
}

// …/feature/recipes/domain/repository/OwnRecipesRepository.kt
interface OwnRecipesRepository {
    fun observeRecipes(): Flow<List<OwnRecipe>>
    suspend fun getRecipe(id: String): Result<OwnRecipe>
    fun newRecipeId(): String
    suspend fun saveRecipe(recipe: OwnRecipe, isNew: Boolean): Result<WriteOutcome>
    suspend fun deleteRecipe(id: String): Result<WriteOutcome>
}
```

`WriteOutcome` es el de `feature/inventory/domain/model/InventoryItem.kt`. Se importa tal cual; no se duplica.

| Caso de uso | Firma | Qué hace |
|---|---|---|
| `ToggleFavoriteUseCase` | `suspend operator fun invoke(recipeId: String, source: FavoriteSource, favorite: Boolean): Result<WriteOutcome>` | Marca o quita la favorita. |
| `ObserveFavoriteIdsUseCase` | `operator fun invoke(): Flow<Set<String>>` | Ids favoritos, para pintar el ♡ en Recetas y en el detalle. |
| `ObserveSavedRecipesUseCase` | `operator fun invoke(filter: Flow<SavedFilter>, query: Flow<String>, sort: Flow<SavedSort>): Flow<SavedRecipesData>` | Combina favoritas + recetas propias + catálogo y aplica filtro, búsqueda y orden. Descarta las favoritas huérfanas. `SavedRecipesData(cards, isEmptyOverall, hasPendingSync)`. |
| `ValidateOwnRecipeUseCase` | `operator fun invoke(draft: OwnRecipeDraft): Result<Unit>` | Reglas de CA-SAV-03-05. Falla con `OwnRecipeException(Invalid(campo))` del **primer** campo inválido, en el orden NAME, CATEGORY, INGREDIENTS, COOK_MINUTES, PREPARATION. |
| `SaveOwnRecipeUseCase` | `suspend operator fun invoke(id: String?, draft: OwnRecipeDraft): Result<SavedOwnRecipe>` | Valida, arma `OwnRecipe` (con `trim`) y lo guarda. Si es nueva, también la marca favorita (CA-SAV-03-09). `SavedOwnRecipe(recipe, outcome)`. |
| `GetOwnRecipeUseCase` | `suspend operator fun invoke(id: String): Result<OwnRecipe>` | Carga una receta para el editor y el detalle. |
| `DeleteOwnRecipeUseCase` | `suspend operator fun invoke(id: String): Result<DeletedOwnRecipe>` | Lee la receta y si era favorita, borra las dos y devuelve la copia para deshacer. `DeletedOwnRecipe(recipe, wasFavorite, favoriteSavedAtMillis)`. |
| `RestoreOwnRecipeUseCase` | `suspend operator fun invoke(deleted: DeletedOwnRecipe): Result<WriteOutcome>` | Vuelve a escribir la receta (mismo id y `createdAt`) y su ♡. |
| `CountSavedRecipesUseCase` | `operator fun invoke(): Flow<Int>` | El tamaño de la unión de los ids favoritos (sin huérfanos) y los ids propios. |

Errores: `data/mapper/OwnRecipeErrorMapper.kt` (copia de `InventoryErrorMapper`) es el único que conoce las excepciones
de Firebase. `ui/own/OwnRecipeErrorText.kt` es el único que traduce `OwnRecipeError` a `strings_saved.xml`.

## 4. Navegación

En `core/navigation/Routes.kt` y `ScreenRoute.kt`:

```kotlin
@Serializable data object Saved : Route                                      // pestaña Guardadas
@Serializable data class OwnRecipeEditor(val recipeId: String? = null) : Route   // null = Nueva receta
@Serializable data class OwnRecipeSaved(val recipeId: String, val name: String, val edited: Boolean) : Route
@Serializable data class OwnRecipeDetail(val recipeId: String) : Route
@Serializable data class DeleteOwnRecipe(val recipeId: String, val name: String) : Route
```

- `MainTabScaffold.kt`:
  - `MainTab.SAVED` pasa a `enabled = true`;
  - `navigateToTab` lleva a `Route.Saved`;
  - `Route.Saved` y sus pantallas internas marcan la pestaña "Guardadas".
- `RecipesNavGraph.kt`: una función nueva `savedDestinations(navController)` registra las 5 rutas. `MainNavGraph.kt`
  la llama.
- Las pantallas internas usan `NavTransitions`, igual que `RecipeDetail`.
- "Sí, eliminar" hace `popBackStack<Route.Saved>(inclusive = false)` y deja el resultado `DeletedOwnRecipe` en el
  `savedStateHandle` de Guardadas. Ese aviso lo muestra `DeletedRecipeBanner`, con el mismo patrón que `EditorResultHost`.
- En "¡Receta guardada!":
  - "Crear otra" navega a `OwnRecipeEditor(null)` quitando `OwnRecipeSaved` y el editor anterior;
  - "Ver receta" navega a `OwnRecipeDetail` y quita `OwnRecipeSaved`;
  - la flecha vuelve a `Saved`.

## 5. Pantallas

| Pantalla | Estado (`XUiState`) | Eventos principales |
|---|---|---|
| `SavedRecipesScreen` | `filter`, `query`, `sort`, `cards`, `isEmptyOverall`, `hasPendingSync`, `isSortMenuOpen`, `deleted: DeletedOwnRecipe?`, `undoSecondsLeft` | `FilterChanged`, `QueryChanged`, `SortChanged`, `FavoriteToggled(id)`, `CardClicked`, `AddClicked`, `UndoClicked` |
| `OwnRecipeEditorScreen` | `isNew`, `draft`, `error: OwnRecipeError?` (los campos en rojo **se derivan** de `error`), `isSaving`, `isCategorySheetOpen`, `ingredientSheet: IngredientSheetState?` | `NameChanged`, `CategoryPicked`, `IngredientAdded/Updated/Removed`, `CookMinutesChanged`, `PreparationChanged`, `SaveClicked`, `DiscardClicked`, `DeleteClicked` |
| `OwnRecipeSavedScreen` | sin ViewModel: recibe `name` y `edited` | `ViewRecipe`, `CreateAnother`, `Back` |
| `DeleteOwnRecipeScreen` | `isDeleting`, `error` | `ConfirmClicked`, `CancelClicked` |
| `OwnRecipeDetailScreen` | `recipe`, `ingredientLines`, `isFavorite` | `FavoriteToggled`, `EditClicked` |

La cuenta regresiva de "DESHACER · 8 s" vive en `SavedRecipesViewModel`: un `Job` que baja `undoSecondsLeft` cada
segundo con `delay(1_000)`. Se prueba con `advanceTimeBy` (sin reloj real).

Detalles visuales (de los frames; los valores salen de los tokens existentes y de `LocalFormMetrics`):
- Tarjeta: dos columnas, foto arriba con ♡ en un círculo blanco, nombre en negrita y "N porciones · M min".
- Editor: campos con ícono a la izquierda (libro para el nombre, etiqueta para la categoría), igual que el editor del
  inventario.
- Confirmación: círculo rojo claro con papelera y botón "Sí, eliminar" rojo (`error`/`errorContainer` del esquema). No
  se crean colores nuevos; si el rojo del Figma no coincide con un rol existente, se pregunta en el Issue.
- Vacío: el ícono de libro del Figma (se exporta del nodo como vector), el título y el botón naranja.

## 6. Reglas (`firestore.rules`)

```
match /users/{uid}/favorites/{recipeId} {
  allow read, delete: if request.auth.uid == uid;
  allow create, update: if request.auth.uid == uid
    && request.resource.data.keys().hasOnly(['source', 'savedAt'])
    && request.resource.data.source in ['catalog', 'own']
    && request.resource.data.savedAt is timestamp;
}
match /users/{uid}/recipes/{recipeId} {
  function validRecipe(d) {
    return d.keys().hasOnly(['name', 'nameNormalized', 'category', 'ingredients', 'cookMinutes', 'preparation',
                             'createdAt', 'updatedAt'])
      && d.name is string && d.name.size() >= 2 && d.name.size() <= 60
      && d.nameNormalized is string
      && d.category in ['AREPAS', 'ARROCES', 'CARNES', 'POLLO', 'PESCADOS', 'PASTAS', 'SOPAS', 'ENSALADAS',
                        'DESAYUNOS', 'POSTRES', 'OTRAS']
      && d.ingredients is list && d.ingredients.size() <= 20
      && d.cookMinutes is int && d.cookMinutes >= 1 && d.cookMinutes <= 240
      && d.preparation is string && d.preparation.size() >= 1 && d.preparation.size() <= 2000
      && d.createdAt is timestamp && d.updatedAt is timestamp;
  }
  allow read, delete: if request.auth.uid == uid;
  allow create, update: if request.auth.uid == uid && validRecipe(request.resource.data);
}
```

Las reglas de Firestore no recorren listas, así que la forma de cada ingrediente la garantiza el caso de uso. El tamaño
de la lista sí lo comprueba la regla.

Casos nuevos en `rules-test/firestore.test.js`:
- el dueño lee y escribe;
- otro usuario no puede;
- se rechazan una clave extra, `source` inválido, `cookMinutes` 0 y 241, un nombre de 1 carácter, 21 ingredientes y
  una categoría desconocida.

## 7. Decisiones (ADR)

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| Dos subcolecciones (`favorites`, `recipes`) | Una colección con tipo | Cada documento tiene una sola forma y reglas simples (Zhair, 2026-10-31). |
| Las recetas propias no son `Recipe` | Guardarlas como `Recipe` del catálogo | No tienen pasos guiados, foto ni precios; así el agente nunca las ve. |
| Deshacer reescribe la receta | Borrar cuando pasan los 8 s | Funciona sin conexión y aunque la app se cierre durante el aviso (spec §7). |
| Crear también marca favorita | Solo "Creadas por mí" | El frame `666:3804` muestra la receta creada en "Favoritas". |
| "Sin conexión" = hay escrituras pendientes | Leer el estado de la red | Es el mismo criterio del inventario (`isPendingSync`) y no necesita permisos. |

## 8. Estrategia de pruebas

- **Unitarias (JVM)** con fakes en `app/src/sharedTest/.../testutil/SavedRecipesFakes.kt`: `FakeFavoritesRepository`
  y `FakeOwnRecipesRepository`, con `outcome` y `pendingWrites` como `FakeInventoryRepository`.
  - `ToggleFavoriteUseCaseTest`, `ObserveSavedRecipesUseCaseTest` (filtros, búsqueda sin tildes, orden, huérfanas,
    vacío);
  - `ValidateOwnRecipeUseCaseTest` (cada límite de CA-SAV-03-05), `SaveOwnRecipeUseCaseTest` (nueva = favorita,
    `trim`, PENDING);
  - `DeleteOwnRecipeUseCaseTest` y `RestoreOwnRecipeUseCaseTest`, `CountSavedRecipesUseCaseTest`;
  - `OwnRecipeDocumentTest` (Map ↔ modelo);
  - `SavedRecipesViewModelTest` (cuenta regresiva con `advanceTimeBy`), `OwnRecipeEditorViewModelTest`.
- **Instrumentadas** (`XScreen` sin estado con un `XUiState` a mano):
  - `SavedRecipesScreenTest` (chips, ♡, vacío, "Sin conexión", aviso con deshacer);
  - `OwnRecipeEditorScreenTest` (campos, aviso de error, rojo, descartar);
  - `DeleteOwnRecipeScreenTest`;
  - `RecipesDiscoverScreenTest` y `RecipeDetailScreenTest` (el ♡);
  - `ProfileScreenTest` (contador);
  - `MainNavigationTest` (pestaña Guardadas habilitada).
- **Reglas:** `rules-test/firestore.test.js` (§6).
- Los nombres de las pruebas citan su CA (`CA-SAV-03-05 …`).
