# Plan técnico 013: Modo oscuro legible

> Cómo se cumple la [spec](./spec.md). Ruta base: `app/src/main/java/com/smartfood/app/` (abreviada `…/`).

## 1. Arquitectura

El color de cada elemento sale de un **rol**, nunca de un token fijo:

```
Color.kt (tokens, sin cambios) → Theme.kt (roles de Material) + SmartFoodColors.kt (roles propios) → pantallas
```

- **Roles de Material:** sirven cuando el elemento coincide con uno (`primary`, `surfaceVariant`, `primaryContainer`…).
- **Roles propios (`SmartFoodColors`):** cubren lo que Material no distingue. Por ejemplo, el encabezado es `primary` en
  claro pero no en oscuro, y `GreenCheck` es un relleno distinto de `primary`.
- Las pantallas leen `MaterialTheme.smartFoodColors`, como `MaterialTheme.spacing`, sin preguntar por el modo.

## 2. Contratos

### 2.1 Roles propios

```kotlin
// …/core/designsystem/theme/SmartFoodColors.kt
@Immutable
data class SmartFoodColors(
    val header: Color,            // encabezados, tarjeta "¿Qué cocinamos hoy?", píldora de precios
    val onHeader: Color,          // texto e íconos sobre header
    val onHeaderVariant: Color,   // correo, círculo del avatar y su borde sobre header
    val checkedFill: Color,       // GreenCheck: Guardar de hojas y avisos, chip marcado de Preferencias
    val onCheckedFill: Color,
    val softFill: Color,          // buscadores y chips sin seleccionar (SurfaceLight)
    val divider: Color,           // bordes y divisores grises (GreyDivider)
    val greenSoft: Color,         // fotos vacías, tarjetas verdes suaves (GreenSoft)
    val highlight: Color,         // tarjetas amarillas (YellowSoft)
    val onHighlight: Color,
    val tag: Color,               // fondo de la etiqueta de categoría de la lista
    val onTag: Color,
    val greenIcon: Color,         // íconos dentro de primaryContainer
    val veil: Color,              // velo del aviso de voz
)

val LightSmartFoodColors: SmartFoodColors
val DarkSmartFoodColors: SmartFoodColors
val LocalSmartFoodColors: ProvidableCompositionLocal<SmartFoodColors>   // por defecto, LightSmartFoodColors

val MaterialTheme.smartFoodColors: SmartFoodColors   // @Composable @ReadOnlyComposable get()
```

| Rol | Claro (igual que hoy) | Oscuro |
|---|---|---|
| `header` / `onHeader` | `GreenDark` / `White` | `GreenDark` / `White` |
| `onHeaderVariant` | `GreenLight` | `GreenLight` |
| `checkedFill` / `onCheckedFill` | `GreenCheck` / `White` | `GreenMain` / `GreenDark2` |
| `softFill` | `SurfaceLight` | `InputDark` |
| `divider` | `GreyDivider` | `BorderDark` |
| `greenSoft` | `GreenSoft` | `InputDark` |
| `highlight` / `onHighlight` | `YellowSoft` / `GreenDark` | `InputDark` / `White` |
| `tag` / `onTag` | `GreenMain20` / `GreenCheck` | `GreenDark` / `GreenMain` |
| `greenIcon` | `GreenCheck` | `GreenMain` |
| `veil` | `White` al 35 % | `Black` al 35 % |

### 2.2 Esquema oscuro de Material

| Rol | Antes | Ahora |
|---|---|---|
| `onPrimary` | `White` | `GreenDark2` |
| `primaryContainer` | `GreenLight` | `GreenDark` |
| `onPrimaryContainer` | `GreenDark` | `GreenMain` |

`LightColorScheme` no cambia.

### 2.3 Barras del sistema

- `SystemBarEffect` conserva un solo efecto con el fondo como única clave y pide la barra de navegación según el tema
  vigente (`rememberUpdatedState`) en el mismo `enableEdgeToEdge`. Con el tema también en la clave, la lectura del tema
  oscuro de DataStore repetía el efecto de la Splash durante la transición y dejaba íconos oscuros sobre el Login
  (visto en el V2436 el 2026-11-19).
- `SmartFoodTopBar`, `RecipesHeader` y la barra del Inventario llaman `SystemBarEffect(isLightBackground = false)`
  (CA-OSC-04-03); antes, las pestañas heredaban los íconos oscuros de la Splash.
- **Limitación del V2436 (vivo):** tras un arranque en frío, el teléfono mantiene los íconos oscuros de la pantalla
  de arranque aunque la app pida blancos; el controlador de la ventana confirma la petición (`isAppearanceLightStatusBars
  = false`) y aun así se pintan oscuros. Cuando la actividad se recrea, sí la respeta. También ocurre en `master`. Se
  probaron, sin éxito, `windowLightStatusBar = false` en el tema XML y quitar la petición de la Splash; los dos se
  revirtieron.
- *Reemplazado por la spec 007 (T009): las hojas usan `SmartFoodBottomSheet`, cuya ventana pide las barras según el
  tema, y `SheetProperties.kt` se borró.* Antes: las tres hojas (`OptionSheet`, `PhotoSourceSheet` e `IngredientSheet`) pasaban `smartFoodSheetProperties()`
  (`core/designsystem/util/SheetProperties.kt`): `ModalBottomSheetProperties` con las dos barras en `!dark`. Sin
  ellas, Material 3 1.4.0 deduce las barras del color del contenido, y `TextMuted` (oscuro) cuenta como "oscuro":
  por eso la barra de navegación quedaba clara. En claro el valor es el mismo de antes.

