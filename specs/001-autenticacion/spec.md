# Spec 001: Autenticación de usuarios

| Campo | Valor |
|---|---|
| Feature | `001-autenticacion` |
| Estado | Implementada (v1) |
| Fecha | 2026-10-04 |
| Entrega | Martes 6 de octubre de 2026 |
| Depende de | Ninguna (es el módulo 1) |
| Habilita | 002-inventario, 003-recetas, 004-agente-recomendador |

## 1. Contexto y motivación

SmartFood guardará datos personales de cada hogar: inventario, fechas de vencimiento, presupuesto e historial
de recetas aceptadas, que el agente de recomendación usa para aprender. Por eso cada persona necesita una
**cuenta propia** que identifique sus datos y los proteja de otros usuarios.

Este módulo cubre todo lo necesario para entrar a la app y salir de ella: crear la cuenta, iniciar sesión,
mantener la sesión abierta, recuperar el acceso y cerrar la sesión.

## 2. Actores

- **Visitante**: persona sin sesión iniciada.
- **Usuario**: persona con cuenta y sesión iniciada.

## 3. Historias de usuario y criterios de aceptación

### HU-01: Registro de cuenta
**Como** visitante **quiero** crear una cuenta con mi nombre, correo y contraseña **para** guardar mi
inventario y mis recetas de forma privada.

| Código | Criterio |
|---|---|
| CA-01-01 | **Dado** que estoy en "Crea tu cuenta", **cuando** dejo vacío el nombre, el correo o la contraseña y presiono "Crear Cuenta", **entonces** veo "Los campos no pueden estar vacíos", los campos vacíos se marcan en rojo y no se crea nada. |
| CA-01-02 | **Dado** un correo sin formato válido (p. ej. `pepito@`), **cuando** presiono "Crear Cuenta", **entonces** veo "Ingresa un correo electrónico válido" y el campo de correo se marca en rojo. |
| CA-01-03 | **Dado** que escribo en el campo de contraseña, **cuando** el campo tiene el foco, **entonces** veo una lista de requisitos (mínimo 8 caracteres, una mayúscula, un carácter especial) que se marca en tiempo real. |
| CA-01-04 | **Dado** una contraseña que no cumple los requisitos, **cuando** presiono "Crear Cuenta", **entonces** veo "La contraseña no cumple los requisitos de seguridad" y no se crea la cuenta. |
| CA-01-05 | **Dado** que no acepto los Términos y condiciones, **entonces** el botón "Crear Cuenta" está deshabilitado. |
| CA-01-06 | **Dado** un correo que ya tiene cuenta, **cuando** presiono "Crear Cuenta", **entonces** veo "Ya existe una cuenta registrada con este correo electrónico". |
| CA-01-07 | **Dado** que todos los datos son válidos, **cuando** presiono "Crear Cuenta", **entonces** se crea la cuenta, se guarda mi perfil (nombre y correo), se envía un correo de verificación, la sesión queda cerrada y veo la pantalla "¡Cuenta creada con éxito!". |
| CA-01-08 | **Dado** que estoy en "¡Cuenta creada con éxito!", **cuando** presiono "Continuar", **entonces** llego a "Bienvenido" (inicio de sesión) y no puedo volver atrás al formulario de registro. |
| CA-01-09 | **Dado** que se crea la cuenta pero falla el guardado del perfil, **entonces** la cuenta se elimina (no quedan cuentas sin perfil) y veo el error correspondiente. |

### HU-02: Inicio de sesión
**Como** usuario registrado **quiero** iniciar sesión con mi correo y contraseña **para** acceder a mis datos.

| Código | Criterio |
|---|---|
| CA-02-01 | **Dado** que dejo vacío el correo o la contraseña, **cuando** presiono "Iniciar Sesión", **entonces** veo "Los campos no pueden estar vacíos" y los campos vacíos se marcan en rojo. |
| CA-02-02 | **Dado** un correo con formato inválido, **cuando** presiono "Iniciar Sesión", **entonces** veo "Ingresa un correo electrónico válido". |
| CA-02-03 | **Dado** una contraseña incorrecta, **cuando** presiono "Iniciar Sesión", **entonces** veo "Correo o contraseña incorrectos" y ambos campos se marcan en rojo. |
| CA-02-04 | **Dado** un correo sin cuenta, **cuando** presiono "Iniciar Sesión", **entonces** veo "No existe una cuenta registrada con este correo electrónico". |
| CA-02-05 | **Dado** que mis credenciales son correctas, **cuando** presiono "Iniciar Sesión", **entonces** llego a la pantalla de inicio (Home) y el botón "atrás" no me devuelve al login. |
| CA-02-06 | **Dado** que se produce un error, **cuando** edito cualquier campo, **entonces** el mensaje de error desaparece. |
| CA-02-07 | **Dado** que hay una operación en curso, **entonces** el botón muestra "Iniciando", los campos se deshabilitan y un segundo toque no lanza otra petición. |

