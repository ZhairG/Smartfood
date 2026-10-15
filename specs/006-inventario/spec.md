# Spec 006: Inventario de alimentos

| Campo | Valor |
|---|---|
| Feature | `006-inventario` |
| Estado | Especificada (pendiente de implementación) |
| Fecha | 2026-10-12 (cantidad y unidad: 2026-10-14) |
| Depende de | 001-autenticacion (sesión y perfil), 002-adaptabilidad-ui, 005-documentos-legales |
| Afecta | Navegación principal (Home pasa a ser la pestaña Perfil, HU-05) y Política de privacidad |
| Diseño | Figma `z1AoQvluQcmx98gjP4vHF6`, frames principales de inventario y búsqueda (ver §8) |

> Los códigos de esta spec llevan el prefijo `INV` (`HU-INV-01`, `CA-INV-01-01`) para no chocar con los de otras specs.

## 1. Contexto y motivación

SmartFood une tres variables para reducir el desperdicio de comida: **inventario**, **vencimiento** y **presupuesto**
(constitución §1). Esta spec cubre las dos primeras. La persona registra lo que tiene en casa con su fecha de
vencimiento y la app le muestra qué se daña primero y le avisa antes de que pase.

Para que registrar y consultar sea rápido:
- cada producto puede llevar una **foto**, tomada desde la app y guardada en la nube con la cuenta;
- la búsqueda acepta **varios ingredientes dictados por voz** a la vez ("carne, pollo y leche"), para responder
  "¿qué tengo de esto?" mientras se cocina o se hace la lista de compras;
- los **filtros por categoría** acotan la lista.

El inventario es también la entrada del futuro agente de recomendación de recetas.

## 2. Actores

- **Usuario**: persona con sesión iniciada que administra el inventario de su hogar.
- **Sistema de avisos**: recuerda al usuario los productos que están por vencer, aunque no tenga la app abierta.

## 3. Definiciones

| Término | Significado |
|---|---|
| Producto | Un alimento del inventario: nombre, fecha de vencimiento, categoría y, opcionalmente, una cantidad con su unidad y una foto. |
| Categoría | Una de: Frutas, Lácteos, Proteínas, Verduras, Cereales, Otro. |
| Cantidad | Número opcional mayor que 0 y hasta 9999, con 2 decimales como máximo (por ejemplo, 1,5). Se escribe con coma o punto decimal. |
| Unidad | Una de: unidades, g, kg, lb, ml, L. Va siempre con una cantidad: las dos están o ninguna. |
| Días restantes | Días entre hoy y la fecha de vencimiento, según el calendario del teléfono. |
| Vencido | Días restantes < 0 (la fecha ya pasó). |
| Próximo a vencer | Días restantes entre 0 y 3, ambos incluidos (vence hoy o en los próximos 3 días). |
| Fresco | Días restantes > 3. |
| Término de búsqueda | Cada parte del texto de búsqueda separada por comas. |

## 4. Historias de usuario y criterios de aceptación

### HU-INV-01: Ver mi inventario
**Como** usuario **quiero** ver mis productos agrupados según su vencimiento **para** saber qué debo consumir primero.

