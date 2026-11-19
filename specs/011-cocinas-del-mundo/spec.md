# Spec 011: Cocinas del mundo

| Campo | Valor |
|---|---|
| Feature | `011-cocinas-del-mundo` |
| Estado | Implementada |
| Fecha | 2026-11-06 |
| Depende de | 008-perfil (Cocina Favorita), 009-recetas-ia (catálogo y agente) |
| Afecta | `assets/recetas.json`, `assets/ingredientes.json`, fotos de recetas, `MealPlanner`, flujo de la IA |
| Entrega | Un PR desde `feature/011-cocinas-del-mundo` |

> Los códigos de esta spec llevan el prefijo `COC` (`HU-COC-01`, `CA-COC-01-01`, `RF-COC-01`, `RNF-COC-01`).

## 1. Contexto y motivación

El catálogo tiene 50 recetas, casi todas colombianas. Preferencias alimentarias guarda la "Cocina Favorita" (Colombiana,
Italiana, Asiática, Mexicana y Mediterránea), pero la spec 009 §6 dejó claro que el agente no la usa.

Zhair recuerda que el profesor pidió ampliar el catálogo y decidió hacerlo con recetas de otras nacionalidades, para que
el agente también trabaje con esa restricción (2026-11-06). La transcripción de Word de la clase solo cita más
ingredientes y más usuarios, así que el pedido queda registrado como decisión de Zhair.

## 2. Actores

- **Usuario con sesión:** quiere que el plan de 3 días respete las cocinas que le gustan.
- **Agente de recetas:** arma el plan con las recetas que cumplen todas las restricciones.

## 3. Historias de usuario y criterios de aceptación

### HU-COC-01: Catálogo con varias cocinas
**Como** usuario **quiero** recetas de varias cocinas **para** tener un plan variado.

| Código | Criterio |
|---|---|
| CA-COC-01-01 | **Dado** el catálogo, **entonces** tiene 70 recetas: las 50 anteriores y 5 nuevas de cada cocina (italiana, asiática, mexicana y mediterránea). |
| CA-COC-01-02 | **Dado** una receta del catálogo, **entonces** declara una sola cocina válida en `cocina`. Si falta o es desconocida, el catálogo no carga y la prueba de contenido falla. |
| CA-COC-01-03 | **Dado** una receta nueva, **entonces** tiene foto de licencia libre con autor, licencia y fuente en `docs/licenses/fotos-recetas.md`. |
| CA-COC-01-04 | **Dado** un ingrediente nuevo, **entonces** tiene empaque, etiquetas de dieta y precio de al menos una tienda en `docs/precios/precios.json`, con el producto consultado. |

### HU-COC-02: El agente respeta la Cocina Favorita
**Como** usuario **quiero** que el plan use solo mis cocinas favoritas **para** recibir recetas que quiero comer.

| Código | Criterio |
|---|---|
| CA-COC-02-01 | **Dado** que no elegí ninguna cocina, **cuando** genero el plan, **entonces** el agente considera todo el catálogo. |
| CA-COC-02-02 | **Dado** que elegí una cocina, **cuando** hay plan con todas las recetas de esa cocina, **entonces** todas sus recetas son de esa cocina (enmienda del 2026-11-19: si no lo hay, aplica CA-COC-02-06). |
| CA-COC-02-03 | **Dado** que elegí varias cocinas, **cuando** hay plan con todas las recetas de ellas, **entonces** cada receta es de cualquiera de ellas (enmienda del 2026-11-19: si no lo hay, aplica CA-COC-02-06). |
| CA-COC-02-04 | **Dado** cocinas y restricciones alimentarias, **cuando** hay plan, **entonces** sus recetas cumplen las dos. |
| CA-COC-02-05 | **Dado** una cocina elegida, **cuando** hay plan, **entonces** es el de mayor utilidad entre los válidos con esa cocina, comprobado contra fuerza bruta. |
| CA-COC-02-06 | **Dado** cocinas elegidas, **cuando** no hay plan con las 3 recetas de esas cocinas, **entonces** el agente busca con 2 y luego con 1, y entrega el plan del primer nivel que lo tenga: el de mayor utilidad con ese mínimo de recetas de mis cocinas, comprobado contra fuerza bruta (2026-11-19). |
| CA-COC-02-07 | **Dado** cocinas y restricciones alimentarias, **cuando** el plan se completa con otras cocinas, **entonces** todas sus recetas siguen cumpliendo la dieta: la dieta nunca se relaja (2026-11-19). |

### HU-COC-03: Ver y ajustar la Cocina Favorita en el flujo de la IA
**Como** usuario **quiero** ver qué cocinas se aplican y qué hacer si no hay plan **para** entender el resultado.

