# Correcciones del póster (feria de proyectos de aula)

Revisión del póster `SmartFood.pdf` (32.ª Feria de Proyectos de Aula, FCNI) del 2026-11-20, a pedido del informe de IA
y de [`docs/pendientes.md`](../pendientes.md) §8. El póster no vive en el repositorio: estas son las frases para
pegarlas en el diseño.

## 1. Objetivos específicos (cambio obligatorio)

Los tres objetivos específicos actuales son de otro proyecto («usuarios activos, rendimiento del servidor y uso de CPU
en una plataforma web», «niveles de tráfico y demanda», «estabilidad y saturación del servidor»). SmartFood no tiene
servidor web propio ni pruebas de carga.

**Texto propuesto** (cuatro viñetas, del mismo largo que las actuales):

- Formular la planificación de comidas de 3 días como un problema de satisfacción de restricciones con optimización:
  variables, dominios, restricciones duras y función de utilidad.
- Implementar un agente de búsqueda con backtracking, *forward checking* y ramificación y poda, integrado en una app
  Android que gestiona el inventario y los vencimientos.
- Comparar el agente con A\*, recocido simulado y una búsqueda voraz en calidad del plan, nodos, memoria y tiempo,
  sobre inventarios sintéticos.
- Medir qué parte de los productos próximos a vencer aprovecha el plan, frente a dos líneas base.

**Versión corta**, si no cabe:

- Formular el plan de 3 días como un problema de restricciones con optimización.
- Implementar el agente de búsqueda (backtracking con poda) dentro de la app Android.
- Comparar el agente con A\*, recocido simulado y voraz, y medir el aprovechamiento de lo que vence.

## 2. Frases que conviene matizar

| Sección | Dice | Propuesta | Por qué |
|---|---|---|---|
| Conclusiones | «Contribuye a la reducción del desperdicio de alimentos.» | «En 30 inventarios sintéticos, el plan usa el 88,9 % de los productos próximos a vencer que alguna receta puede aprovechar, frente al 74,7 % y 85,0 % de dos métodos simples.» | No se midió con usuarios ni despensas reales; el 88,9 % cuenta productos usados al menos en parte, no kilogramos (evaluación §9.1). |
| Metodología, Pruebas | «Validación funcional, usabilidad y rendimiento.» | «Validación funcional, pruebas contra fuerza bruta y rendimiento.» | No hubo pruebas de usabilidad con usuarios; sí pruebas de optimalidad contra fuerza bruta y de rendimiento. |
| Encabezado | «Asignatura: Dispositivos Móviles» | Confirmar con Zhair: si el póster también se presenta en Inteligencia Artificial, poner ambas asignaturas. | El informe de IA revisó este póster como evidencia del curso de IA. |

## 3. Lo que está bien y no se toca

- El resumen, el objetivo general, la metodología por fases y las capturas de la app.
- Las referencias (Jetpack Compose, Firebase y Russell y Norvig).