## 3. Cambios por archivo

| Archivo | Cambio |
|---|---|
| `core/designsystem/theme/Theme.kt` | §2.2 y proveer `LocalSmartFoodColors`. |
| `core/designsystem/components/TopBar.kt`, `feature/recipes/ui/components/RecipesHeader.kt` | `primary`/`onPrimary` → `header`/`onHeader`. |
| `feature/inventory/ui/list/InventoryScreen.kt` | La barra superior a `header`; el FAB sigue en `primary`. La etiqueta de categoría (en `InventoryItemRow.kt`) a `tag`/`onTag`. |
| `feature/profile/ui/ProfileScreen.kt` | Encabezado a `header`; avatar, borde y correo a `onHeaderVariant`; iniciales en `header`; íconos de las filas en `greenIcon`. |
| `feature/recipes/ui/discover/RecipesDiscoverScreen.kt` | Tarjeta "¿Qué cocinamos hoy?" a `header`/`onHeader`; el cuerpo sigue en `GreenSoft` (ADR-OSC-03). |
| `feature/recipes/ui/detail/RecipeDetailScreen.kt` | Píldora de precios a `header`; `GreyDivider` → `divider`. |
| Acciones de texto en la barra (`AiFlowScreen` "Ayuda", `CookingScreens` "Salir", `SavedRecipesScreen` "Ordenar", `BarTextAction`) | `onPrimary` → `onHeader`. |
| `Buttons.kt` (variante verde), `Dialogs.kt` (Guardar), `OptionSheet.kt` (Guardar), `PreferenceChip.kt` | `GreenCheck`/`White` → `checkedFill`/`onCheckedFill`. |
| `OptionSheet.kt` | `SheetButton` recibe su `contentColor`: antes usaba `onPrimary` también en "Cancelar" (naranja), que con el nuevo `onPrimary` oscuro quedaría oscuro. El radio pasa de `GreenCheck` a `greenIcon`. |
| `RecipeSearchField.kt`, `InventorySearchScreen.kt`, `CategoryChips.kt` | `SurfaceLight` → `softFill`; `GreyDivider` → `divider`. |
| `RecipeFilterPill.kt`, `AiFlowScreen.kt`, `Dialogs.kt` | `GreyDivider` → `divider`. |
| `AiFlowScreen.kt`, `AiHelpScreen.kt`, `CookingScreens.kt`, `SavedRecipeCard.kt` | `if (LocalIsDarkTheme) … else YellowSoft/GreenSoft` → `highlight`/`greenSoft`. |
| `OwnRecipeDetailScreen.kt`, `OwnRecipeSavedScreen.kt`, `SavedRecipesScreen.kt`, `ProfileAvatar.kt` | `GreenSoft` → `greenSoft`. |
| `PhotoSourceSheet.kt` | Ícono `GreenCheck` → `greenIcon`; propiedades de la hoja (§2.3). |
| `PhotoSlot.kt` | `SlotButton` recibe su `contentColor`: la basura (roja) usa `onError` y la cámara `onPrimary`; antes las dos usaban `onPrimary`. |
| `PasswordRequirementChecklist.kt` | `White` → `surfaceVariant` (blanco en claro, `InputDark` en oscuro). |
| `VoiceSearchOverlay.kt` | Velo → `veil`. |
| `SystemBarEffect.kt`, `IngredientSheet.kt` | §2.3. |

## 4. Decisiones

| Código | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| ADR-OSC-01 | Roles propios en un `CompositionLocal` | Solo roles de Material | Material no separa "encabezado" de "relleno de acción"; sin roles propios quedarían `if (dark)` en las pantallas. |
| ADR-OSC-02 | Cambiar `onPrimary` oscuro a `GreenDark2` | Cambiar `primary` oscuro a `GreenDark` | `primary` también colorea títulos y textos verdes, que en oscuro se leen bien con `GreenMain`. |
| ADR-OSC-03 | El texto del cuerpo de "¿Qué cocinamos hoy?" sigue en `GreenSoft` | Rol propio | La tarjeta usa `header`, que es `GreenDark` en los dos modos; `GreenSoft` sobre ella llega a ~8:1. |
| ADR-OSC-04 | La Splash queda en `GreenLight` | Splash oscura | La pantalla de arranque del sistema es clara (`values-v31`) y CA-UI-05-02 exige que empalmen. |

## 5. Pruebas

- **Unitaria `SmartFoodColorsContrastTest`** (`app/src/test/…/core/designsystem/`): calcula la luminancia relativa
  WCAG 2.1 de cada par (relleno, contenido) en claro y en oscuro.
  - En oscuro, los textos llegan a 4,5:1 y los íconos y bordes a 3:1 (CA-OSC-01-01 y 01-02).
  - En claro, cada rol es igual al token anterior (CA-OSC-01-03).
  - Los esquemas de Material se leen de `LightColorScheme` y `DarkColorScheme`, que pasan a `internal` en `Theme.kt`.
- **Instrumentada `ColorModeTest`:** se agregan casos que dibujan `SmartFoodTopBar`, `RecipeSearchField` y
  `RecipeFilterPill` con `ThemeMode.DARK` y comprueban el color de píxel (CA-OSC-02-01, 02-03 y 03-01).
- **Manual:** las mismas 16 pantallas por adb en el V2436, antes y después, en el PR.
