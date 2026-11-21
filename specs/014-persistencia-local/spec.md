# Spec 014: Persistencia local con Room

| Campo | Valor |
|---|---|
| Feature | `014-persistencia-local` |
| Estado | En implementación (rama `feature/014-persistencia-local`) |
| Fecha | 2026-11-20 |
| Depende de | 006-inventario, 008-perfil, 009-recetas-ia |
| Afecta | «Ahorrados este mes» del Perfil, búsqueda de Recetas (Descubrir), búsqueda del Inventario, cierre de sesión |
| Origen | Punto 10 de [`docs/pendientes.md`](../../docs/pendientes.md): informe de Aplicaciones Móviles (unidad 6) |

> Los códigos llevan el prefijo `LOC` (`HU-LOC-01`, `CA-LOC-01-01`).

## 1. Contexto y motivación

La app guarda datos en DataStore («Recuérdame», tema), en archivos internos (fotos pendientes) y en la caché sin
conexión de Firestore, pero no usa SQL. El informe de Aplicaciones Móviles pidió un caso pertinente de Room, con
entidad, DAO y pruebas. Zhair eligió dos (2026-11-20):

1. **Caché del historial de cocina.** «Ahorrados este mes» del Perfil depende de `users/{uid}/cookings`. Con Room, el
   valor sale del teléfono: se ve sin red y al abrir la app en frío, sin esperar a Firestore. Firestore sigue siendo la
   fuente de verdad.
2. **Búsquedas recientes** en las dos búsquedas con barra (Recetas e Inventario), incluida la voz del Inventario.

## 2. Actores

- **Usuario con sesión:** busca recetas y productos, y mira su ahorro en el Perfil.

## 3. Historias de usuario y criterios de aceptación

### HU-LOC-01: Ver mi ahorro sin conexión
**Como** usuario **quiero** ver «Ahorrados este mes» aunque no tenga red **para** saber cuánto he rescatado en cualquier momento.

| Código | Criterio |
|---|---|
| CA-LOC-01-01 | **Dado** que cociné recetas este mes, **cuando** abro el Perfil sin conexión (también con la app abierta en frío), **entonces** «Ahorrados este mes» muestra el valor guardado en el teléfono. |
| CA-LOC-01-02 | **Dado** que el historial cambió en Firestore (por ejemplo, desde otro teléfono), **cuando** hay red, **entonces** el teléfono reemplaza su copia y el valor se actualiza. |
| CA-LOC-01-03 | **Dado** que termino de cocinar, con o sin conexión, **entonces** el registro queda en el teléfono enseguida y «Ahorrados este mes» lo suma. La escritura no cambia: el registro va en el lote del cierre de la cocina (spec 009, CA-RIA-04-09) y llega a la copia con el snapshot local de Firestore. |
| CA-LOC-01-04 | **Dado** que la lectura de Firestore falla, **entonces** la app sigue mostrando lo guardado en el teléfono y no se cierra. |
| CA-LOC-01-05 | **Dado** que cierro sesión, **entonces** el historial guardado en el teléfono se borra, y otra cuenta en el mismo teléfono nunca ve el historial ajeno. |

### HU-LOC-02: Repetir una búsqueda reciente
**Como** usuario **quiero** ver mis últimas búsquedas **para** repetirlas con un toque.

