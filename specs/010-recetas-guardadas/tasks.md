# Tareas 010: Recetas guardadas

> Derivadas de [`plan.md`](./plan.md). Cada tarea cita los criterios que cubre y se marca `[x]` cuando cumple la
> Definition of Done de la [constitución](../constitution.md#7-definition-of-done). Los Issues agrupan tareas por
> feature vertical (spec §10) y se trabajan **en orden** en la rama `feature/010-recetas-guardadas`, con **un commit por
> tarea** (`Tnnn`). Ruta base: `app/src/main/java/com/smartfood/app/` (`…/`).
>
> **No se tocan en ninguna tarea:** `…/feature/recipes/domain/agent/*`, `…/feature/recipes/ui/cooking/*`,
> `app/src/main/assets/*.json`, `AssetsRecipeCatalog.kt`.

## Fase 0: Preparación
- [x] T001 La spec, el plan, las tareas y las capturas de `disenos-figma/Modulo 5 - Recetas guardadas`.

## Issue 1: Favoritas y lista Guardadas (intermedia, #117)
- [x] T002 **Dominio y datos de favoritas** (RF-SAV-01, CA-SAV-01-04, 01-05):
  - crea `…/feature/recipes/domain/model/SavedRecipes.kt` con los modelos de plan §2.2 que usa este Issue:
    `FavoriteSource`, `Favorite`, `SavedFilter`, `SavedSort`, `SavedRecipeCard`;
  - crea `…/domain/repository/FavoritesRepository.kt` y `…/data/repository/FirestoreFavoritesRepository.kt` (patrón
    `write {}` de `FirestoreInventoryRepository`);
  - agrega su `@Binds` en `RecipesDataModule.kt`;
  - agrega `FakeFavoritesRepository` en `app/src/sharedTest/.../testutil/SavedRecipesFakes.kt`.
- [x] T003 **Reglas de favoritas** (RNF-SAV-01): bloque `favorites` de plan §6 en `firestore.rules` y sus casos en
  `rules-test/firestore.test.js`.
- [x] T004 **Casos de uso** (CA-SAV-01-03, CA-SAV-02-03, 02-04, 02-05):
  - `ToggleFavoriteUseCase`, `ObserveFavoriteIdsUseCase` y `ObserveSavedRecipesUseCase` (en este Issue, solo con
    favoritas del catálogo; T010 agrega las propias);
  - pruebas `ToggleFavoriteUseCaseTest` y `ObserveSavedRecipesUseCaseTest`.
- [x] T005 **♡ en Recetas y en el detalle** (CA-SAV-01-01 a 01-03, desviación §9):
  - crea `…/feature/recipes/ui/components/FavoriteButton.kt`;
  - mueve `RecipeCard` de `RecipesDiscoverScreen.kt` a `…/ui/components/RecipeCard.kt` con el ♡;
  - en `RecipesDiscoverContract/ViewModel` y `RecipeDetailContract/ViewModel`, agrega `favoriteIds` y el evento
    `FavoriteToggled`;
  - actualiza `RecipesDiscoverViewModelTest`, `RecipesDiscoverScreenTest` y `RecipeDetailScreenTest`.
- [x] T006 **Pestaña Guardadas** (CA-SAV-02-01):
  - `Route.Saved` en `Routes.kt` y `ScreenRoute.kt`;
  - en `MainTabScaffold.kt`: `SAVED` habilitada y `navigateToTab` hacia `Route.Saved`;
  - `savedDestinations()` en `RecipesNavGraph.kt`, llamada desde `MainNavGraph.kt`;
  - actualiza `MainNavigationTest`.
- [x] T007 **Pantalla Guardadas** (CA-SAV-02-02 a 02-10):
  - `…/ui/saved/SavedRecipesContract.kt`, `SavedRecipesViewModel.kt`, `SavedRecipesScreen.kt`, `SavedRecipeCard.kt` y
    `SortMenu.kt`;
  - textos en el archivo nuevo `res/values/strings_saved.xml`;
  - el ícono de libro del vacío se exporta del Figma `557:2585` a `res/drawable/ic_figma_book.xml`;
  - el "+" y "Crear receta" navegan a `Route.OwnRecipeEditor()`, que en este Issue es un destino vacío; T013 lo llena.
- [x] T008 **Pruebas:** `SavedRecipesViewModelTest` y `SavedRecipesScreenTest` (chips, búsqueda, orden, vacío,
  "Sin conexión").

## Issue 2: Crear y editar receta propia (avanzada, #130)
- [x] T009 **Dominio y datos de recetas propias** (RF-SAV-02):
  - `RecipeCategory`, `OwnRecipe`, `OwnRecipeDraft`, `OwnRecipeField`, `OwnRecipeError` y `OwnRecipeException` en
    `SavedRecipes.kt`;
  - `OwnRecipesRepository`, `FirestoreOwnRecipesRepository`, `OwnRecipeDocument.kt` (Map ↔ modelo) y
    `data/mapper/OwnRecipeErrorMapper.kt`;
  - su `@Binds` y `FakeOwnRecipesRepository`;
  - prueba `OwnRecipeDocumentTest`.
- [x] T010 **Reglas de recetas** (RNF-SAV-01): bloque `recipes` de plan §6 y sus casos en `rules-test`.
  `ObserveSavedRecipesUseCase` suma las recetas propias (chips "Creadas por mí" y "Recientes", CA-SAV-02-03).
- [x] T011 **Validación** (CA-SAV-03-05): `ValidateOwnRecipeUseCase` y `ValidateOwnRecipeUseCaseTest`, con cada límite:
  - 1, 2, 60 y 61 caracteres en el nombre;
  - 0, 1, 240 y 241 minutos;
  - 2.000 y 2.001 caracteres en la preparación;
  - 20 y 21 ingredientes, ingredientes repetidos y cantidad 0.
- [x] T012 **Guardar y leer** (CA-SAV-03-04, 03-08, 03-09, 04-02):
  - `SaveOwnRecipeUseCase` (una receta nueva también queda favorita) y `GetOwnRecipeUseCase`;
  - prueba `SaveOwnRecipeUseCaseTest`.
- [x] T013 **Editor** (CA-SAV-03-01 a 03-06, 04-01, 04-03, 04-04, desviaciones §9):
  - `…/ui/own/OwnRecipeEditorContract.kt`, `OwnRecipeEditorViewModel.kt` y `OwnRecipeEditorScreen.kt` (copiar
    `ProductEditor*`);
  - `IngredientSheet.kt` y la hoja de categoría con `OptionSheet`;
  - `OwnRecipeErrorText.kt`;
  - la ruta `OwnRecipeEditor(recipeId)` ya existe como destino vacío (T007): se llena, no se vuelve a crear.
- [x] T014 **"¡Receta guardada!" y detalle** (CA-SAV-03-07, 04-02, 04-05):
  - `OwnRecipeSavedScreen.kt` (creada y editada);
  - `OwnRecipeDetailScreen.kt` con su ViewModel;
  - las rutas `OwnRecipeSaved` y `OwnRecipeDetail`;
  - la tarjeta propia de Guardadas ya abre el editor con `SavedRecipesEffect.OpenOwnRecipe` (CA-SAV-02-06, T007).
- [x] T015 **Pruebas:** `OwnRecipeEditorViewModelTest`, `OwnRecipeEditorScreenTest`, `OwnRecipeSavedScreenTest` y
  `OwnRecipeDetailScreenTest`.
- [x] T016 **Navegación del flujo:** crear → éxito → "Ver receta" / "Crear otra" / flecha, en `MainNavigationTest`.

## Issue 3: Eliminar con deshacer y contador del Perfil (intermedia, #131)
- [x] T017 **Casos de uso** (CA-SAV-05-03, 05-04): `DeleteOwnRecipeUseCase` y `RestoreOwnRecipeUseCase`, con sus
  pruebas (la receta vuelve con el mismo id, su `createdAt` y su ♡).
- [x] T018 **Confirmación** (CA-SAV-05-01, 05-02): `…/ui/own/DeleteOwnRecipeScreen.kt` con su ViewModel, la ruta
  `DeleteOwnRecipe` y las pruebas `DeleteOwnRecipeViewModelTest` y `DeleteOwnRecipeScreenTest`.
- [x] T019 **Aviso con cuenta regresiva** (CA-SAV-05-03 a 05-06, RNF-SAV-04):
  - `DeletedRecipeBanner.kt` sobre Guardadas (patrón `EditorResultHost`);
  - la cuenta de 8 a 1 y "Deshacer" en `SavedRecipesViewModel`;
  - pruebas con `advanceTimeBy` en `SavedRecipesViewModelTest` y casos en `SavedRecipesScreenTest`.
- [x] T020 **Contador** (CA-SAV-06-01, 06-02):
  - `CountSavedRecipesUseCase` y su prueba;
  - `ProfileViewModel` y `ProfileContract` reciben el número;
  - `ProfileScreen.kt` deja de usar `profile_stat_recipes_value` (se borra de `strings_profile.xml`);
  - actualiza `ProfileViewModelTest` y `ProfileScreenTest`.
- [x] T021 **Spec 008 y 009:**
  - en la 008, CA-PER-01-02 y la desviación de §9 apuntan a la 010;
  - en la 009, se cierra la desviación del ♡ y de Guardadas.

## Cierre (en el mismo PR)
- [ ] T022 Suite instrumentada completa y `rules-test` en verde.
- [ ] T023 Despliegue del bloque `recipes` de `firestore.rules` (con confirmación de Zhair). El bloque `favorites` se
  despliega al cerrar el #117, para poder probar el ♡ contra Firebase.
- [ ] T024 Checklist manual en el teléfono (abajo) y capturas Figma vs app en el PR.
- [ ] T025 Matriz al día, `tools/ca_coverage_baseline.txt`, `README.md` y `AGENTS.md` (arquitectura de Guardadas).

## Matriz de trazabilidad

Leyenda: **U** = unitaria, **I** = instrumentada, **R** = `rules-test`, **M** = manual.

| Criterio | Pruebas |
|---|---|
| CA-SAV-01-01 | I `RecipesDiscoverScreenTest`, I `SavedRecipesScreenTest` |
| CA-SAV-01-02 | I `RecipeDetailScreenTest` |
| CA-SAV-01-03 | U `ToggleFavoriteUseCaseTest`, U `RecipesDiscoverViewModelTest`, U `SavedRecipesViewModelTest` |
| CA-SAV-01-04 | U `ToggleFavoriteUseCaseTest` (PENDING), M |
| CA-SAV-01-05 | R, M |
| CA-SAV-02-01 | I `MainNavigationTest` |
| CA-SAV-02-02 | I `SavedRecipesScreenTest` |
| CA-SAV-02-03 | U `ObserveSavedRecipesUseCaseTest` |
| CA-SAV-02-04 | U `ObserveSavedRecipesUseCaseTest`, I `SavedRecipesScreenTest` |
| CA-SAV-02-05 | U `ObserveSavedRecipesUseCaseTest`, U `SavedRecipesViewModelTest`, I `SavedRecipesScreenTest` |
| CA-SAV-02-06 | U `SavedRecipesViewModelTest`, I `SavedRecipesScreenTest`, I `MainNavigationTest` |
| CA-SAV-02-07 | U `ObserveSavedRecipesUseCaseTest`, I `SavedRecipesScreenTest` |
| CA-SAV-02-08 | I `SavedRecipesScreenTest` |
| CA-SAV-02-09 | U `ObserveSavedRecipesUseCaseTest`, I `SavedRecipesScreenTest`, M |
| CA-SAV-02-10 | U `SavedRecipesViewModelTest`, I `SavedRecipesScreenTest`, I `MainNavigationTest` |
| CA-SAV-02-11 | U `SavedRecipesViewModelTest`, U `RecipesDiscoverViewModelTest`, U `RecipeDetailViewModelTest`, I `SavedRecipesScreenTest` |
| CA-SAV-03-01 | I `OwnRecipeEditorScreenTest` |
| CA-SAV-03-02 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeEditorScreenTest` |
| CA-SAV-03-03 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeEditorScreenTest` |
| CA-SAV-03-04 | U `SaveOwnRecipeUseCaseTest`, U `OwnRecipeEditorViewModelTest`, I `OwnRecipeSavedScreenTest` |
| CA-SAV-03-05 | U `ValidateOwnRecipeUseCaseTest`, R |
| CA-SAV-03-06 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeEditorScreenTest` |
| CA-SAV-03-07 | I `OwnRecipeSavedScreenTest`, I `MainNavigationTest` |
| CA-SAV-03-08 | U `SaveOwnRecipeUseCaseTest`, M |
| CA-SAV-03-09 | U `SaveOwnRecipeUseCaseTest` |
| CA-SAV-04-01 | I `OwnRecipeEditorScreenTest` |
| CA-SAV-04-02 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeSavedScreenTest` |
| CA-SAV-04-03 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeEditorScreenTest` |
| CA-SAV-04-04 | U `OwnRecipeEditorViewModelTest`, I `OwnRecipeEditorScreenTest`, I `MainNavigationTest` |
| CA-SAV-04-05 | I `OwnRecipeDetailScreenTest` |
| CA-SAV-05-01 | I `DeleteOwnRecipeScreenTest` |
| CA-SAV-05-02 | U `DeleteOwnRecipeViewModelTest`, I `DeleteOwnRecipeScreenTest`, I `MainNavigationTest` |
| CA-SAV-05-03 | U `DeleteOwnRecipeUseCaseTest`, U `SavedRecipesViewModelTest`, I `SavedRecipesScreenTest` |
| CA-SAV-05-04 | U `DeleteOwnRecipeUseCaseTest`, U `SavedRecipesViewModelTest`, I `SavedRecipesScreenTest` |
| CA-SAV-05-05 | U `SavedRecipesViewModelTest` |
| CA-SAV-05-06 | I `SavedRecipesScreenTest` |
| CA-SAV-06-01 | U `DeleteOwnRecipeUseCaseTest`, U `ProfileViewModelTest`, I `ProfileScreenTest` |
| CA-SAV-06-02 | U `DeleteOwnRecipeUseCaseTest`, M |
| RNF-SAV-01 | R |
| RNF-SAV-04 | I (semántica del ♡ y del aviso) |
| RNF-SAV-05 | M (capturas Figma vs app) |

### Checklist de verificación manual
1. Marcar y quitar ♡ en Recetas, en el detalle y en Guardadas; ver la misma favorita en otro teléfono.
2. Guardadas: los tres chips, búsqueda con y sin tildes, "Ordenar" por fecha y por nombre.
3. Crear una receta con dos ingredientes; ver "¡Receta guardada!", "Ver receta" y "Crear otra".
4. Editar con 0 min (aviso y rojo), corregir a 30 min y guardar; "Descartar cambios" no guarda.
5. Eliminar, deshacer antes de 8 s; eliminar otra vez y esperar los 8 s.
6. En modo avión: marcar ♡ y crear una receta; ver la tarjeta "Sin conexión"; al volver la red, desaparece.
7. El Perfil muestra el número correcto después de cada paso.
