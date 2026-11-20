# Tareas 007: Animaciones, transiciones y efectos

> Derivadas de [`plan.md`](./plan.md). Rama `feature/007-animaciones`, un commit por tarea. Las pruebas y la medición
> se escriben ahora y se corren en la ronda final de los 10 puntos (PR #138).

## Fase 0: Preparación
- [x] T001 La spec, el plan y las tareas, extendidas a Perfil, Recetas + IA y Guardadas (HU-ANI-06). Enmiendas: RNF-06 de la spec 001 (300 ms), `AGENTS.md`, `docs/pendientes.md` y `tools/ca_coverage.py` (área `ANI`).

## Fase 1: Tokens y medición
- [x] T002 `core/designsystem/theme/Motion.kt` con duraciones, curvas y especificaciones. Prueba `SmartFoodMotionTest` (RF-ANI-01, CA-ANI-01-01).
- [x] T003 `tools/medir_fluidez.py`: primer cuadro, hueco más largo y cuadros lentos de una transición con `gfxinfo framestats` (RNF-ANI-01).

## Fase 2: Navegación
- [x] T004 `NavTransitions`: empuje de 300 ms ease-in-out y disolvencia de 100 ms entre las cuatro pestañas (Guardadas incluida). Ajustar `AppNavigationTest` y `MainNavigationTest` (CA-ANI-02-01 a 02-05, CA-ANI-06-01).

## Fase 3: Campos y teclado
- [x] T005 Color del foco animado en `Inputs.kt`, `LineFrame` del editor y el editor de receta propia; ojo y checklist de contraseña con 150 ms (CA-ANI-03-01, 03-04).
- [x] T006 `AuthScreenScaffold`: encabezado de marca que sigue al teclado (CA-ANI-03-02, 03-03, RNF-ANI-02, RNF-ANI-04).
- [ ] T007 Editor de producto, búsqueda y editor de receta propia: el campo enfocado no salta al abrir el teclado (CA-ANI-03-05).

## Fase 4: Diálogos, hojas y avisos
- [x] T008 `Dialogs.kt`: entrada y salida animadas con velo en Compose; `onDismiss` al terminar la salida (CA-ANI-04-02).
- [x] T009 `SmartFoodBottomSheet`; `OptionSheet`, `PhotoSourceSheet` e `IngredientSheet` lo usan (CA-ANI-04-01).
- [ ] T010 Errores en línea de formularios y editores con 150 ms (CA-ANI-04-03).
- [ ] T011 Avisos de resultado: editor, "Guardado" del Perfil y deshacer de Guardadas con 300 ms (CA-ANI-04-04).
- [ ] T012 `VoiceSearchOverlay`: cambios de estado con 300 ms (CA-ANI-04-05).

## Fase 5: Listas, chips, botones y recetas
- [ ] T013 Lista del inventario: esqueleto → contenido con `Crossfade` y filas con `animateItem` (CA-ANI-05-01, 05-02).
- [ ] T014 Búsqueda del inventario y chips de inventario, Recetas, Guardadas, Preferencias e IA con 300 ms (CA-ANI-05-02, 05-03).
- [ ] T015 Botones con estado de carga e indicador de pasos con los tokens (CA-ANI-05-04, 05-05).
- [ ] T016 IA, cocina guiada y rejillas de Recetas y Guardadas (CA-ANI-06-02 a 06-04).

## Fase 6: Cierre (ronda final)
- [ ] T017 Medición de RNF-ANI-01 con el build release en el V2436 (`tools/medir_fluidez.py`); tabla en el PR.
- [ ] T018 Checklist manual, con y sin "Quitar animaciones" (CA-ANI-01-02, CA-ANI-01-03).
- [ ] T019 Pruebas instrumentadas de las pantallas tocadas y matriz al día.
- [ ] T020 (Solo si T017 encuentra un hueco > 50 ms) Baseline Profile con `androidx.profileinstaller` y el módulo `:baselineprofile` (RNF-ANI-01).

## Matriz de trazabilidad

Leyenda: **U** = unitaria, **I** = instrumentada, **Med** = medición con `tools/medir_fluidez.py` en release,
**M** = verificación manual (checklist).

| Criterio | Pruebas |
|---|---|
| CA-ANI-01-01 | U `SmartFoodMotionTest`, revisión de código (sin duraciones sueltas) |
| CA-ANI-01-02 | M (escala de animación 0) |
| CA-ANI-01-03 | Med, M |
| CA-ANI-02-01, 02-02, 02-04 | I `AppNavigationTest`, Med |
| CA-ANI-02-03 | I `MainNavigationTest`, Med |
| CA-ANI-02-05 | M |
| CA-ANI-03-01, 03-04 | I `SmartFoodFieldsTest` |
| CA-ANI-03-02, 03-03, 03-05 | Med, M |
| CA-ANI-04-01 | I `SmartFoodBottomSheetTest` |
| CA-ANI-04-02 | I `SmartFoodDialogsTest` |
| CA-ANI-04-03 a 04-05 | M |
| CA-ANI-05-01 | I `InventoryScreenTest` |
| CA-ANI-05-02 a 05-05 | M, Med |
| CA-ANI-06-01 | I `AppNavigationTest`, M |
| CA-ANI-06-02 a 06-04 | M, Med |
| RNF-ANI-01 | Med |
| RNF-ANI-02 | M (emulador API 24-29) |
| RNF-ANI-03 | M, I (pruebas existentes de `liveRegion`) |
| RNF-ANI-04 | I `FormFocusTest`, I `AdaptiveLayoutTest` |

### Checklist de verificación manual
1. Con el build release, entrar y volver de Registro, Recuperación, Términos, editor, búsqueda, cámara, Editar información, detalle de receta, IA, cocina guiada y receta propia: empuje continuo de ~300 ms.
2. Cambiar entre las cuatro pestañas: disolvencia corta, sin empuje.
3. Enfocar y desenfocar campos de Login, Registro, editor y editor de receta propia: el borde cambia de color con suavidad.
4. Abrir y cerrar el teclado en Login y Registro: el encabezado se colapsa y vuelve al ritmo del teclado, sin salto.
5. Abrir y cerrar las hojas (categoría, foto, ingrediente, tema) por botón, velo, arrastre y "atrás", y los diálogos de eliminar y cerrar sesión: velo y contenido entran y salen juntos.
6. Guardar un producto, el perfil y las preferencias, y borrar una receta propia: los avisos aparecen y se van con disolvencia.
7. Agregar y borrar un producto, y filtrar Recetas y Guardadas: las tarjetas y filas se reacomodan sin saltos.
8. Generar un plan con la IA y recorrer la cocina guiada: los pasos cambian con disolvencia.
9. Activar "Quitar animaciones" y repetir 1, 4 y 5: todo cambia al instante y queda igual que con animaciones.
