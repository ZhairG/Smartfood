# Spec 003: Recuperación de contraseña real

| Campo | Valor |
|---|---|
| Feature | `003-recuperacion-real` |
| Estado | Implementada (v1) |
| Fecha | 2026-10-05 |
| Depende de | 001-autenticacion (HU-04), 002-adaptabilidad-ui |
| Afecta | Recuperación de contraseña (pasos 1, 2 y 3) |

> Los códigos de esta spec llevan el prefijo `REC` (`HU-REC-01`, `CA-REC-01-01`) para no chocar con los de la 001.

## 1. Contexto y motivación

En la v1, la recuperación de contraseña (HU-04 de la spec 001) es una **simulación**: acepta dos correos fijos y el
código `1234`, no envía ningún correo y la contraseña real no cambia. Un usuario que de verdad olvidó su contraseña
no tiene cómo recuperar su cuenta.

Esta spec convierte el flujo en uno **real** sin cambiar sus tres pasos ni su diseño:
1. El usuario escribe su correo y recibe un código de 4 dígitos.
2. Escribe el código.
3. Elige una nueva contraseña, que reemplaza a la anterior.

Además, protege el flujo contra dos abusos: descubrir qué correos tienen cuenta y adivinar el código a la fuerza.

Los criterios de la HU-04 siguen vigentes, **excepto CA-04-03**, que esta spec reemplaza (ver CA-REC-01-02).

## 2. Actores

- **Visitante**: persona sin sesión que olvidó su contraseña.
- **Atacante**: persona que intenta averiguar qué correos tienen cuenta o adivinar un código ajeno.

## 3. Historias de usuario y criterios de aceptación

### HU-REC-01: Recibir un código real
**Como** visitante que olvidó su contraseña **quiero** recibir un código en mi correo **para** demostrar que la cuenta
es mía.

| Código | Criterio |
|---|---|
| CA-REC-01-01 | **Dado** el paso 1 con un correo que tiene cuenta, **cuando** presiono "Enviar Código", **entonces** recibo en ese correo un mensaje de SmartFood con un código de 4 dígitos y paso al paso 2. |
| CA-REC-01-02 | **Dado** el paso 1 con un correo bien escrito **sin** cuenta, **cuando** presiono "Enviar Código", **entonces** paso al paso 2 igual que con un correo registrado, pero no se envía ningún correo. *(Reemplaza a CA-04-03: la app no revela qué correos están registrados.)* |
| CA-REC-01-03 | **Dado** el paso 2, **entonces** el subtítulo dice "Si el correo tiene una cuenta, te enviamos un código de 4 dígitos a:" seguido del correo. |
| CA-REC-01-04 | **Dado** un correo sin cuenta, **cuando** escribo cualquier código en el paso 2, **entonces** veo "Código incorrecto, inténtalo de nuevo". |
| CA-REC-01-05 | **Dado** que pedí un código nuevo ("Reenviar código"), **entonces** solo vale el último código; los anteriores se rechazan como incorrectos. |

### HU-REC-02: Cambiar la contraseña de verdad
**Como** visitante **quiero** que la nueva contraseña reemplace a la anterior **para** volver a entrar a mi cuenta.

| Código | Criterio |
|---|---|
| CA-REC-02-01 | **Dado** el paso 3 con un código válido, **cuando** presiono "Reestablecer Contraseña", **entonces** puedo iniciar sesión con la nueva contraseña y la anterior deja de funcionar. |
| CA-REC-02-02 | **Dado** que se cambió la contraseña, **entonces** las sesiones abiertas en otros dispositivos se cierran y el código usado deja de valer. |
| CA-REC-02-03 | **Dado** el paso 3, **cuando** el código venció, se agotaron sus intentos o ya no es válido, **entonces** vuelvo al paso 2 con las casillas vacías y veo el mensaje del error. |
| CA-REC-02-04 | **Dado** que alguien salta las validaciones de la app, **cuando** envía una contraseña que no cumple la política (RF-AUTH-04), **entonces** el cambio se rechaza con "La contraseña no cumple los requisitos de seguridad". |

### HU-REC-03: Protección contra abuso
**Como** usuario **quiero** que nadie pueda adivinar mi código ni llenar mi correo de mensajes **para** que mi cuenta
siga siendo mía.

