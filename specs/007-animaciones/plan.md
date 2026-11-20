# Plan técnico 007: Animaciones, transiciones y efectos

> Cómo se cumple la [spec](./spec.md). Las mediciones de partida están en la spec §1.

## 1. Arquitectura

Todo el movimiento sale de un solo archivo de tokens en el design system. Las pantallas y los componentes no declaran
duraciones ni curvas: piden una especificación a esos tokens.

```
core/designsystem/theme/Motion.kt        ← tokens: duraciones, curvas y especificaciones listas (RF-ANI-01)
core/navigation/NavTransitions.kt        ← empuje de 300 ms y disolvencia de pestañas (RF-ANI-02)
core/designsystem/components/Inputs.kt   ← color del foco animado (CA-ANI-03-01)
core/designsystem/components/Dialogs.kt  ← entrada y salida de los diálogos (CA-ANI-04-02)
core/designsystem/components/SmartFoodBottomSheet.kt  ← hoja con tokens (CA-ANI-04-01), nuevo
feature/auth/ui/components/AuthScreenScaffold.kt      ← encabezado que sigue al teclado (CA-ANI-03-02/03)
feature/inventory/ui/…                   ← lista, búsqueda, editor, aviso de voz y resultados
```

## 2. Tokens de movimiento (`Motion.kt`)

```kotlin
object SmartFoodMotion {
    const val SHORT = 100     // pestañas (Figma: disolvencia de 100 ms)
    const val FAST = 150      // foco, ojo, checklist, errores en línea (Figma: Smart Animate de 150 ms)
    const val MEDIUM = 300    // navegación, hojas, diálogos, avisos, listas, chips y botones (Figma: 300 ms)

    val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    val EaseInOutBack = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)   // "ease-in-out-back" de CSS

    fun <T> navigation() = tween<T>(MEDIUM, easing = EaseInOut)
    fun <T> tab() = tween<T>(SHORT, easing = EaseOut)
    fun <T> focus() = tween<T>(FAST, easing = EaseOut)
    fun <T> standard() = tween<T>(MEDIUM, easing = EaseInOut)
    fun <T> loading() = tween<T>(MEDIUM, easing = EaseOut)               // "BotonCargando"
    fun <T> emphasizedEnter() = tween<T>(MEDIUM, easing = EaseInOutBack)  // solo la escala de los diálogos
}
```

- Las duraciones son `const` para que las pruebas avancen `mainClock` con los mismos valores.
- Compose ya multiplica las duraciones por la escala de animación del sistema (`MotionDurationScale`). Con escala 0, las
  animaciones terminan en el siguiente cuadro (CA-ANI-01-02). No se lee `Settings.Global` a mano.

## 3. Navegación (HU-ANI-02)

- `NavTransitions.PUSH_DURATION_MS` pasa de 600 a `SmartFoodMotion.MEDIUM` y la curva a `EaseInOut`. Sigue siendo un
  empuje completo (las dos pantallas se mueven), como el "Push" del Figma.
- Pestañas: hoy `EnterTransition.None`; pasan a `fadeIn(tab())` / `fadeOut(tab())`. `isTab()` suma `Route.Saved`, que
  hoy falta y hace que Guardadas entre con empuje (CA-ANI-02-03).
- `AppNavHost` no cambia: ya declara las transiciones una sola vez.
- **Primer cuadro:** en release no hay hueco (spec §1). Si al medir RNF-ANI-01 aparece un hueco > 50 ms en alguna
  pantalla, se agrega un Baseline Profile (`androidx.profileinstaller` y el módulo `:baselineprofile`) en una tarea
  aparte (T019), porque toca `libs.versions.toml` y `settings.gradle.kts`.

## 4. Campos y teclado (HU-ANI-03)

- `SmartFoodTextField`, `SmartFoodPasswordField`, `SmartFoodSelectorField` y `LineFrame` del editor: el color del borde
  y del ícono pasan por `animateColorAsState(target, focus())`.
- `PasswordFieldWithChecklist`: el `AnimatedVisibility` del popup usa `fadeIn/fadeOut(focus())`; el popup sigue
  anclado con `AbovePopupPositionProvider` (RNF-ANI-04).