| Código | Criterio |
|---|---|
| CA-INV-01-01 | **Dado** que tengo productos, **cuando** abro "Mi Inventario", **entonces** los veo en tres secciones en este orden: "Vencido" (rojo), "Próximo a vencer" (amarillo) y "Fresco" (verde). Las secciones vacías no se muestran. |
| CA-INV-01-02 | **Dado** un producto, **entonces** su fila muestra la fecha de vencimiento (dd/mm/aaaa), el nombre, la etiqueta de su categoría y un botón de editar (lápiz). |
| CA-INV-01-03 | **Dado** un producto con foto, **entonces** la miniatura es la foto; **dado** uno sin foto, **entonces** la miniatura es un bloque del color de su estado. |
| CA-INV-01-04 | **Dado** una sección, **entonces** sus productos están ordenados por fecha de vencimiento, del más próximo al más lejano; a igual fecha, por nombre. |
| CA-INV-01-05 | **Dado** que no tengo productos, **entonces** veo un mensaje que me invita a añadir el primero con el botón "+". |
| CA-INV-01-06 | **Dado** que hoy es 10/09/2026, **entonces** un producto que vence el 09/09/2026 está en "Vencido", uno que vence el 10/09/2026 o el 13/09/2026 está en "Próximo a vencer" y uno que vence el 14/09/2026 está en "Fresco". |
| CA-INV-01-07 | **Dado** que añadí, edité o eliminé un producto, **cuando** vuelvo a la lista, **entonces** veo el cambio sin recargar manualmente. |
| CA-INV-01-08 | **Dado** un producto con cantidad, **entonces** su fila muestra la cantidad y la unidad junto a la etiqueta de la categoría (por ejemplo, "1,5 kg"); **dado** uno sin cantidad, **entonces** no se muestra nada en su lugar. |

### HU-INV-02: Añadir un producto
**Como** usuario **quiero** registrar un producto con su vencimiento y categoría **para** llevar el control de lo que
tengo.

| Código | Criterio |
|---|---|
| CA-INV-02-01 | **Dado** que estoy en "Mi Inventario", **cuando** toco "+", **entonces** se abre "Añadir Producto" con la zona de foto vacía y los campos nombre (ejemplo "Ej. Carne"), "Fecha de vencimiento" y "Seleccionar categoría". |
| CA-INV-02-02 | **Dado** el formulario, **mientras** falte el nombre, la fecha o la categoría, **entonces** el botón "Añadir al Inventario" se ve gris y no hace nada; **cuando** están los tres, **entonces** se habilita en naranja. |
| CA-INV-02-03 | **Dado** el nombre, **cuando** tiene solo espacios o más de 50 caracteres, **entonces** el campo se marca en rojo con su mensaje y no se guarda. Los espacios al inicio y al final se ignoran. |
| CA-INV-02-04 | **Dado** "Fecha de vencimiento", **cuando** la toco, **entonces** elijo la fecha en un calendario que no permite días anteriores a hoy. |
| CA-INV-02-05 | **Dado** "Seleccionar categoría", **cuando** la toco, **entonces** aparece la hoja "Seleccione una categoría" con las seis categorías; "Guardar" aplica la elegida y "Cancelar" deja la anterior. |
| CA-INV-02-06 | **Dado** un formulario válido, **cuando** toco "Añadir al Inventario", **entonces** veo "Producto Guardado: Te avisaremos antes de que caduque para que no se desperdicie." y regreso a "Mi Inventario" con el producto en su sección. |
| CA-INV-02-07 | **Dado** que el guardado falla (por ejemplo, sin conexión), **entonces** veo un mensaje de error, el formulario conserva lo que escribí y puedo reintentar. |
| CA-INV-02-08 | **Dado** el formulario, **entonces** el campo "Cantidad" es opcional y no cambia el estado del botón "Añadir al Inventario" (CA-INV-02-02). **Cuando** escribo una cantidad, **entonces** la unidad empieza en "unidades" y puedo cambiarla por g, kg, lb, ml o L; si dejo la cantidad vacía, el producto se guarda sin cantidad ni unidad. |
| CA-INV-02-09 | **Dado** el campo "Cantidad", **cuando** escribo un valor que no es un número, que es 0 o menor, mayor que 9999 o con más de 2 decimales, **entonces** el campo se marca en rojo con su mensaje y no se guarda. |

### HU-INV-03: Editar un producto
**Como** usuario **quiero** corregir los datos de un producto **para** que el inventario refleje lo que realmente tengo.