| Código | Criterio |
|---|---|
| CA-REC-03-01 | **Dado** un código enviado hace más de 10 minutos, **cuando** lo uso, **entonces** veo "El código venció. Solicita uno nuevo". |
| CA-REC-03-02 | **Dado** un código, **cuando** fallo 5 veces, **entonces** el código se invalida y veo "Demasiados intentos. Solicita un código nuevo"; ni siquiera el código correcto vale ya. |
| CA-REC-03-03 | **Dado** que pedí un código hace menos de 60 segundos, **cuando** pido otro, **entonces** veo "Espera un momento antes de pedir otro código" y no se envía nada. |
| CA-REC-03-04 | **Dado** que se pidieron 5 códigos para el mismo correo en la última hora, **cuando** pido otro, **entonces** veo "Espera un momento antes de pedir otro código". |
| CA-REC-03-05 | Los límites de CA-REC-03-03 y CA-REC-03-04 se aplican igual exista o no la cuenta, para no revelar nada. |
| CA-REC-03-06 | **Dado** el paso 2, **cuando** el error es de código (incorrecto, vencido o con los intentos agotados), **entonces** las casillas se marcan en rojo. |
| CA-REC-03-07 | **Dado** que el correo con el código no se pudo enviar, **entonces** ese intento no cuenta para los límites de CA-REC-03-03 y CA-REC-03-04 y puedo volver a pedirlo enseguida. |

### HU-REC-04: Ver cuánto falta para pedir otro código
**Como** visitante **quiero** ver cuánto tiempo falta para poder pedir otro código **para** no tocar "Reenviar" sin
saber por qué no pasa nada.

| Código | Criterio |
|---|---|
| CA-REC-04-01 | **Dado** que se envió un código, **entonces** en el paso 2, en lugar del enlace "Reenviar código", veo una cuenta regresiva "Reenviar en 0:59" que baja cada segundo y no se puede tocar. Al llegar a cero vuelve el enlace "Reenviar código". |
| CA-REC-04-02 | **Dado** que el servidor rechaza la solicitud por los límites de HU-REC-03, **entonces** la cuenta regresiva muestra el tiempo real que falta, en minutos y segundos (por ejemplo, "Reenviar en 37:12"). |
| CA-REC-04-03 | **Dado** que vuelvo al paso 1 con el mismo correo mientras corre la espera, **entonces** el botón dice "Enviar Código en 0:42" y está deshabilitado. Si escribo otro correo, el botón se habilita. |
| CA-REC-04-04 | La cuenta regresiva usa los colores de acento de la app en modo claro y oscuro, y no cambia el alto del formulario: cada paso sigue cabiendo sin scroll (spec 002). |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-REC-01 | Generar un código aleatorio de 4 dígitos por solicitud y enviarlo al correo de la cuenta. | HU-REC-01 |
| RF-REC-02 | Guardar solo una huella del código (nunca el código), con su vencimiento y sus intentos restantes. | HU-REC-01, HU-REC-03 |
| RF-REC-03 | Verificar el código en el servidor y, en el paso 3, volver a verificarlo antes de cambiar la contraseña. | HU-REC-02 |
| RF-REC-04 | Cambiar la contraseña de la cuenta, cerrar sus otras sesiones e invalidar el código. | HU-REC-02 |
| RF-REC-05 | Responder igual exista o no la cuenta, en los tres pasos. | HU-REC-01, HU-REC-03 |
| RF-REC-06 | La recuperación simulada de la v1 se conserva como **modo demo** opcional, solo para builds de depuración, y se comporta igual que la real ante un correo sin cuenta. | HU-REC-01 |
| RF-REC-07 | El servidor informa cuánto falta para poder pedir otro código, tanto al enviarlo como al rechazar una solicitud por los límites. | HU-REC-04 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-REC-01 | Las credenciales del servidor de correo y de administración viven solo en el servidor (amplía RNF-01). |
| RNF-REC-02 | Ningún cliente puede leer ni escribir los datos de recuperación (amplía RNF-02). |
| RNF-REC-03 | El servidor corre en la misma región que la base de datos y con un límite de instancias, para mantener el costo en $0 dentro de la cuota gratuita. |
| RNF-REC-04 | El flujo se puede desarrollar y probar completo en local, sin tocar el proyecto real ni enviar correos. |

## 6. Fuera de alcance

- Verificar que las peticiones vienen de la app oficial (atestación del dispositivo).
- Cambiar la contraseña con la sesión iniciada (sigue fuera de alcance, como en la 001).
- Marcar el correo como verificado al recuperar la contraseña.

## 7. Supuestos

- El proyecto de Firebase está en el plan Blaze (prueba gratuita de Google Cloud hasta el 4 de enero de 2027; ver
  [plan 001 §6.5](../001-autenticacion/plan.md#65-ruta-a-una-recuperación-real-post-entrega)).
- El equipo tiene una cuenta de Gmail con verificación en 2 pasos para generar una contraseña de aplicación.