- **Encabezado de marca (`AuthScreenScaffold`):** hoy decide "colapsado o no" con `WindowInsets.ime` en la fase de
  layout y salta. Pasa a una fracción continua:

  ```
  progreso = imeActual / imeObjetivo     // WindowInsets.ime y WindowInsets.imeAnimationTarget (API 30+)
  altoEncabezado = lerp(altoCompleto, altoColapsado, progreso)
  ```

  Como `WindowInsets.ime` ya avanza cuadro a cuadro con la animación del sistema, el encabezado y el panel la siguen
  sin duración propia. Se sigue leyendo en la fase de layout (sin recomponer cada cuadro). Antes de API 30 el inset
  llega en un solo paso (RNF-ANI-02).
- Editor y búsqueda: ya aplican el inset del teclado fuera del `verticalScroll`; solo se comprueba con la medición que
  el campo enfocado no salta (CA-ANI-03-05).

## 5. Diálogos y hojas (HU-ANI-04)

- **Diálogos (`Dialogs.kt`):** el contenido va dentro de un `AnimatedVisibility` con `MutableTransitionState`:
  - entrada: `fadeIn(standard()) + scaleIn(emphasizedEnter(), initialScale = 0.92f)`;
  - salida: `fadeOut(standard()) + scaleOut(standard(), targetScale = 0.92f)`;
  - el velo se dibuja en Compose con `fadeIn/fadeOut(standard())` y se quita el oscurecimiento de la ventana
    (`DialogWindowProvider` → `setDimAmount(0f)`), para que velo y diálogo vayan juntos;
  - al cerrar, el `Dialog` se quita de la composición cuando termina la salida (`transitionState.isIdle`), y entonces
    se llama a `onDismiss`. Los ViewModels no cambian.
- **Hojas:** `ModalBottomSheet` de Material 3 usa su propio resorte y no permite fijar la duración. Se crea
  `SmartFoodBottomSheet` en el design system (`components/BottomSheet.kt`):
  - API: `SmartFoodBottomSheet(onDismissRequest, containerColor, shape, content: ColumnScope.() -> Unit)`, la misma
    forma que usan hoy `OptionSheet`, `PhotoSourceSheet` e `IngredientSheet` (la hoja "Tema de la app" es una
    `OptionSheet`).
  - Ventana: un `Dialog` de pantalla completa (`usePlatformDefaultWidth = false`, `decorFitsSystemWindows = false`) sin
    oscurecimiento propio; el velo y la hoja se dibujan en Compose.
  - Entrada y salida: velo con `fadeIn/fadeOut(standard())` y hoja con `slideInVertically/slideOutVertically(standard())`
    sobre un `MutableTransitionState`; `onDismissRequest` se llama cuando termina la salida.
  - Cierre: tocar el velo, "atrás" (`onDismissRequest` del `Dialog`) o arrastrar la hoja hacia abajo más de un tercio
    de su alto (`draggable` vertical); si se suelta antes, vuelve a su sitio con `standard()`.
  - Barras: la ventana del `Dialog` pide los íconos de la barra de navegación según `LocalIsDarkTheme`, como
    `smartFoodSheetProperties()` de la spec 013, que deja de usarse.
  - Teclado: la hoja aplica `imePadding()` para la búsqueda de ingredientes.
- **Errores en línea (`AuthErrorMessage`, mensajes del editor):** `expandVertically + fadeIn` y
  `shrinkVertically + fadeOut`, todos con `focus()`.
- **Resultados del editor (`EditorResultHost`):** `AnimatedVisibility` con `fadeIn/fadeOut(standard())`.
- **Aviso de voz (`VoiceSearchOverlay`):** `AnimatedContent` entre estados con `standard()`. La animación de las ondas
  no cambia.

## 6. Listas, búsqueda y botones (HU-ANI-05)

- Lista (`InventoryScreen`): `Crossfade(standard())` entre `InventorySkeleton` y el contenido, y
  `Modifier.animateItem(fadeInSpec = standard(), placementSpec = standard(), fadeOutSpec = standard())` en las filas,
  con claves estables (`item.id`).
- Búsqueda: `animateItem` en los resultados y `animateColorAsState(standard())` en los chips de categoría.
- Botones con carga (`SmartFoodPrimaryButton` y equivalentes): `AnimatedContent(targetState = isLoading)` con
  `fadeIn/fadeOut(loading())`.