| Código | Criterio |
|---|---|
| CA-LOC-02-01 | **Dado** la barra de búsqueda de Recetas o del Inventario enfocada y vacía, con búsquedas guardadas, **entonces** veo «Recientes» con hasta 5 chips de texto, la más reciente primero, y «Borrar». Sin búsquedas guardadas, no aparece nada. |
| CA-LOC-02-02 | **Dado** que escribí al menos 2 letras, **cuando** toco un resultado o pulso «Buscar» en el teclado, **entonces** el texto se guarda como búsqueda reciente. No se guarda cada letra que escribo. |
| CA-LOC-02-03 | **Dado** una búsqueda por voz del Inventario que se entendió, **entonces** el texto que llena la barra («carne, pollo») se guarda como búsqueda reciente. |
| CA-LOC-02-04 | **Dado** que repito una búsqueda, aunque cambien mayúsculas o tildes, **entonces** no se duplica: sube al primer lugar. Se conservan las 10 últimas de cada búsqueda. |
| CA-LOC-02-05 | **Dado** «Recientes», **cuando** toco un chip, **entonces** la barra se llena con ese texto y veo sus resultados. |
| CA-LOC-02-06 | **Dado** «Recientes», **cuando** toco «Borrar», **entonces** se quitan las búsquedas recientes de esa barra, no las de la otra. |
| CA-LOC-02-07 | **Dado** que empiezo a escribir, **entonces** «Recientes» se oculta. Las búsquedas de Recetas y del Inventario son independientes. |
| CA-LOC-02-08 | **Dado** que cierro sesión, **entonces** las búsquedas recientes se borran, y otra cuenta en el mismo teléfono no las ve. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-LOC-01 | Base Room `SmartFoodDatabase` con las tablas `cookings` y `recent_searches`, cada fila con el `uid` de su dueño. | HU-LOC-01, 02 |
| RF-LOC-02 | El historial de cocina se lee de Room y se sincroniza con los snapshots de Firestore. | HU-LOC-01 |
| RF-LOC-03 | Búsquedas recientes por ámbito (`RECIPES`, `INVENTORY`), normalizadas sin mayúsculas ni tildes, con un máximo de 10. | HU-LOC-02 |
| RF-LOC-04 | Al cerrar sesión se borran las dos tablas. | HU-LOC-01, 02 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-LOC-01 | Room 2.7.2 con KSP, compatible con Kotlin 2.2.10 y `minSdk` 24. El esquema se exporta a `app/schemas/` y se versiona. |
| RNF-LOC-02 | Privacidad: los DAO siempre filtran por el `uid` con sesión, aunque el borrado al cerrar sesión falle. |
| RNF-LOC-03 | Las lecturas y escrituras de Room nunca corren en el hilo principal. |
| RNF-LOC-04 | El APK release (R8 completo) abre y usa Room sin errores. |
| RNF-LOC-05 | Textos en `res/values/strings_search.xml`; accesibilidad como RNF-04 (los chips se anuncian como botones). |

## 6. Fuera de alcance

- Guardar en Room el inventario, las favoritas o las recetas propias: Firestore ya les da uso sin conexión.
- Sugerencias mientras se escribe (autocompletar).
- Quitar una sola búsqueda reciente (solo «Borrar» todas).
- Búsquedas recientes en Guardadas o en el editor de recetas propias.

## 7. Supuestos

- El texto de una búsqueda por voz es el que llena la barra después del dictado (CA-INV-07-03).
- «Ahorrados este mes» sigue calculándose con `GetMonthlySavingsUseCase`; solo cambia de dónde salen los registros.

## 8. Fidelidad con Figma

- **Frames de referencia:** Recetas (`557:3028`) y la búsqueda del Inventario (spec 006). Ninguno tiene búsquedas recientes.
- **Interacciones del prototipo:** no hay; los chips aparecen y se ocultan con las transiciones de la spec 007 cuando
  esté fusionada (hoy, sin animación).
- El historial en Room no cambia ninguna pantalla.

## 9. Desviaciones (aprobadas por Zhair el 2026-11-20)

| Desviación | Motivo |
|---|---|
| Bajo la barra de Recetas y del Inventario aparece «Recientes» con hasta 5 chips de texto y «Borrar», con el estilo de los filtros, solo con la barra enfocada y vacía. Los chips no llevan ícono. | El Figma no define búsquedas recientes, y no tiene un ícono de reloj ni de historial: no se usa uno de Material. |

## 10. Plan de entrega

Rama `feature/014-persistencia-local` desde `master`, un PR en draft, un commit por tarea (ver [`tasks.md`](./tasks.md)).

| Feature vertical | Contenido | Tareas |
|---|---|---|
| Base local | Dependencias de Room, base, entidades, DAO y su módulo | T002 |
| Historial sin conexión | Repositorio con caché y borrado al cerrar sesión | T003 |
| Búsquedas recientes | Dominio y datos; Descubrir; Inventario con voz | T004 a T006 |

Archivos compartidos que toca: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `core/di/CoreDataModule.kt` y
`feature/recipes/data/di/RecipesDataModule.kt`.
