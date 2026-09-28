# 0030 — Equipo obligatorio en las inscripciones

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0003](0003-alcance-del-mvp.md), [0023](0023-inscripcion-como-entidad.md)

## Contexto

Una inscripción relaciona un campeonato, un piloto y un equipo. En algunos contextos reales (karting amateur, pilotos privados) un piloto compite sin equipo, lo que obligaría a permitir que `team_id` sea nulo.

## Decisión

**El equipo es obligatorio.** La columna `team_id` es `NOT NULL`, la relación se declara `optional = false` y el DTO exige `teamId` con `@NotNull`.

El motivo es de alcance: Race Manager está pensado para **campeonatos organizados** del estilo de la Fórmula 1 o MotoGP, donde todo participante corre bajo un equipo. No es un olvido, es una restricción de dominio deliberada.

## Alternativas consideradas

- **Equipo opcional** (`team_id` anulable). Reflejaría más situaciones reales, a cambio de: un `LEFT JOIN FETCH` en lugar de `JOIN FETCH` al cargar la relación (un `JOIN` normal descartaría las inscripciones sin equipo), campos nulos en el DTO de respuesta, y un caso límite que tratar en la clasificación de equipos de la fase 11.

## Consecuencias

**Positivas**

- Sin casos límite en el cálculo de la clasificación de equipos: toda inscripción suma a algún equipo.
- Las tres relaciones de la entidad se tratan igual, sin excepciones.

**Negativas o costes**

- No se pueden registrar pilotos privados o sin equipo.
- Si en el futuro hiciera falta, el cambio sería una migración que elimine el `NOT NULL`, más adaptar la consulta a `LEFT JOIN FETCH` y la clasificación de equipos. Este ADR documenta por qué está así, para que ese cambio sea consciente.