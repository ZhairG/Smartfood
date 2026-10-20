# Spec 008: Perfil funcional

| Campo | Valor |
|---|---|
| Feature | `008-perfil` (Módulo 3) |
| Estado | Implementada (PR #119, fusionado el 2026-10-19) |
| Fecha | 2026-10-18 |
| Depende de | 001-autenticacion, 002-adaptabilidad-ui, 005-documentos-legales, 006-inventario |
| Afecta | Pestaña Perfil, perfil del usuario en Firestore, reglas de Firestore y Storage, tema de la app y aviso diario |
| Entrega | Viernes 23 de octubre de 2026 (o martes 20, si cambia el orden de los módulos) |

> Los códigos de esta spec llevan el prefijo `PER` (`HU-PER-01`, `CA-PER-01-01`, `RF-PER-01`, `RNF-PER-01`).

## 1. Contexto y motivación

En la sustentación de la app el equipo se comprometió a terminar el Perfil. Hoy la pestaña muestra el nombre, el correo,
las iniciales y la cantidad de productos del inventario, y solo funciona "Cerrar sesión" (spec 001, CA-05-03). Las
cuatro opciones ("Editar información personal", "Preferencias alimentarias", "Notificaciones" y "Cambiar contraseña")
no hacen nada, y las tarjetas "Recetas guardadas" y "Ahorrados este mes" no tienen datos.

Esta spec deja el Perfil funcionando como el prototipo de Figma, más una opción para cambiar el tema de la app
(desviación aprobada, §9).

## 2. Actores

- **Usuario con sesión:** quiere mantener sus datos, sus gustos y sus avisos al día.

## 3. Historias de usuario y criterios de aceptación

### HU-PER-01: Ver mi perfil
**Como** usuario **quiero** ver mis datos y mis números principales **para** saber cómo voy y llegar a cada ajuste.

| Código | Criterio |
|---|---|
| CA-PER-01-01 | **Dado** que abro la pestaña Perfil, **entonces** veo mi foto (o mis iniciales si no tengo foto), mi nombre completo y mi correo, como en el Figma `306:369`. |
| CA-PER-01-02 | **Dado** el Perfil, **entonces** las tarjetas muestran: "Recetas guardadas" = 0 hasta el módulo de recetas guardadas (#117), "Ahorrados este mes" = $0 hasta la spec 009, e "Ingredientes en inventario" = cantidad de productos de mi inventario (CA-INV-09-03). |
| CA-PER-01-03 | **Dado** el Perfil, **cuando** toco "Editar información personal", "Preferencias alimentarias", "Notificaciones", "Cambiar contraseña" o "Tema de la app", **entonces** se abre esa pantalla u hoja con la transición de empuje, y la barra inferior sigue visible con "Perfil" seleccionado. |
| CA-PER-01-04 | **Dado** una pantalla interna del Perfil, **cuando** toco la flecha o "atrás" del sistema, **entonces** vuelvo al Perfil. |

### HU-PER-02: Editar mi información personal
**Como** usuario **quiero** actualizar mi nombre, mi correo, mi teléfono, mi fecha de nacimiento y mi foto **para** que
mi cuenta tenga mis datos reales.

| Código | Criterio |
|---|---|
| CA-PER-02-01 | **Dado** "Editar información" (Figma `361:649`), **entonces** veo mi foto o iniciales con "Toca para cambiar tu foto" y los campos "Nombre completo", "Correo electrónico", "Teléfono" y "Fecha de nacimiento" con mis datos actuales. |
| CA-PER-02-02 | **Dado** que no cambié nada, **entonces** "Guardar" (arriba a la derecha) se ve deshabilitado; **cuando** cambio algún dato, se habilita. |
| CA-PER-02-03 | **Dado** que toco "Guardar", **entonces** aparece "¿Desea guardar los cambios?" con "Guardar" y "Cancelar" (Figma `369:1195`). "Cancelar" cierra el aviso sin guardar. |
| CA-PER-02-04 | **Dado** que confirmo, **cuando** los datos son válidos, **entonces** se ve el diálogo de carga y luego "Guardado · Perfil se actualizó" (Figma `369:905`), que se cierra solo a los 2 segundos; sigo en "Editar información" con los datos nuevos y el Perfil también los muestra. |
| CA-PER-02-05 | **Dado** el nombre, **entonces** es obligatorio, se guarda sin espacios al inicio ni al final y tiene de 2 a 50 caracteres; si no cumple, el campo se marca en rojo con su mensaje y no se guarda. |
| CA-PER-02-06 | **Dado** el teléfono, **entonces** es opcional; si lo escribo, debe ser un celular colombiano de 10 dígitos que empieza por 3 (se muestra como "+57 300 000 0000"); si no cumple, se marca en rojo y no se guarda. |
| CA-PER-02-07 | **Dado** la fecha de nacimiento, **entonces** es opcional, se elige con un calendario (DD/MM/AAAA) y no puede ser posterior a hoy. |
| CA-PER-02-08 | **Dado** que toco la foto, **entonces** se abre la hoja "Agregar foto" con "Galería" y "Cámara". "Cámara" abre la **cámara del sistema** del teléfono; "Galería" abre el selector de fotos. La foto elegida se ve en el círculo antes de guardar. |
| CA-PER-02-09 | **Dado** que guardé una foto nueva, **entonces** se sube reducida (máx. 1080 px, JPEG) y queda como mi foto en el Perfil. Si no hay conexión, aparece el error de red y la foto anterior se conserva. |
| CA-PER-02-10 | **Dado** que cambio el correo a uno distinto, **entonces** aparece debajo el campo "Contraseña actual"; **cuando** confirmo el guardado y la contraseña es correcta, **entonces** llega al correo nuevo un código de 4 dígitos (en español, desde el correo de SmartFood) y aparece el aviso "Revisa tu correo" para escribirlo, con "Reenviar código" a los 60 s. El correo de la cuenta no cambia hasta escribir el código correcto. |
| CA-PER-02-11 | **Dado** el cambio de correo, **cuando** la contraseña es incorrecta, el correo ya está en uso, el código es incorrecto o venció (10 min, 5 intentos) o no hay conexión, **entonces** se muestra el error correspondiente y el correo no cambia. |
| CA-PER-02-12 | **Dado** que escribí el código correcto, **entonces** "Guardado" dice "Tu correo ahora es <correo nuevo>", la cuenta y el perfil cambian al instante y el Perfil muestra el correo nuevo **sin cerrar sesión**; el próximo inicio de sesión es con el correo nuevo. |
| CA-PER-02-13 | **Dado** cambios sin guardar, **cuando** salgo con la flecha o "atrás", **entonces** se descartan sin preguntar, igual que el editor de productos (CA-INV-03-04). |

### HU-PER-03: Mis preferencias alimentarias
**Como** usuario **quiero** guardar mis restricciones, cocinas favoritas, nivel de picante y presupuesto por comida
**para** que la app me conozca.

| Código | Criterio |
|---|---|
| CA-PER-03-01 | **Dado** "Preferencias alimentarias" (Figma `376:1260`), **entonces** veo cuatro grupos de chips: "Restricciones alimentarias" (Vegetariano, Vegano, Sin gluten, Sin lácteos, Sin frutos secos), "Cocina Favorita" (Colombiana, Italiana, Asiatica, Mexicana, Mediterranea), "Nivel de Picante" (Suave, Medio, Picante) y "Presupuesto por comida" (Económico, Moderado, Sin restricción), con mis elecciones guardadas resaltadas en verde. |
| CA-PER-03-02 | **Dado** los dos primeros grupos, **entonces** puedo elegir varios chips o ninguno; **dado** los dos últimos, elijo como máximo uno, y tocar el elegido lo quita. |
| CA-PER-03-03 | **Dado** que toco "Guardar Preferencias", **entonces** aparece "¿Desea guardar los cambios?" (Figma `593:1976`) y, al confirmar, el diálogo de carga y "Guardado · Preferencias se actualizó" (Figma `593:1898`), que se cierra solo. "Guardar Preferencias" está deshabilitado si no cambié nada. |
| CA-PER-03-04 | **Dado** que guardé mis preferencias, **cuando** vuelvo a la pantalla (o en otro teléfono con la misma cuenta), **entonces** veo las mismas elecciones. |

### HU-PER-04: Mis notificaciones
**Como** usuario **quiero** elegir qué avisos recibo **para** que la app no me moleste de más.

| Código | Criterio |
|---|---|
| CA-PER-04-01 | **Dado** "Notificaciones" (Figma `593:2079`), **entonces** veo tres interruptores: "Productos por vencer", "Recetas sugeridas" e "Inventario vacío", con su descripción, todos encendidos la primera vez. |
| CA-PER-04-02 | **Dado** un interruptor, **cuando** lo toco, **entonces** cambia y queda guardado en este teléfono al instante (sin botón "Guardar"). |
| CA-PER-04-03 | **Dado** "Productos por vencer" apagado, **entonces** no recibo el aviso diario de vencimientos (HU-INV-08); **cuando** lo enciendo, se vuelve a programar. |
| CA-PER-04-04 | **Dado** "Inventario vacío" encendido, **cuando** el aviso diario se ejecuta y mi inventario no tiene productos, **entonces** recibo "Tu inventario está vacío · Registra tus alimentos para no perder de vista sus fechas"; al tocarlo se abre la app en el inventario. |
| CA-PER-04-05 | **Dado** "Recetas sugeridas", **entonces** su valor se guarda; su efecto lo define la spec 009 (Recetas + IA). |
| CA-PER-04-06 | **Dado** Android 13 o superior sin permiso de notificaciones, **cuando** enciendo un interruptor, **entonces** la app pide el permiso; si lo niego, el interruptor vuelve a apagado y se muestra "Activa las notificaciones en los ajustes del teléfono" con un botón para abrirlos. |

### HU-PER-05: Cambiar mi contraseña
**Como** usuario **quiero** cambiar mi contraseña sabiendo la actual **para** mantener segura mi cuenta.

| Código | Criterio |
|---|---|
| CA-PER-05-01 | **Dado** "Cambiar contraseña" (Figma `603:2358`), **entonces** veo el texto de ayuda y los campos "Contraseña actual", "Nueva Contraseña" y "Confirmar Contraseña", cada uno con el ojo para mostrarla, y el botón "Actualizar contraseña". |
| CA-PER-05-02 | **Dado** el campo "Nueva Contraseña", **cuando** tiene el foco, **entonces** se ve el mismo checklist de requisitos del registro (CA-01-03) encima del campo. |
| CA-PER-05-03 | **Dado** el botón, **entonces** solo se habilita cuando la actual no está vacía, la nueva cumple los requisitos y la confirmación es igual. |
| CA-PER-05-04 | **Dado** que toco "Actualizar contraseña" con datos válidos, **cuando** la contraseña actual es correcta, **entonces** se ve el diálogo de carga, luego "Guardado · Contraseña se actualizó" (Figma `603:2460`), y a los 2 segundos vuelvo al Perfil. La próxima vez inicio sesión con la nueva. |
| CA-PER-05-05 | **Dado** que la contraseña actual es incorrecta, **entonces** el campo se marca en rojo con "La contraseña actual no es correcta" y la contraseña no cambia. |
| CA-PER-05-06 | **Dado** que no hay conexión o hubo demasiados intentos, **entonces** se muestra el error correspondiente y puedo reintentar. |

### HU-PER-06: Tema de la app *(desviación aprobada, §9)*
**Como** usuario **quiero** elegir si la app se ve clara u oscura **para** usarla cómodo de día y de noche.

| Código | Criterio |
|---|---|
| CA-PER-06-01 | **Dado** el Perfil, **entonces** veo una fila más, "Tema de la app", debajo de "Cambiar contraseña", con el tema actual a la derecha ("Claro", "Oscuro" o "Según el teléfono"). |
| CA-PER-06-02 | **Dado** que toco "Tema de la app", **entonces** se abre una hoja con las tres opciones y "Guardar"/"Cancelar", con el mismo estilo de la hoja de categoría del inventario. |
| CA-PER-06-03 | **Dado** que elijo una opción y guardo, **entonces** toda la app cambia de inmediato, sin reiniciarse, y la elección se conserva al cerrar y abrir la app en este teléfono. |
| CA-PER-06-04 | **Dado** la primera vez, **entonces** el tema es "Claro" (CA-UI-05-01 sigue vigente como valor por defecto). |
| CA-PER-06-05 | **Dado** el tema oscuro, **entonces** todas las pantallas se leen bien: textos, íconos y campos con el contraste del esquema oscuro existente (spec 002, CA-UI-05-04). |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-PER-01 | Perfil del usuario con nombre, correo, teléfono, fecha de nacimiento y foto, guardado en `users/{uid}`. | HU-PER-01, HU-PER-02 |
| RF-PER-02 | Foto de perfil en Storage (`users/{uid}/profile/`), tomada con la cámara del sistema o de la galería. | HU-PER-02 |
| RF-PER-03 | Cambio de correo con un código enviado por el SMTP de SmartFood al correo nuevo; las Cloud Functions `requestEmailChangeCode` y `confirmEmailChange` cambian la cuenta y el perfil (mismo patrón y límites que la spec 003). | HU-PER-02 |
| RF-PER-04 | Preferencias alimentarias en `users/{uid}`. | HU-PER-03 |
| RF-PER-05 | Preferencias de notificaciones en el teléfono, que encienden o apagan el aviso diario y el aviso de inventario vacío. | HU-PER-04 |
| RF-PER-06 | Cambio de contraseña confirmando la actual. | HU-PER-05 |
| RF-PER-07 | Tema de la app guardado en el teléfono y aplicado a toda la app. | HU-PER-06 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-PER-01 | Seguridad: las reglas de Firestore solo dejan al dueño actualizar campos conocidos y válidos de su perfil, y nunca `termsVersion`, `termsAcceptedAt` ni `createdAt` (RNF-02). La foto solo la sube el dueño, como JPEG de menos de 2 MB. |
| RNF-PER-02 | Los textos van en `res/values/strings_profile.xml` (RNF-03). |
| RNF-PER-03 | Accesibilidad: los interruptores, chips y la foto se anuncian con su nombre y estado; los avisos de resultado usan `liveRegion` (RNF-04). |
| RNF-PER-04 | Fidelidad con Figma (constitución §1): medidas, colores, tipografía e íconos de los frames de §8; las transiciones siguen la spec 007. |
| RNF-PER-05 | Funciona desde Android 7.0 (API 24); el permiso de notificaciones solo se pide en Android 13+. |

## 6. Fuera de alcance

- Calcular "Recetas guardadas" (próximo módulo, #117) y "Ahorrados este mes" (spec 009).
- Que la IA use las restricciones alimentarias (decisión del 18/10/2026; queda como trabajo futuro).
- Eliminar la cuenta desde la app.
- Sincronizar las notificaciones y el tema entre teléfonos (son de cada teléfono).
- Mejorar la cámara propia de la app (se usa la del sistema en el Perfil; la del inventario no cambia).

## 7. Supuestos

- El cambio de correo usa los secretos `SMTP_USER` y `SMTP_PASS` de la spec 003. Si Firebase invalida la sesión al
  cambiar el correo, la app vuelve a iniciarla sola con el correo nuevo y la contraseña recién confirmada
  (CA-PER-02-12).
- El esquema oscuro de `Theme.kt` se usa tal cual; si una pantalla no se lee bien en oscuro, se corrige el rol de
  color, no el token (spec 002).

## 8. Fidelidad con Figma

Capturas en [`disenos-figma/Modulo 3 - Perfil`](../../disenos-figma/README.md).

| Pantalla | Frame | Criterios |
|---|---|---|
| Perfil | `306:369` | CA-PER-01-01 a 01-03 |
| Editar información | `361:649`, `369:1195` (pregunta), `369:905` (guardado) | HU-PER-02 |
| Preferencias alimentarias | `376:1260`, `593:1976` (pregunta), `593:1898` (guardado) | HU-PER-03 |
| Notificaciones | `593:2079` | HU-PER-04 |
| Cambiar contraseña | `603:2358`, `603:2460` (guardado) | HU-PER-05 |
| Cerrar sesión | `604:2668` | spec 001 CA-05-03 (ya implementado) |

**Interacciones del prototipo:** todas las navegaciones de estos frames son instantáneas; las transiciones de la app
siguen la spec 007. Los avisos "Guardado" vuelven solos (`AFTER_TIMEOUT`): en Editar información y Preferencias a la
misma pantalla, y en Cambiar contraseña al Perfil.

## 9. Desviaciones (aprobadas por Zhair el 2026-10-18)

| Desviación | Motivo |
|---|---|
| Fila "Tema de la app" y su hoja (HU-PER-06), que no están en el Figma. | Pedido de Zhair; reutiliza el esquema oscuro de la spec 002. |
| La foto de perfil se toma con la **cámara del sistema** y no con un visor propio. | Pedido de Zhair: la cámara del Figma se mejorará más adelante. |
| El correo es editable **con un código** y pide la contraseña actual en un **campo** "Contraseña actual" que aparece debajo del correo solo si cambió; el código se escribe en el aviso "Revisa tu correo" (casillas de `OtpCodeInput` de la recuperación). El Figma no tiene ninguno de los dos. | Firebase exige confirmar la identidad y el correo nuevo. Un campo es más claro que un segundo aviso (decisión de Zhair en el #109). |
| Mientras se guarda se ve el diálogo de carga "Guardando cambios…", y los errores se muestran en rojo bajo los campos. | El Figma no define los estados de carga ni de error (como el inventario, CA-INV-02-12). |
| "Recetas guardadas" y "Ahorrados este mes" muestran 0 y $0. | "Recetas guardadas" depende del módulo de recetas guardadas (#117) y "Ahorrados este mes", de la spec 009. |
| Notificaciones guarda al tocar cada interruptor; el Figma no muestra botón "Guardar". | El frame no tiene botón visible. |

## 10. Plan de entrega

Un PR para todo el módulo (`feature/008-perfil` → `master`), con **4 Issues verticales**, con un **commit por tarea** (`Tnnn`) y cada Issue con su
domain, data, estructura, UI y pruebas. Los Issues se trabajan **en orden** sobre la misma rama (cada persona hace
`git pull` antes de empezar), porque comparten `Routes.kt`, `MainNavGraph.kt` y las reglas.

| Issue | Contenido | Tareas | Dificultad |
|---|---|---|---|
| 1. Editar información | Modelo de perfil ampliado, reglas de Firestore y Storage, foto con cámara del sistema y galería, cambio de correo, pantalla y Perfil con foto | T002-T009 | avanzada |
| 2. Preferencias alimentarias | Modelo, repositorio, reglas (campo `preferences`), pantalla de chips | T010-T013 | intermedia |
| 3. Notificaciones y tema | DataStore, aviso diario y de inventario vacío, permiso, fila y hoja de tema, `MainActivity` | T014-T019 | intermedia |
| 4. Cambiar contraseña | Reautenticación y cambio en `AuthRepository`, pantalla con checklist | T020-T022 | intermedia |

Cierre (T023-T026) en el mismo PR: suite instrumentada, reglas, checklist manual y matriz.