### HU-03: Sesión persistente ("Recuérdame")
**Como** usuario **quiero** que la app recuerde mi sesión **para** no escribir mis datos cada vez que la abro.

| Código | Criterio |
|---|---|
| CA-03-01 | **Dado** que inicié sesión con "Recuérdame" activo (es el valor por defecto), **cuando** cierro y vuelvo a abrir la app, **entonces** después del splash llego directo a Home. |
| CA-03-02 | **Dado** que inicié sesión con "Recuérdame" desactivado, **cuando** vuelvo a abrir la app, **entonces** la sesión se cierra y después del splash llego al inicio de sesión. |
| CA-03-03 | **Dado** que no hay sesión, **cuando** abro la app, **entonces** después del splash llego al inicio de sesión. |
| CA-03-04 | El splash se muestra al menos 2 segundos, aunque la sesión se resuelva antes. |

### HU-04: Recuperación de contraseña (**SIMULADA**)
**Como** usuario que olvidó su contraseña **quiero** recuperar el acceso con un código enviado a mi correo
**para** no perder mi cuenta.

> ⚠️ **Alcance v1: el flujo es una simulación de interfaz con datos de prueba fijos.** No se envía ningún correo
> y la contraseña real **no cambia**. La razón es que generar y validar un código OTP de forma segura exige un
> backend (ver `plan.md`, ADR-01). La pantalla, la navegación y las validaciones son definitivas, así que
> pasar a la versión real solo requiere cambiar la implementación del repositorio.

**Datos de prueba**

| Dato | Valor |
|---|---|
| Correos con cuenta simulada | `demo@smartfood.com`, `prueba@smartfood.com` |
| Código de verificación válido | `1234` |

| Código | Criterio |
|---|---|
| CA-04-01 | **Dado** el paso 1 (correo), **cuando** presiono "Enviar Código" con el correo vacío, **entonces** veo "Correo no puede estar vacío". |
| CA-04-02 | **Dado** el paso 1, **cuando** el correo tiene formato inválido, **entonces** veo "Ingresa un correo electrónico válido". |
| CA-04-03 | **Dado** el paso 1, **cuando** el correo no pertenece a una cuenta, **entonces** veo "No encontramos una cuenta con este correo". |
| CA-04-04 | **Dado** el paso 1 con un correo con cuenta, **cuando** presiono "Enviar Código", **entonces** paso al paso 2, que muestra mi correo, y el indicador de pasos avanza. |
| CA-04-05 | **Dado** el paso 2 (código), **cuando** presiono "Verificar Código" sin escribir nada, **entonces** veo "Código no puede estar vacío". |
| CA-04-06 | **Dado** el paso 2, **cuando** el código está incompleto o no coincide, **entonces** veo "Código incorrecto, inténtalo de nuevo" y las casillas se marcan en rojo. |
| CA-04-07 | **Dado** el paso 2 con el código correcto, **cuando** presiono "Verificar Código", **entonces** paso al paso 3. |
| CA-04-08 | **Dado** el paso 2, **cuando** presiono "¿Correo incorrecto?…", **entonces** vuelvo al paso 1 conservando el correo y con el código borrado. |
| CA-04-09 | **Dado** el paso 2, **cuando** presiono "Reenviar código", **entonces** se solicita de nuevo, se borra el código escrito y sigo en el paso 2. |
| CA-04-10 | **Dado** el paso 3 (nueva contraseña), **entonces** el botón "Reestablecer Contraseña" se habilita solo cuando la contraseña cumple los 3 requisitos y la confirmación coincide. La lista de requisitos se marca en tiempo real. |
| CA-04-11 | **Dado** el paso 3 válido, **cuando** presiono "Reestablecer Contraseña", **entonces** veo "¡Contraseña actualizada!" y "Continuar" me lleva al inicio de sesión sin poder volver atrás al flujo. |
| CA-04-12 | **Dado** cualquier paso, **cuando** presiono "Iniciar sesión" (volver), **entonces** regreso al inicio de sesión y, si vuelvo a entrar a la recuperación, el flujo empieza en el paso 1. |

