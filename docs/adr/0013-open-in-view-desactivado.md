# 0013 — `open-in-view` desactivado

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0009](0009-diseno-de-entidades-jpa.md), [0012](0012-dtos-con-records-y-mappers-manuales.md)

## Contexto

Por defecto, Spring Boot mantiene abierta la sesión de Hibernate durante **toda** la petición HTTP (el patrón *Open Session in View*). Eso permite que una relación perezosa se cargue desde el controller o durante la serialización a JSON, lanzando consultas desde la capa de presentación sin que nadie las haya pedido explícitamente.

## Decisión

`spring.jpa.open-in-view: false`. La sesión solo existe dentro de las transacciones del servicio.

## Alternativas consideradas

- **Dejarlo activado.** Más cómodo al principio, pero oculta consultas a la base de datos fuera de la capa de servicio y hace muy difícil ver problemas de rendimiento como el N+1.

## Consecuencias

**Positivas**

- El servicio decide explícitamente qué datos carga.
- Encaja con el uso de DTOs: el mapeo a `Response` se hace dentro de la transacción, con los datos ya cargados.

**Negativas o costes**

- Acceder a una relación perezosa fuera del servicio lanza `LazyInitializationException`. Cuando haya relaciones, habrá que cargar lo necesario en el servicio de forma explícita (`JOIN FETCH` o `@EntityGraph`).