- `StepIndicator`: reemplaza su `spring` y su `tween(300)` por `standard()`.

## 6.1 Módulos de Perfil, Recetas + IA y Guardadas (HU-ANI-06)

- La navegación ya pasa por `NavTransitions`, así que el empuje de 300 ms les llega con T004.
- **IA (`AiFlowScreen`):** `AnimatedContent(targetState = step)` con `fadeIn/fadeOut(standard())` entre `Context`,
  `Loading`, `Plan` y `NoPlan` (CA-ANI-06-02).
- **Cocina guiada (`CookingScreens`):** `AnimatedContent` sobre el índice del paso con disolvencia de 300 ms, y la barra
  de progreso con `animateFloatAsState(standard())` (CA-ANI-06-03).
- **Rejillas de Recetas y Guardadas:** `Modifier.animateItem(standard())` con claves estables (id de la receta)
  (CA-ANI-06-04).
- **Avisos:** "Guardado" de Editar información y Preferencias, y `DeletedRecipeBanner` de Guardadas, con
  `AnimatedVisibility` y disolvencia de 300 ms (CA-ANI-04-04).
- **Chips:** `PreferenceChip`, `RecipeFilterPill`, `CategoryChips` y las píldoras del contexto de la IA animan su color
  con `animateColorAsState(standard())` (CA-ANI-05-03).

## 7. Decisiones (ADR)

| Decisión | Alternativas | Motivo |
|---|---|---|
| Duraciones y curvas del Figma (300/150/100 ms) | Material 3 Motion (emphasized, 400-500 ms); copiar Nequi | La constitución pide fidelidad con el Figma (HU-UI-07). Los 300 ms coinciden con las apps de referencia (270-370 ms). |
| Empuje completo, como el "Push" del Figma | Eje compartido de M3 (recorrido parcial + fundido) | Es lo que dibuja el prototipo; con 300 ms deja de verse pesado. |
| El encabezado sigue el inset del teclado | Animar el encabezado con su propia duración | Una duración propia nunca coincide con la del teclado de cada fabricante. |
| Hoja propia (`SmartFoodBottomSheet`) | `ModalBottomSheet` de Material 3 | El resorte de M3 no admite duración fija (CA-ANI-04-01). |
| Velo de los diálogos en Compose | Oscurecimiento de la ventana | El de la ventana aparece de golpe y no se sincroniza con el diálogo. |
| Fluidez medida en release | Medir en debug | En debug, Compose corre sin optimizar y pierde ~300 ms al iniciar cada pantalla (spec §1). |

## 8. Estrategia de pruebas

- **Unitarias:** `SmartFoodMotionTest` (duraciones y que cada curva va de 0 a 1 y es monótona salvo `EaseInOutBack`).
- **Instrumentadas** (con `mainClock.autoAdvance = false` y avanzando los tokens):
  - `AppNavigationTest`: a mitad del empuje (150 ms) se ven las dos pantallas; a los 300 ms, solo la nueva. Se ajusta la
    prueba actual de 600 ms (CA-03-04 de la spec 001).
  - `MainNavigationTest`: entre pestañas, a los 100 ms solo queda la nueva y no hubo empuje.
  - `SmartFoodFieldsTest`: el color del borde a 75 ms está entre el de reposo y el de foco (captura de píxel).
  - `SmartFoodDialogsTest` y una prueba de `SmartFoodBottomSheet`: el contenido aparece al terminar la entrada y
    `onDismiss` llega después de la salida.
  - `InventoryScreenTest`: el esqueleto da paso a la lista tras 300 ms.
  - Las pruebas de foco y adaptabilidad de la spec 002 (`FormFocusTest`, `AdaptiveLayoutTest`) siguen en verde.
- **Medición (RNF-ANI-01):** `tools/medir_fluidez.py` toma `dumpsys gfxinfo framestats` de una transición y reporta el
  primer cuadro, el hueco más largo y el porcentaje de cuadros lentos. Se corre con el build release en el V2436 y el
  resultado va al PR.
- **Manual:** checklist en `tasks.md`, con `screenrecord` y "Quitar animaciones" activado y desactivado.
