# 0025 — Reglas de puntuación configurables por campeonato

- **Estado:** Aceptada, pendiente de implementar
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0024](0024-resultados-vinculados-a-la-inscripcion.md)

## Contexto

Distintos campeonatos reparten puntos de forma distinta (la Fórmula 1 da 25, 18, 15... pero un campeonato de karting puede dar 30, 20, 15). La aplicación no debería estar escrita solo para la Fórmula 1.

## Decisión

Una tabla `scoring_rule` con `championship_id`, `position` y `points`: **una fila por posición que puntúa**, y cada campeonato con su propio sistema. Una posición sin fila da cero puntos.

El cálculo de puntos es lógica de negocio en el servicio y se cubre con tests unitarios.

## Alternativas consideradas

- **Sistema de la Fórmula 1 fijo en el código.** Un `switch` con números mágicos que solo sirve para un tipo de campeonato.
- **Un sistema de puntuación reutilizable (`scoring_system`) compartido por varios campeonatos.** Más flexible, pero con tres tablas y una consulta más en cada cálculo. Se deja como posible refactor futuro.

## Consecuencias

**Positivas**

- Admite campeonatos con reglas distintas sin tocar código.
- La lógica de puntuación es independiente y fácil de probar.

**Negativas o costes**

- Al crear un campeonato hay que definir sus reglas; habrá que decidir si se ofrece un sistema por defecto.
