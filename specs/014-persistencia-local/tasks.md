# Tareas 014: Persistencia local con Room

> Rama `feature/014-persistencia-local`, un commit por tarea. Contratos en [`plan.md`](./plan.md). Las pruebas se
> escriben con cada tarea y se corren en la ronda final de los pendientes (PR #138).

- [x] T001 La spec, el plan y las tareas; la fila 10 de `docs/pendientes.md`.
- [x] T002 **Base local** (RF-LOC-01, RNF-LOC-01): dependencias de Room en `libs.versions.toml` y `app/build.gradle.kts`,
  `SmartFoodDatabase`, `CookingEntity`, `RecentSearchEntity`, `CookingDao`, `RecentSearchDao`, `DatabaseModule`,
  `@ApplicationScope` y el esquema en `app/schemas/`; `CookingDaoTest` y `RecentSearchDaoTest`.
- [x] T003 **Historial sin conexión** (HU-LOC-01): `CachedCookingHistoryRepository`, `RecipesDataModule`,
  `LocalUserData` y `LogoutUseCase`; `CachedCookingHistoryRepositoryTest` y `SessionUseCasesTest` (`LogoutUseCase`).
- [x] T004 **Dominio de las búsquedas recientes** (RF-LOC-03): `SearchScope`,
  `RecentSearchesRepository`, los tres casos de uso y `RoomRecentSearchesRepository`; `RecentSearchUseCasesTest`.
- [x] T005 **Recientes en Descubrir** (CA-LOC-02-01, 02-02, 02-04 a 02-07): `SmartFoodRecentSearches`,
  `strings_search.xml`, `RecipeSearchField`, contrato, ViewModel y pantalla; `RecipesDiscoverViewModelTest`,
  `RecentSearchesTest` y `RecipesDiscoverScreenTest`.
- [x] T006 **Recientes en el Inventario** (CA-LOC-02-01 a 02-07): contrato, ViewModel (también la voz) y pantalla;
  `InventorySearchViewModelTest` e `InventorySearchScreenTest`.
- [ ] T007 **Documentación**: `AGENTS.md` (arquitectura y lista de specs), `README.md`, matriz al día,
  `tools/ca_coverage.py` y su línea base, y `docs/pendientes.md`.
- [ ] T008 **Cierre** (ronda final): unitarias, `assembleDebug`, `lintDebug`, instrumentadas de las clases tocadas,
  APK release en el V2436 y checklist manual (abajo), con capturas por adb en claro y oscuro.

## Matriz de trazabilidad

Leyenda: **U** = unitaria, **I** = instrumentada, **M** = manual.

| Criterio | Pruebas |
|---|---|
| CA-LOC-01-01 | U `CachedCookingHistoryRepositoryTest`, I `CookingDaoTest`, M |
| CA-LOC-01-02 | U `CachedCookingHistoryRepositoryTest`, I `CookingDaoTest` |
| CA-LOC-01-03 | U `CachedCookingHistoryRepositoryTest` |
| CA-LOC-01-04 | U `CachedCookingHistoryRepositoryTest` |
| CA-LOC-01-05 | U `CachedCookingHistoryRepositoryTest`, U `SessionUseCasesTest`, I `CookingDaoTest`, M |
| CA-LOC-02-01 | U `RecentSearchUseCasesTest`, I `RecentSearchesTest`, I `RecipesDiscoverScreenTest`, I `InventorySearchScreenTest` |
| CA-LOC-02-02 | U `RecentSearchUseCasesTest`, U `RecipesDiscoverViewModelTest`, U `InventorySearchViewModelTest` |
| CA-LOC-02-03 | U `InventorySearchViewModelTest` |
| CA-LOC-02-04 | U `RecentSearchUseCasesTest`, I `RecentSearchDaoTest` |
| CA-LOC-02-05 | U `RecipesDiscoverViewModelTest`, U `InventorySearchViewModelTest`, I `RecentSearchesTest` |
| CA-LOC-02-06 | U `RecentSearchUseCasesTest`, I `RecentSearchDaoTest`, I `RecentSearchesTest` |
| CA-LOC-02-07 | I `RecipesDiscoverScreenTest`, I `InventorySearchScreenTest`, I `RecentSearchDaoTest` |
| CA-LOC-02-08 | U `SessionUseCasesTest`, I `RecentSearchDaoTest`, M |

### Checklist de verificación manual
1. Cocinar una receta; activar el modo avión, cerrar la app por completo y abrir el Perfil: «Ahorrados este mes» muestra el valor.
2. Buscar «pollo» en Recetas y tocar un resultado; volver, tocar la barra vacía: aparece «pollo» en «Recientes».
3. Buscar «Pollo» otra vez: no se duplica. Buscar por voz en el Inventario: aparece lo dictado.
4. «Borrar» en Recetas no borra las del Inventario.
5. Cerrar sesión e iniciar con otra cuenta: no aparecen las búsquedas ni el ahorro de la anterior.
6. Repetir en modo oscuro y con el APK release.
