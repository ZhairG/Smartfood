# Spec 005: Términos y condiciones y Política de privacidad

| Campo | Valor |
|---|---|
| Feature | `005-documentos-legales` |
| Estado | Implementada y desplegada (v1, texto en vista previa) |
| Fecha | 2026-10-06 |
| Depende de | 001-autenticacion (HU-01), 002-adaptabilidad-ui |
| Afecta | Registro (casilla de Términos, CA-01-05) y perfil del usuario |

> Los códigos de esta spec llevan el prefijo `LEG` (`HU-LEG-01`, `CA-LEG-01-01`) para no chocar con los de otras specs.

## 1. Contexto y motivación

Para crear una cuenta hay que aceptar los "Términos y condiciones" y la "Política de privacidad" (CA-01-05), pero en
la v1 esos enlaces son decorativos (spec 001 §7): la persona acepta algo que no puede leer.

Además, SmartFood guarda datos personales (nombre, correo y, más adelante, inventario y hábitos de compra). La Ley 1581
de 2012 (habeas data) pide informar la política de tratamiento y **poder demostrar** que el titular la autorizó.

El texto definitivo todavía no está redactado. Esta spec deja lista la experiencia completa con un **texto plantilla
marcado como vista previa**, para cambiar solo el contenido cuando llegue.

## 2. Actores

- **Visitante**: persona que está creando su cuenta.
- **Equipo de SmartFood**: necesita saber qué versión de los documentos aceptó cada usuario.

## 3. Historias de usuario y criterios de aceptación

### HU-LEG-01: Leer los documentos antes de aceptar
**Como** visitante **quiero** leer los Términos y condiciones y la Política de privacidad desde el registro **para**
saber qué acepto antes de crear mi cuenta.

| Código | Criterio |
|---|---|
| CA-LEG-01-01 | **Dado** que estoy en "Crea tu cuenta", **cuando** toco "Términos y condiciones", **entonces** se abre la pantalla "Términos y condiciones" con la transición de empuje y la casilla **no** cambia. |
| CA-LEG-01-02 | **Dado** que estoy en "Crea tu cuenta", **cuando** toco "Política de privacidad", **entonces** se abre la pantalla "Política de privacidad" con la transición de empuje y la casilla **no** cambia. |
| CA-LEG-01-03 | **Dado** que abrí un documento desde el registro, **cuando** vuelvo (flecha de la barra o "atrás" del sistema), **entonces** regreso a "Crea tu cuenta" con los campos y la casilla tal como los dejé. |
| CA-LEG-01-04 | **Dado** el registro, **cuando** toco la casilla o el texto "Acepto los", **entonces** la aceptación se alterna como antes (CA-01-05 sigue vigente). |
| CA-LEG-01-05 | **Dado** que el texto es una plantilla, **entonces** la pantalla muestra el aviso "Vista previa: el contenido definitivo está en redacción" y la versión del documento. |
| CA-LEG-01-06 | **Dado** un documento abierto, **entonces** se lee de arriba abajo con desplazamiento, en modo claro y oscuro, desde 360 dp de ancho, y los títulos de sección se anuncian como encabezados. *(Los documentos no son formularios: no les aplica el "sin scroll" de la spec 002.)* |
| CA-LEG-01-07 | **Dado** un lector de pantalla, **cuando** el foco está en la casilla, **entonces** ofrece las acciones "Abrir Términos y condiciones" y "Abrir Política de privacidad". |

### HU-LEG-02: Constancia de aceptación
**Como** equipo de SmartFood **queremos** guardar qué versión de los documentos aceptó cada persona y cuándo **para**
demostrar la autorización del tratamiento de datos.

| Código | Criterio |
|---|---|
| CA-LEG-02-01 | **Dado** un registro exitoso, **entonces** mi perfil guarda la versión vigente de los documentos y la fecha de aceptación, tomada del servidor. |
| CA-LEG-02-02 | **Dado** un cliente que intenta crear un perfil sin la versión aceptada o con una fecha distinta de la del servidor, **entonces** el perfil se rechaza. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-LEG-01 | Mostrar cada documento en su propia pantalla, abierta desde los enlaces del registro, sin perder el formulario. | HU-LEG-01 |
| RF-LEG-02 | Identificar el contenido con una versión única para los dos documentos, que se aceptan con una sola casilla. | HU-LEG-01, HU-LEG-02 |
| RF-LEG-03 | Guardar en el perfil la versión aceptada y la fecha del servidor, y exigirlas al crear el perfil. | HU-LEG-02 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-LEG-01 | Todo el texto de los documentos está en recursos de texto (RNF-03), separado del código, para reemplazarlo sin tocar la pantalla. |
| RNF-LEG-02 | Accesibilidad: títulos de sección como encabezados, enlaces disponibles como acciones del lector de pantalla y botón atrás con descripción (amplía RNF-04). |

## 6. Fuera de alcance

- El texto legal definitivo (lo redacta el equipo; esta spec deja la plantilla).
- Pedir de nuevo la aceptación cuando cambie la versión.
- Abrir los documentos desde Home o desde un perfil.
- Descargar los documentos en PDF.

## 7. Supuestos

- La estructura de la Política de privacidad sigue los contenidos mínimos de la Ley 1581 de 2012 y el Decreto 1377 de
  2013 (responsable, finalidad, derechos del titular y procedimiento para ejercerlos).