| Código | Criterio |
|---|---|
| CA-COC-03-01 | **Dado** cocinas elegidas en el Perfil, **cuando** abro "Sugerencia con IA", **entonces** veo la línea "Cocina favorita: …" con sus nombres. Sin cocinas, la línea no aparece. |
| CA-COC-03-02 | **Dado** que no hay plan ni con una receta de mis cocinas y sin ellas sí lo habría, **entonces** "Prueba una de estas opciones" incluye "Quita tu cocina favorita en el Perfil". |
| CA-COC-03-03 | **Dado** que no hay plan y quitar las cocinas no lo arregla, o que no elegí cocinas, **entonces** esa sugerencia no aparece. |
| CA-COC-03-04 | **Dado** que las preferencias no se pueden leer, **entonces** el flujo sigue sin filtrar por cocina, como con las restricciones. |
| CA-COC-03-05 | **Dado** que el plan se completó con otras cocinas (CA-COC-02-06), **entonces** bajo "Tu plan de 3 días" veo "No alcanzó para 3 recetas de cocina <cocinas>: completamos tu plan con otras cocinas." (2026-11-19). |

## 4. Requisitos funcionales

| Código | Requisito |
|---|---|
| RF-COC-01 | `recetas.json` lleva `cocina` en cada receta: `colombiana`, `italiana`, `asiatica`, `mexicana` o `mediterranea`. |
| RF-COC-02 | La cocina es una restricción dura de una sola receta: se aplica en la consistencia de nodo, junto a la dieta. |
| RF-COC-03 | La Cocina Favorita solo afecta al plan del agente. Recetas, Descubrir y Guardadas muestran todo el catálogo. |
| RF-COC-04 | Las dietas siguen siendo intocables: nunca se sugiere quitarlas. |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-COC-01 | Sin cocinas elegidas, el agente da el mismo plan que daría sin este filtro. |
| RNF-COC-02 | Rendimiento: se repite `MealPlannerPerformanceTest` con 70 recetas contra la meta de 2 s, y el resultado va en `docs/ia/evaluacion-agente.md`. Con inventarios normales cumple; el inventario enorme es una limitación aceptada (§9). |
| RNF-COC-03 | Las fotos siguen en `drawable-nodpi` como webp de 800 px de ancho; `res/raw/keep.xml` las conserva en release. |
| RNF-COC-04 | Funciona desde Android 7.0 (API 24). R8 no cambia: `cocina` va en el DTO `@Serializable` existente. |

## 6. Fuera de alcance

- Filtrar por cocina la pestaña Recetas, Descubrir o Guardadas.
- Una preferencia blanda (dar más puntos a una cocina sin excluir las demás).
- Usar el nivel de picante o el presupuesto de Preferencias en el agente.
- Elegir la cocina al crear una receta propia.
- Botón que quite la cocina desde la sugerencia: las sugerencias del agente son texto (CA-RIA-02-07).

## 7. Supuestos

- Cada receta pertenece a una sola cocina. Si un plato existe en varias, manda su origen más reconocido.
- Con varias cocinas elegidas, una receta entra si es de cualquiera.
- Los platos se adaptan a ingredientes que se consiguen en Colombia (por ejemplo, almendras en el pesto y hummus sin
  tahini). Su nombre se mantiene porque el plato sigue siendo reconocible.
- Las recetas nuevas son sencillas a propósito. El agente admite máximo 3 ingredientes por comprar al día, así que con
  una despensa típica (arroz, papa, cebolla, tomate, ajo y zanahoria) cada cocina debe tener al menos 3 recetas posibles.

## 8. Fidelidad con Figma

El Figma no define nada de esta spec: la línea de cocinas reutiliza el estilo de "Según tus preferencias" y la sugerencia
usa la lista existente de "Prueba una de estas opciones" (`recipes_ai_*`).

## 9. Desviaciones (aprobadas por Zhair el 2026-11-06)

| Desviación | Motivo |
|---|---|
| La píldora de precios del detalle de receta dice "D1 · precios de octubre y noviembre 2026" y no "octubre 2026" (aprobada el 2026-11-07). | Los 50 ingredientes originales son del 20 de octubre y los 7 de esta spec, del 6 de noviembre; con "octubre" el texto sería inexacto. |
| Línea "Cocina favorita: …" en el contexto de la IA, que no está en el Figma. | La persona debe saber por qué el plan solo trae esas cocinas. |
| Sugerencia "Quita tu cocina favorita en el Perfil", que no está en el Figma. | Cada cocina tiene pocas recetas y sin esta salida la persona quedaría sin plan. |
| Aviso "No alcanzó para 3 recetas de cocina …: completamos tu plan con otras cocinas." bajo "Tu plan de 3 días", que no está en el Figma (aprobada por Zhair el 2026-11-19, CA-COC-03-05). | La persona debe saber por qué el plan trae recetas de otras cocinas. |

La restricción dura deja a menudo sin plan y el inventario enorme supera los 2 s en el portátil. Zhair aceptó las dos
limitaciones el 2026-11-06; los datos están en `docs/ia/evaluacion-agente.md` §7. La primera se corrigió el 2026-11-19
con la relajación escalonada (CA-COC-02-06, `docs/ia/evaluacion-agente.md` §8).

## 10. Plan de entrega

Un PR (`feature/011-cocinas-del-mundo` → `master`) con un **commit por tarea**. Las tareas T002 a T005 son el catálogo, T006
y T007 la restricción, y T008 la evaluación. El detalle está en [`tasks.md`](./tasks.md).
