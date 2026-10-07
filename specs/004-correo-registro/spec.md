# Spec 004: Correo de bienvenida al registrarse

| Campo | Valor |
|---|---|
| Feature | `004-correo-registro` |
| Estado | Implementada (v1) |
| Fecha | 2026-10-06 |
| Depende de | 001-autenticacion (HU-01), 003-recuperacion-real (servidor y correo) |
| Afecta | Registro (CA-01-07, RF-AUTH-03) |

> Los códigos de esta spec llevan el prefijo `MAIL` (`HU-MAIL-01`, `CA-MAIL-01-01`) para no chocar con los de otras specs.

## 1. Contexto y motivación

En la v1, al crear la cuenta la app pide el correo de verificación genérico de la plataforma (CA-01-07). Ese correo no
lleva la marca de SmartFood, no saluda a la persona y suele llegar en inglés o a la carpeta de spam. Es el primer
contacto del usuario con el producto.

Esta spec lo reemplaza por **un solo correo de bienvenida de SmartFood**, que saluda por el nombre e incluye el
enlace para verificar el correo. Lo envía el servidor, con el mismo canal de correo de la recuperación (spec 003).

La verificación sigue siendo **informativa**, como en RF-AUTH-03: no se exige para entrar.

## 2. Actores

- **Visitante**: persona que acaba de crear su cuenta.
- **Atacante**: persona que intenta usar el envío para mandar correos a terceros o repetirlos.

## 3. Historias de usuario y criterios de aceptación

### HU-MAIL-01: Recibir la bienvenida y verificar mi correo
**Como** visitante que acaba de registrarse **quiero** recibir un correo de bienvenida de SmartFood **para** saber
que mi cuenta quedó creada y confirmar que el correo es mío.

| Código | Criterio |
|---|---|
| CA-MAIL-01-01 | **Dado** un registro exitoso, **entonces** recibo en el correo de la cuenta un mensaje de SmartFood que me saluda por mi nombre y trae el botón "Verificar mi correo". |
| CA-MAIL-01-02 | **Dado** el correo de bienvenida, **cuando** abro el enlace "Verificar mi correo", **entonces** mi correo queda verificado. |
| CA-MAIL-01-03 | **Dado** un registro exitoso, **entonces** recibo un solo correo: ya no llega el correo de verificación genérico. |
| CA-MAIL-01-04 | **Dado** que el correo de bienvenida no se pudo enviar, **entonces** la cuenta sigue creada, veo "¡Cuenta creada con éxito!" y puedo iniciar sesión. |

### HU-MAIL-02: Protección contra abuso
**Como** equipo de SmartFood **queremos** que el envío no se pueda usar para mandar correos repetidos ni a terceros
**para** proteger a las personas y la reputación del remitente.

| Código | Criterio |
|---|---|
| CA-MAIL-02-01 | **Dado** que ya se envió la bienvenida de una cuenta, **cuando** su perfil se vuelve a escribir o se recrea, **entonces** no se envía otro correo. |
| CA-MAIL-02-02 | **Dado** un perfil cuyo correo no coincide con el de la cuenta, **entonces** la bienvenida se envía solo al correo de la cuenta. |
| CA-MAIL-02-03 | **Dado** un nombre con etiquetas HTML (por ejemplo `<b>Pepito</b>`), **entonces** el correo lo muestra como texto, sin interpretarlo. |
| CA-MAIL-02-04 | **Dado** una cuenta que ya tiene el correo verificado o que ya no existe, **entonces** no se envía nada. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-MAIL-01 | Al guardarse el perfil de una cuenta nueva, enviar un correo de bienvenida con el nombre del perfil y un enlace de verificación del correo. | HU-MAIL-01 |
| RF-MAIL-02 | Enviar como máximo una bienvenida por cuenta, siempre al correo de la cuenta. | HU-MAIL-02 |
| RF-MAIL-03 | La app deja de pedir el correo de verificación genérico. **Reemplaza la forma de cumplir RF-AUTH-03.** | HU-MAIL-01 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-MAIL-01 | Las credenciales del servidor de correo viven solo en el servidor (amplía RNF-01 y RNF-REC-01). |
| RNF-MAIL-02 | En el entorno local el correo no se envía: el enlace de verificación queda en el log (amplía RNF-REC-04). |
| RNF-MAIL-03 | Mismo servidor, región y límite de instancias que la spec 003, para mantener el costo en $0. |
| RNF-MAIL-04 | Ningún cliente puede leer ni escribir el registro de bienvenidas enviadas. |

## 6. Fuera de alcance

- Exigir el correo verificado para iniciar sesión.
- Un botón "Reenviar correo de verificación" en la app.
- Una página propia para confirmar la verificación (se usa la página de acción estándar de la plataforma).
- Reintentar automáticamente un envío fallido.

## 7. Supuestos

- Las funciones de la spec 003 están desplegadas y los secretos del correo configurados.
- La página de acción de la plataforma está configurada en español (consola de Firebase → Authentication →
  Plantillas → idioma).