| Código | Criterio |
|---|---|
| CA-INV-03-01 | **Dado** un producto de la lista, **cuando** toco su lápiz, **entonces** se abre "Editar Producto" con su foto, nombre, fecha, categoría y cantidad (si tiene) cargados. |
| CA-INV-03-02 | **Dado** "Editar Producto", **cuando** toco "Guardar" en la barra superior con datos válidos, **entonces** veo "Guardado: «Carne» se actualizó" y regreso a la lista con el cambio. |
| CA-INV-03-03 | **Dado** "Editar Producto", **entonces** se aplican las mismas validaciones del alta (CA-INV-02-02, 02-03 y 02-09), salvo que se puede conservar una fecha ya vencida; solo una fecha **nueva** no puede ser anterior a hoy. |
| CA-INV-03-04 | **Dado** que cambié datos, **cuando** vuelvo atrás sin tocar "Guardar", **entonces** el producto queda como estaba. |

### HU-INV-04: Eliminar un producto
**Como** usuario **quiero** quitar un producto que ya consumí o boté **para** que no ocupe mi inventario.

| Código | Criterio |
|---|---|
| CA-INV-04-01 | **Dado** "Editar Producto", **cuando** toco "Eliminar", **entonces** aparece "¿Desea eliminar el producto?" con "Eliminar" y "Cancelar". |
| CA-INV-04-02 | **Dado** la confirmación, **cuando** toco "Eliminar", **entonces** veo "Producto eliminado", regreso a la lista y el producto ya no está; si tenía foto, la foto también se borra de la nube. |
| CA-INV-04-03 | **Dado** la confirmación, **cuando** toco "Cancelar", **entonces** sigo en "Editar Producto" sin cambios. |

### HU-INV-05: Foto del producto
**Como** usuario **quiero** tomarle una foto al producto **para** reconocerlo de un vistazo en la lista.

| Código | Criterio |
|---|---|
| CA-INV-05-01 | **Dado** el formulario de un producto, **cuando** toco el botón de cámara, **entonces** se abre el visor de la app con un recuadro de encuadre y el botón de captura. |
| CA-INV-05-02 | **Dado** el visor, **cuando** capturo, **entonces** vuelvo al formulario con la foto en la zona de imagen; se guarda con el producto al tocar "Añadir al Inventario" o "Guardar". |
| CA-INV-05-03 | **Dado** que es la primera vez que uso la cámara, **entonces** la app me pide permiso; **si** lo niego, **entonces** veo un mensaje que explica para qué se usa, con un acceso a los ajustes del teléfono, y puedo guardar el producto sin foto. |
| CA-INV-05-04 | **Dado** un producto con foto, **cuando** toco la papelera de la zona de imagen, **entonces** aparece "¿Desea eliminar la imagen?"; al confirmar veo "Imagen eliminada" y la zona queda vacía. En la edición, la foto se borra de la nube al guardar. |
| CA-INV-05-05 | **Dado** un producto con foto, **cuando** tomo otra, **entonces** la nueva la reemplaza y la anterior se borra de la nube al guardar. |
| CA-INV-05-06 | **Dado** que la foto no se pudo subir, **entonces** el producto no se guarda a medias: veo un error y el formulario conserva los datos y la foto para reintentar. |

### HU-INV-06: Buscar y filtrar
**Como** usuario **quiero** buscar por nombre y filtrar por categoría **para** encontrar rápido un producto.

