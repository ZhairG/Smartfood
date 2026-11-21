# Trabajo pendiente después de la entrega

Lista ordenada del trabajo que sigue a la entrega `v1.0-entrega` (commit `9e68d67`). Sale de la revisión de los
informes de evaluación (IA y Aplicaciones Móviles), de una revisión del modo oscuro y de dos trabajos que quedaron
abiertos. Cada punto pasa por el modo plan de [`AGENTS.md`](../AGENTS.md) (preguntas, plan y aprobación) antes de
escribir código. Al cerrarse un punto se actualizan su estado y su PR en la tabla.

> **Informes de evaluación:** los dos informes completos están en `retroalimentacion/` (`informe-ia.pdf` e
> `informe-moviles.pdf`). Esa carpeta es local y está en `.gitignore`: solo existe en el computador de Zhair, porque
> los informes incluyen notas y datos de los integrantes. Al empezar un punto marcado con «Origen: informe…», el
> agente vuelve a leer el informe correspondiente.

## Estado

| # | Punto | Spec | Estado | PR |
|---|---|---|---|---|
| 1 | Modo sin facturación (sin plan Blaze) | [012](../specs/012-modo-sin-facturacion/spec.md) | Implementado, pruebas en la ronda final | — |
| 2 | La dieta se pierde si falla la lectura del perfil | 009 (CA-RIA-02-12) | Hecho | [#140](https://github.com/ZhairG/Smartfood/pull/140) |
| 3 | Cierre de cocina: precio y reintentos | 009 (CA-RIA-04-08, 04-09) | Hecho | [#141](https://github.com/ZhairG/Smartfood/pull/141) |
| 4 | Guardado pendiente o confirmado en el Perfil | 008 (CA-PER-02-14, 03-06) | Hecho | [#142](https://github.com/ZhairG/Smartfood/pull/142) |
| 5 | Cocina Favorita sin plan posible | 011 (CA-COC-02-06, 02-07, 03-05) | Hecho | [#143](https://github.com/ZhairG/Smartfood/pull/143) |
| 6 | Revisión del modo oscuro | 013 (enmienda CA-UI-05-04) | Hecho | [#144](https://github.com/ZhairG/Smartfood/pull/144) |
| 7 | Animaciones y transiciones | 007 | Pendiente | — |
| 8 | Correcciones de documentación y medición del agente | 009 | Pendiente | — |
| 9 | Pendientes menores | 009, 010 | Pendiente | — |
| 10 | Persistencia local con Room (opcional) | por definir | Pendiente | — |

## 1. Modo sin facturación (urgente)

El proyecto de Firebase dejará el plan Blaze. En el plan gratuito (Spark) dejan de funcionar las Cloud Functions y
Cloud Storage: la recuperación de contraseña con código (spec 003), el correo de bienvenida (spec 004), el cambio de
correo del Perfil y las fotos del inventario y del perfil.

- **Antes de quitar la facturación:** copiar a Firestore las fotos que ya están en Storage. Sin Blaze, Storage no
  responde y esas fotos dejan de verse.
- **Interruptor:** parámetro `blaze_activo` de Remote Config, por defecto `false`. El dominio expone un
  `BackendModeProvider`; solo `core/data` conoce Remote Config. Volver a Blaze no exige publicar otra versión.
- **Implementación por modo:** cada repositorio afectado conserva su versión actual y suma una gratuita. Una tercera
  delega según el modo:
  - recuperación de contraseña con el enlace de Firebase (`sendPasswordResetEmail`) en lugar del código;
  - cambio de correo con `verifyBeforeUpdateEmail`;
  - fotos guardadas como bytes en Firestore (`users/{uid}/media`, en partes de menos de 1 MiB), leídas con un
    `Fetcher` de Coil;
  - correo de bienvenida: `sendEmailVerification` o sin correo (se decide en su plan).
- Reglas nuevas en `firestore.rules` con sus pruebas en `rules-test/`. Las funciones no se borran: quedan en
  `functions/` para volver a Blaze.
- **Referencia:** spec 008 «Modo sin facturación» de
  [Huellitas_YA](https://github.com/ErickEsquivel/Huellitas_YA) (PR #23 a #25), que resolvió el mismo problema.

## 2. La dieta se pierde si falla la lectura del perfil

*Origen: informes de IA y de Aplicaciones Móviles (prioridad 1 en ambos).*

`GetAiContextUseCase` convierte una lectura fallida de las preferencias en «sin restricciones» (`getOrNull()` y
`orEmpty()`), y con eso también se pierden las cocinas favoritas. Con un perfil vegano y una lectura fallida, el plan
incluyó una receta con pollo.

- Distinguir «sin restricciones» de «perfil desconocido». En el segundo caso no se genera el plan y se muestra un
  error con opción de reintentar.
- Prueba que reproduce el caso: perfil `VEGAN` con la lectura fallida.

## 3. Cierre de cocina: precio y reintentos

*Origen: informe de Aplicaciones Móviles.*

- `FinishCookingUseCase` arma el `ItemDraft` sin `priceText`, así que al descontar una cantidad el producto pierde su
  precio.
- Primero descuenta los ingredientes y al final guarda el historial con un identificador nuevo. Si algo falla y el
  usuario reintenta, los descuentos se repiten. Se necesita un identificador de operación estable y escrituras
  agrupadas en un lote (`WriteBatch`), sin transacciones, para conservar el uso sin conexión.

## 4. Guardado pendiente o confirmado en el Perfil

*Origen: informe de Aplicaciones Móviles.*

El Perfil devuelve el mismo éxito si el servidor confirmó la escritura o si venció la espera de 3 s. Debe devolver
`WriteOutcome` (`SYNCED` o `PENDING`) y mostrar «Guardado en tu teléfono», como el inventario.

## 5. Cocina Favorita sin plan posible

*Origen: pruebas en el teléfono y el informe de IA.*

Con un inventario pequeño, una Cocina Favorita distinta de la colombiana deja 0 o 1 recetas posibles, y no sale plan
(asiática con vegetariano no produce ninguno).

- Si no hay plan con las 3 recetas de la cocina favorita, relajar a «al menos una» y avisar al usuario que el plan se
  completó con otras cocinas.
- La dieta nunca se relaja.
- Revisar `MealPlannerTest` contra fuerza bruta y las cifras de [`ia/evaluacion-agente.md`](ia/evaluacion-agente.md).

## 6. Revisión del modo oscuro

El modo oscuro tiene poco contraste en el encabezado, la barra de navegación, algunas listas y los íconos con fondo.

- **Referencia:** el Figma solo tiene frames oscuros del módulo de autenticación (Login, Registro, recuperación,
  checklist de contraseña, «Iniciando», éxito y «Sin conexión»). El resto de los módulos va como **Desviación**
  aprobada.
- **Causas encontradas:** `DarkColorScheme` usa `primaryContainer = GreenLight` (un contenedor claro sobre fondo
  oscuro), y unos 25 archivos de la UI usan tokens claros fijos (`White`, `GreenLight`, `GreenSoft`, `YellowSoft`…)
  en lugar de roles del esquema.
- Requiere enmendar la regla de `AGENTS.md` que dice que los esquemas claro y oscuro no se tocan.

## 7. Animaciones y transiciones (spec 007)

El borrador de la spec 007 (`spec.md`, `plan.md` y `tasks.md`, prefijo `ANI`) se escribió el 2026-10-18 y nunca se
fusionó. Hoy está en el *stash* local «On docs/007-animaciones: borrador spec 007» (commit `11d20e9`). Las specs 008 a
010 ya lo citan.

- Recuperarlo y extenderlo a Perfil, Recetas y Guardadas, que llegaron después.
- Contenido principal: empuje de 300 ms ease-in-out (hoy 600 ms), disolvencia de 100 ms entre pestañas, foco de los
  campos de 150 ms, diálogos y hojas de 300 ms y listas animadas. La fluidez se mide en el build release.
- Va después del modo oscuro porque los dos tocan el design system.

## 8. Correcciones de documentación y medición del agente

*Origen: informe de IA.*

- **Memoria de A\*:** «hasta 6.972 estados» es la media de los máximos de frontera (6.971,5). El máximo individual es
  15.172. Ninguno de los dos mide bytes.
- **Aprovechamiento:** el 88,9 % es la proporción de productos elegibles próximos a vencer que se usan al menos en
  parte. Hay que mostrar siempre su denominador y separarlo de la cantidad consumida.
- Análisis de sensibilidad de los pesos de la utilidad (1,0 · 0,3 · 0,5) y del recocido simulado con varias semillas
  y el mismo presupuesto de cómputo.
- Medir en un teléfono con el catálogo de 70 recetas, incluido el caso de estrés (8,3 s en un portátil, frente a la
  meta de 2 s). Definir un tiempo límite y una respuesta clara si la búsqueda no termina.
- Corregir los objetivos específicos del póster.

## 9. Pendientes menores

- T024 de la spec 010.
- Prueba intermitente `AiFlowScreenTest.caRia0207`.

## 10. Persistencia local con Room (opcional)

*Origen: informe de Aplicaciones Móviles (unidad 6 del curso).*

La app usa DataStore, archivos internos para las fotos pendientes y la caché de Firestore, pero no SQL ni Room. Un
caso pertinente sería una caché local del historial de cocina o de las recetas guardadas, con entidad, DAO y pruebas.

## Reglas para todos los puntos

- Modo plan con preguntas antes de cualquier cambio, y aprobación de Zhair.
- Zhair implementa los 10 puntos solo. No se crean Issues por punto.
- Una rama por punto, desde `master` actualizado, con un commit por tarea (`Tnnn`) y su PR abierto como *draft*.
- **Las pruebas van al final.** Unitarias, compilación, lint, instrumentadas, `rules-test`, `functions` y prueba en el
  teléfono se corren una sola vez, cuando todos los puntos estén implementados. Después, los PR se marcan como listos
  y se fusionan en orden. Es una excepción al paso 3 de [`AGENTS.md`](../AGENTS.md) que vale solo para esta lista.
  Las pruebas nuevas de cada punto sí se escriben con el código y citan su CA.
- Nunca incluir `app/release/*` en los commits.
- Antes de cada `git push`, confirmar con `gh auth status` que la cuenta activa sea la del dueño del repositorio.