### HU-05: Pantalla de inicio y cierre de sesión
**Como** usuario **quiero** ver que entré con mi cuenta y poder cerrar sesión **para** proteger mis datos en un
celular compartido.

| Código | Criterio |
|---|---|
| CA-05-01 | **Dado** que inicié sesión, **entonces** Home me saluda con mi nombre ("Hola, Pepito"), leído de mi perfil. |
| CA-05-02 | **Dado** que mi perfil no se puede leer, **entonces** Home muestra un saludo genérico ("Hola") sin fallar. |
| CA-05-03 | **Dado** que estoy en Home, **cuando** presiono "Cerrar Sesión", **entonces** la sesión se cierra, llego al inicio de sesión y "atrás" no me devuelve a Home. |

### HU-06: Falta de conexión
| Código | Criterio |
|---|---|
| CA-06-01 | **Dado** que no hay internet, **cuando** intento iniciar sesión o registrarme, **entonces** veo el diálogo "Sin conexión a Internet" con el botón "Reconectar". |
| CA-06-02 | **Dado** el diálogo de desconexión, **cuando** presiono "Reconectar", **entonces** se reintenta la misma acción con los datos que ya escribí. |

## 4. Requisitos funcionales

| Código | Requisito | Historias |
|---|---|---|
| RF-AUTH-01 | Crear cuentas con correo y contraseña. | HU-01 |
| RF-AUTH-02 | Guardar el perfil del usuario (nombre completo, correo y fecha de creación) asociado a su cuenta. | HU-01, HU-05 |
| RF-AUTH-03 | Enviar un correo de verificación al registrarse. La verificación **no** es obligatoria para entrar en la v1. | HU-01 |
| RF-AUTH-04 | Validar el formato del correo y la política de contraseña: 8 caracteres o más, al menos una mayúscula y al menos un carácter no alfanumérico. | HU-01, HU-02, HU-04 |
| RF-AUTH-05 | Iniciar y cerrar sesión. | HU-02, HU-05 |
| RF-AUTH-06 | Recordar la preferencia "Recuérdame" en el dispositivo y decidir el destino inicial según la sesión. | HU-03 |
| RF-AUTH-07 | Flujo de recuperación de 3 pasos **simulado** con datos fijos e intercambiable por uno real. | HU-04 |
| RF-AUTH-08 | Traducir cada falla técnica a un error de negocio con un mensaje entendible. | HU-01, HU-02, HU-06 |

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-01 | Ningún secreto (API keys de terceros, contraseñas) en el código fuente ni en el APK. |
| RNF-02 | Cada usuario solo puede leer y escribir su propio perfil (reglas de Firestore). |
| RNF-03 | Todos los textos visibles están en `strings.xml`, en español. |
| RNF-04 | Accesibilidad: los íconos informativos tienen `contentDescription` y los errores se anuncian con `liveRegion`. |
| RNF-05 | Android 7.0 (API 24) o superior y orientación vertical. Los formularios se desplazan para no quedar tapados por el teclado. **Ampliado por la [spec 002](../002-adaptabilidad-ui/spec.md):** los formularios caben sin scroll desde 360×720 dp y el foco y el teclado responden sin retrasos. |
| RNF-06 | Las pantallas respetan el diseño de Figma del equipo (colores, tipografía y transiciones de empuje de 600 ms). |

## 6. Fuera de alcance (v1)

- Envío real del código de recuperación y cambio real de la contraseña (se simulan; ver HU-04).
- Inicio de sesión con Google, Facebook o teléfono.
- Exigir el correo verificado para entrar.
- Editar el perfil o eliminar la cuenta desde la app.
- Pantalla independiente de "sin conexión" (se usa un diálogo).

## 7. Supuestos

- El proyecto de Firebase tiene habilitados **Authentication (correo/contraseña)** y **Cloud Firestore**.
- Los textos legales de Términos y Política de privacidad se redactarán en otra feature; por ahora los enlaces
  son decorativos.
