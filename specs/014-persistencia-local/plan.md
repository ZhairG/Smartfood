# Plan técnico 014: Persistencia local con Room

> Contratos de la [spec 014](./spec.md). Tareas en [`tasks.md`](./tasks.md).

## 1. Arquitectura

```
core/database/                     Room: base, entidades, DAO, LocalUserData y DatabaseModule (Hilt)
core/search/domain/                RecentSearch, SearchScope, RecentSearchesRepository y casos de uso
core/search/data/                  RoomRecentSearchesRepository
core/designsystem/components/      SmartFoodRecentSearches (chips «Recientes»)
feature/recipes/data/repository/   CachedCookingHistoryRepository (Room + Firestore)
```

`domain` sigue siendo Kotlin puro: Room solo aparece en `core/database` y en las clases `data`.

## 2. Base de datos

- `SmartFoodDatabase` (`@Database(version = 1, exportSchema = true)`), con las entidades `CookingEntity` y
  `RecentSearchEntity`. El esquema se exporta con el argumento de KSP `room.schemaLocation = $projectDir/schemas`.
- `CookingEntity(id PK, uid, recipeId, servings, cookedAt, rescuedValueCop)`, con índice por `uid`.
- `RecentSearchEntity(uid, scope, normalized, text, usedAt)`, con clave primaria `(uid, scope, normalized)`.
- `CookingDao`:
  - `observe(uid): Flow<List<CookingEntity>>`, ordenado por `cookedAt` descendente;
  - `upsert(entity)`;
  - `replaceAll(uid, entities)` en `@Transaction`;
  - `deleteAll()`.
- `RecentSearchDao`:
  - `observe(uid, scope, limit): Flow<List<RecentSearchEntity>>`, ordenado por `usedAt` descendente;
  - `upsert(entity)`;
  - `trim(uid, scope, keep)`;
  - `clear(uid, scope)`;
  - `deleteAll()`.
- `DatabaseModule` provee la base (`Room.databaseBuilder(..., "smartfood.db")`), los dos DAO y `LocalUserData`.
- `CoreDataModule` agrega `@ApplicationScope CoroutineScope` (`SupervisorJob() + Dispatchers.IO`).

## 3. Historial con caché (HU-LOC-01)

```kotlin
class CachedCookingHistoryRepository @Inject constructor(
    private val remote: FirestoreCookingHistoryRepository,
    private val dao: CookingDao,
    private val currentUid: CurrentUid,   // () -> String?, sale de FirebaseAuth
) : CookingHistoryRepository
```

- `observeHistory()`:
  - sin sesión, emite una lista vacía;
  - con sesión, hace un `channelFlow`: en un hijo, `remote.observeHistory()` → `dao.replaceAll(uid, …)`, y si falla
    lo atrapa (CA-LOC-01-04); en el principal, `dao.observe(uid)` → modelos.
- `saveRecord(record)`: `remote.saveRecord(record)`; si no falla, `dao.upsert(...)` (CA-LOC-01-03). El `WriteOutcome`
  no cambia.
- `newRecordId()` delega en Firestore.
- `RecipesDataModule` enlaza `CookingHistoryRepository` con esta clase.

## 4. Búsquedas recientes (HU-LOC-02)

```kotlin
enum class SearchScope { RECIPES, INVENTORY }
data class RecentSearch(val text: String, val usedAt: Long)

interface RecentSearchesRepository {
    fun observe(scope: SearchScope, limit: Int): Flow<List<RecentSearch>>
    suspend fun record(scope: SearchScope, text: String, normalized: String, usedAt: Long, keep: Int)
    suspend fun clear(scope: SearchScope)
}

class RecordRecentSearchUseCase(repository, clock) {
    suspend operator fun invoke(scope: SearchScope, text: String)   // valida y normaliza (CA-LOC-02-02, 02-04)
}
class ObserveRecentSearchesUseCase(repository) { operator fun invoke(scope): Flow<List<String>> } // 5 (CA-LOC-02-01)
class ClearRecentSearchesUseCase(repository) { suspend operator fun invoke(scope) }              // CA-LOC-02-06
```

- **Validación (solo en el caso de uso):** `text.trim()` con al menos 2 letras. La normalización es la de
  `SearchInventoryUseCase.normalize` (minúsculas y sin tildes) y es la clave para no duplicar. `MAX_KEPT = 10` y
  `MAX_SHOWN = 5`.
- `RoomRecentSearchesRepository` usa el `uid` con sesión y, sin sesión, no hace nada.

## 5. UI

- `SmartFoodRecentSearches(items, onItemClick, onClear, modifier)`:
  - título «Recientes» y «Borrar» en una fila, con los chips debajo en un `FlowRow`;
  - chip con el estilo de `CategoryChip` (borde, radio, Poppins), sin ícono;
  - colores por rol de `MaterialTheme`.
- **Descubrir:**
  - `RecipesDiscoverUiState.recentSearches`;
  - eventos `SearchSubmitted`, `RecentClicked(text)` y `ClearRecents`;
  - `RecipeClicked` guarda la búsqueda si había texto;
  - `RecipeSearchField` suma `onSearch` (`ImeAction.Search`) y un estado de foco;
  - la pantalla muestra el componente si la barra tiene foco y `query.isEmpty()`.
- **Inventario:** lo mismo en `InventorySearchUiState`, `InventorySearchEvent` y la barra de `InventorySearchScreen`.
  `applySpokenIngredients` guarda el texto que llena la barra (CA-LOC-02-03).

## 6. Cierre de sesión

```kotlin
interface LocalUserData { fun clearAll() }   // core/database; RoomLocalUserData lanza deleteAll() en el ApplicationScope
```

`LogoutUseCase` llama a `localUserData.clearAll()` junto a `photoSync.clearAll()` (CA-LOC-01-05, CA-LOC-02-08).

## 7. Decisiones (ADR)

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Firestore es la fuente de verdad del historial y Room es caché | Room primero con un Worker que sube a Firestore | Firestore ya tiene cola sin conexión; no se duplica ni se toca la escritura del #141. |
| `uid` en cada fila y borrado al salir | Solo borrar al salir | Si el borrado falla, otra cuenta igual no ve datos ajenos. |
| Base y DAO en `core/database` | Una base por feature | Room exige declarar todas las entidades en una sola `@Database`. |
| Chips sin ícono | Ícono `History` de Material | El Figma no tiene ese ícono y AGENTS.md pide los del Figma. |

## 8. Estrategia de pruebas

- **Instrumentadas:**
  - `CookingDaoTest` y `RecentSearchDaoTest` con `Room.inMemoryDatabaseBuilder`: filtrado por `uid`, orden,
    `replaceAll`, `trim` y `clear` por ámbito;
  - `RecentSearchesTest` (el componente);
  - `RecipesDiscoverScreenTest` e `InventorySearchScreenTest` (CA-LOC-02-01, 02-05 a 02-07).
- **Unitarias**, con fakes en `sharedTest/testutil/LocalDataFakes.kt` (`FakeCookingDao`, `FakeRecentSearchesRepository`
  y `FakeLocalUserData`):
  - `CachedCookingHistoryRepositoryTest`;
  - `RecentSearchUseCasesTest`;
  - `RecipesDiscoverViewModelTest`;
  - `InventorySearchViewModelTest`;
  - `LogoutUseCaseTest`.