| Código | Criterio |
|---|---|
| CA-INV-06-01 | **Dado** "Mi Inventario", **cuando** toco la lupa, **entonces** se abre la búsqueda con la barra "Buscar producto…", la tarjeta "Usa tu voz", los filtros "Todos", "Frutas", "Lácteos", "Proteínas", "Verduras", "Cereales" y "Otro", y todos los productos con su punto de estado y "Vence: dd/mm/aaaa". |
| CA-INV-06-02 | **Dado** la búsqueda, **cuando** escribo "Car", **entonces** solo veo los productos cuyo nombre contiene "car" (por ejemplo, Carne), sin importar mayúsculas ni tildes ("lacteo" encuentra "Lácteo"). |
| CA-INV-06-03 | **Dado** la búsqueda, **cuando** elijo un filtro de categoría, **entonces** solo veo los productos de esa categoría y el filtro se ve seleccionado; "Todos" quita el filtro. |
| CA-INV-06-04 | **Dado** un texto y un filtro a la vez, **entonces** veo solo los productos que cumplen los dos (por ejemplo, "Car" + Proteínas → Carne). |
| CA-INV-06-05 | **Dado** un texto con comas, como "leche, tomate", **entonces** veo los productos que coinciden con **cualquiera** de los términos (Leche Entera y Tomate). Un texto sin comas es un solo término: "Carne Pollo" no coincide con "Carne". |
| CA-INV-06-06 | **Dado** que nada coincide, **entonces** veo "No hay productos que coincidan". |
| CA-INV-06-07 | **Dado** un texto escrito, **cuando** toco la "x" de la barra, **entonces** el texto se borra y vuelvo a ver todos los productos del filtro elegido. |
| CA-INV-06-08 | **Dado** un resultado, **cuando** lo toco, **entonces** se abre "Editar Producto" de ese producto. |

### HU-INV-07: Buscar varios ingredientes por voz
**Como** usuario **quiero** dictar varios ingredientes de una vez **para** saber cuáles tengo sin escribirlos uno a uno.

| Código | Criterio |
|---|---|
| CA-INV-07-01 | **Dado** la búsqueda, **cuando** toco el micrófono de "Usa tu voz — Dicta muchos ingredientes al mismo tiempo", **entonces** aparece sobre la pantalla el aviso "Habla ahora" con la animación de ondas. |
| CA-INV-07-02 | **Dado** que dicté, **mientras** se procesa, **entonces** el aviso muestra "…". |
| CA-INV-07-03 | **Dado** que dije "carne, pollo y leche", **entonces** el aviso se cierra, la barra muestra "carne, pollo, leche" y veo los productos que coinciden con cualquiera de los tres (CA-INV-06-05). Los separadores reconocidos son las pausas, las comas, "y" y "e". |
| CA-INV-07-04 | **Dado** un término en plural simple ("tomates", "limones"), **entonces** también coincide con el singular (Tomate, Limón). |
| CA-INV-07-05 | **Dado** que no se entendió nada, **entonces** el aviso muestra "No se entendió. Reinténtalo" con el botón "Reintentar", que vuelve a escuchar. |
| CA-INV-07-06 | **Dado** que es la primera vez, **entonces** se me pide permiso de micrófono; **si** lo niego, **entonces** veo un mensaje que explica para qué se usa, con un acceso a los ajustes, y la búsqueda por texto sigue funcionando. |
| CA-INV-07-07 | **Dado** el aviso de voz abierto, **cuando** toco fuera o vuelvo atrás, **entonces** se cancela la escucha y la barra queda como estaba. |
| CA-INV-07-08 | **Dado** un teléfono sin servicio de reconocimiento de voz, **entonces** la tarjeta "Usa tu voz" no se muestra. |
| CA-INV-07-09 | **Dado** un filtro de categoría elegido, **cuando** dicto, **entonces** el resultado respeta también ese filtro (CA-INV-06-04). |

### HU-INV-08: Avisos de vencimiento
**Como** usuario **quiero** que la app me avise de lo que está por vencer **para** consumirlo antes de que se dañe.

| Código | Criterio |
|---|---|
| CA-INV-08-01 | **Dado** que tengo productos que vencen hoy o en los próximos 3 días, **entonces** recibo una notificación al día, a las 9:00 (hora del teléfono). |
| CA-INV-08-02 | **Dado** un solo producto por vencer, **entonces** la notificación dice "«Leche Entera» vence pronto"; **dado** varios, **entonces** dice "«Leche Entera» y 2 productos más vencen pronto", nombrando el más próximo. |
| CA-INV-08-03 | **Dado** que no hay productos por vencer, **entonces** ese día no recibo notificación. |
| CA-INV-08-04 | **Dado** la notificación, **cuando** la toco, **entonces** se abre la app en "Mi Inventario" (o en el inicio de sesión, si la sesión ya no está activa). |
| CA-INV-08-05 | **Dado** un teléfono que pide permiso para notificar, **cuando** guardo mi primer producto, **entonces** se me pide el permiso; **si** lo niego, **entonces** el inventario funciona igual y no se me vuelve a pedir automáticamente. |
| CA-INV-08-06 | **Dado** que cerré sesión, **entonces** no recibo más avisos de esa cuenta en el teléfono. |

