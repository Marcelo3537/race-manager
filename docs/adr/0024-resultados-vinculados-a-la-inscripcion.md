# 0024 — Los resultados apuntan a la inscripción

- **Estado:** Aceptada, pendiente de implementar
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0023](0023-inscripcion-como-entidad.md), [0025](0025-reglas-de-puntuacion-configurables.md)

## Contexto

Un resultado es lo que obtiene un piloto en una carrera. Para la clasificación de equipos hay que saber a qué equipo se suman los puntos de cada resultado, y un mismo piloto puede correr con equipos distintos en campeonatos distintos.

Ejemplo: Alonso puntúa en 2025 con Aston Martin y en 2026 con Ferrari. La clasificación de constructores de 2025 tiene que sumar sus puntos a Aston Martin, no a Ferrari.

## Decisión

`race_result` referencia a `championship_entry`, no a `driver`: `race_id`, `entry_id`, `position`, `points`, `fastest_lap` y `status`. Cada fila es el resultado de **una inscripción en una carrera**; una carrera con veinte pilotos genera veinte filas.

Los puntos los calcula el backend a partir de la posición y de las reglas del campeonato ([0025](0025-reglas-de-puntuacion-configurables.md)); el cliente no los envía.

## Alternativas consideradas

- **`race_result.driver_id`.** Para saber el equipo habría que buscar, además, en qué inscripción estaba ese piloto en el campeonato de esa carrera. La información existe, pero hay que reconstruirla con saltos adicionales en cada consulta.

## Consecuencias

**Positivas**

- Cada resultado sabe directamente a qué campeonato y a qué equipo pertenece. La clasificación de equipos es una suma agrupada por equipo sobre las inscripciones del campeonato.

**Negativas o costes**

- Para obtener el piloto de un resultado hay que pasar por la inscripción.