### HU-INV-09: Navegación principal
**Como** usuario **quiero** moverme entre el inventario y mi perfil **para** usar la app sin perderme.

| Código | Criterio |
|---|---|
| CA-INV-09-01 | **Dado** que inicio sesión o abro la app con sesión activa, **entonces** llego a "Mi Inventario". |
| CA-INV-09-02 | **Dado** "Mi Inventario" o "Perfil", **entonces** veo la barra inferior con "Inventario" y "Perfil", con la pestaña actual resaltada; tocar una pestaña cambia de pantalla sin apilar. |
| CA-INV-09-03 | **Dado** "Perfil", **entonces** veo el saludo con mi nombre y el botón "Cerrar sesión" (CA-05-01 a CA-05-03 siguen vigentes en esta pestaña). |
| CA-INV-09-04 | **Dado** una pantalla interna (búsqueda, añadir, editar o cámara), **entonces** se abre con la transición de empuje, tiene la flecha atrás y no muestra la barra inferior. |
| CA-INV-09-05 | **Dado** "Mi Inventario", **cuando** uso "atrás" del sistema, **entonces** la app se cierra (no vuelve a la pantalla de carga ni al inicio de sesión). |

## 5. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-INV-01 | Guardar los productos de cada usuario en la nube (nombre, vencimiento, categoría, y cantidad con unidad y foto opcionales) y mostrarlos actualizados. | HU-INV-01 a 04 |
| RF-INV-02 | Clasificar cada producto en Vencido, Próximo a vencer (0 a 3 días) o Fresco según la fecha del teléfono. | HU-INV-01, HU-INV-08 |
| RF-INV-03 | Validar los datos del producto en un solo lugar del dominio, igual para el alta y la edición. | HU-INV-02, HU-INV-03 |
| RF-INV-04 | Tomar fotos con la cámara dentro de la app, guardarlas en la nube y borrarlas cuando se eliminan, se reemplazan o se borra el producto. | HU-INV-04, HU-INV-05 |
| RF-INV-05 | Buscar por nombre sin distinguir mayúsculas ni tildes, con varios términos separados por comas (OR) y combinables con un filtro de categoría. | HU-INV-06 |
| RF-INV-06 | Convertir un dictado en español en una lista de ingredientes y usarla como búsqueda. | HU-INV-07 |
| RF-INV-07 | Avisar una vez al día de los productos por vencer y dejar de avisar al cerrar sesión. | HU-INV-08 |
| RF-INV-08 | Navegación principal con barra inferior (Inventario y Perfil); el inventario es el inicio de la sesión. | HU-INV-09 |

## 6. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-INV-01 | Privacidad (amplía RNF-02): cada usuario solo puede leer y escribir sus productos y sus fotos. Los datos se validan también en el servidor (tipos, longitudes, categoría, cantidad y unidad, y tamaño de la foto). |
| RNF-INV-02 | Las fotos se reducen antes de subir (lado mayor de 1080 px como máximo y unos 300 KB). El servidor rechaza archivos de más de 2 MB o que no sean imágenes. |
| RNF-INV-03 | Sin secretos (RNF-01): el reconocimiento de voz usa el servicio del sistema del teléfono en español de Colombia, sin claves propias. Ese servicio puede procesar el audio en línea; la Política de privacidad lo informa. |
| RNF-INV-04 | Accesibilidad (amplía RNF-04): el estado no se comunica solo con color (encabezado de sección y descripción "Vencido", "Próximo a vencer" o "Fresco" en cada fila); los filtros anuncian si están seleccionados; el aviso de voz y los mensajes de resultado se anuncian solos; los botones de solo ícono (lupa, lápiz, cámara, papelera, micrófono, "x") tienen descripción. |
| RNF-INV-05 | Sin conexión, toda operación que escribe muestra un error de red y conserva lo que el usuario escribió. |
| RNF-INV-06 | Adaptabilidad (spec 002): los formularios de producto caben y responden al teclado como los de autenticación, sin medidas fijas nuevas ni scroll programático; la lista y la búsqueda se desplazan. Se ven bien en modo claro y oscuro desde 360 dp de ancho. |
| RNF-INV-07 | Los permisos de cámara, micrófono y notificaciones se piden solo en el momento en que se usan y la app funciona si se niegan. |
| RNF-INV-08 | Todo texto visible está en recursos (RNF-03). |

## 7. Fuera de alcance

- **Precio** del producto. Pertenece a la futura spec de presupuesto; el modelo debe poder crecer sin romper los datos
  existentes. (El equipo decidió el 2026-10-14 incluir cantidad y unidad opcionales en esta spec, T002.)
- Convertir entre unidades o descontar cantidades al cocinar.
- Elegir fotos de la galería.
- Escáner de código de barras y reconocimiento del alimento por la foto.
- Añadir productos por voz (la voz solo busca).
- Uso completo sin conexión (cola de cambios pendientes).
- Inventario compartido entre varias personas del hogar.
- Pestañas "Recetas" y "Guardadas" (llegan con el agente de recomendación).
- Configurar la hora o el umbral de los avisos.

## 8. Diseño de referencia y desviaciones

Referencia: frames principales de la página "SmartFood" del Figma `z1AoQvluQcmx98gjP4vHF6`:
- **Lista:** "Inventario" `476:3095` e "Inventario Imagen" `476:3126`.
- **Alta y edición:** "Añadir Producto" `476:3664`, "Editar Producto" `476:3689`, "Foto Producto" `476:4056` y
  "Categoria" `476:4113`.
- **Diálogos:** "Guardar Producto" `476:3851`, "Eliminar Producto" `476:3787` y "Eliminar Imagen Inventario"
  `476:4021`.
- **Búsqueda:** "Busqueda Inventario" `476:3190` y sus variantes de texto, filtro, micrófono y error.
- **Voz:** "Grabadora" `476:4507`.

Los frames se leen desde el archivo de Figma, no desde capturas. Antes de empezar cualquier tarea de diseño, se
comprueba que el acceso al archivo funciona leyendo uno de estos frames. Si no funciona, no se implementa el diseño
hasta configurarlo (ver `AGENTS.md`, "Implementar diseños de Figma").

Desviaciones aprobadas por el equipo:
- **Barra inferior de 2 pestañas** (Inventario y Perfil), tomada de la sección "Natalia" del Figma, porque Recetas y
  Guardadas todavía no existen.
- **Botón "Añadir al Inventario" habilitado en naranja** (sección "Natalia"); los frames principales solo lo muestran
  deshabilitado.
- **Campos con borde** del design system en lugar de la línea inferior del Figma, por consistencia con
  autenticación y la adaptabilidad de la spec 002. Se conserva el ícono a la izquierda de cada campo, gris y con
  el tamaño y la separación del Figma. El texto de los campos usa el mismo tamaño que en autenticación, no el más
  pequeño del Figma de inventario (#62).
- **Fila opcional "Cantidad"** debajo de "Seleccionar categoría" (campo numérico más selector de unidad), y la
  cantidad en la fila de la lista junto a la etiqueta de categoría. El Figma no tiene estos elementos; se ajustan
  cuando exista un frame (T002).

## 9. Supuestos

- La fecha de vencimiento es un día sin hora; "hoy" es la fecha del teléfono.
- Un producto se guarda con un solo vencimiento; si hay dos paquetes con fechas distintas, se registran como dos
  productos.
- La plantilla de la Política de privacidad (spec 005) se amplía para mencionar las fotos de productos, el uso del
  micrófono y los avisos.
