# 0008 — Convenciones de nombres en la base de datos

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0007](0007-flyway-y-validacion-de-hibernate.md), [0009](0009-diseno-de-entidades-jpa.md)

## Contexto

Java y PostgreSQL tienen convenciones de nombres distintas, y PostgreSQL convierte a minúsculas todo identificador que no vaya entre comillas. Además, los errores de restricciones aparecen en los logs con el nombre de la restricción, así que ese nombre tiene que ser legible.

## Decisión

- **Tablas en singular y `snake_case`:** `championship`, `team`, `driver`, `circuit`.
- **Columnas en `snake_case`** (`date_of_birth`, `constructor_name`, `length_km`) y campos Java en `camelCase`. La estrategia de nombres de Spring Boot los empareja sola, sin `@Column(name = ...)`.
- **La tabla de usuarios se llamará `app_user`**, porque `USER` es palabra reservada en PostgreSQL.
- **Restricciones siempre con nombre explícito:** `uk_<tabla>_<columnas>` para unicidad y `ck_<tabla>_<columna>` para comprobaciones. Ejemplos: `uk_championship_name_season`, `ck_circuit_length_km`.
- **Nombres de dominio sin ambigüedad:** `constructorName` en lugar de `constructor`; las unidades forman parte del nombre (`lengthKm`).
- `@Table(name = ...)` se escribe siempre, aunque coincida con el nombre de la clase.

## Alternativas consideradas

- **Dejar que PostgreSQL nombre las restricciones.** Genera nombres automáticos que no dicen nada cuando saltan en un log.
- **Identificadores entre comillas para conservar mayúsculas.** Obligaría a entrecomillar en cada consulta manual.

## Consecuencias

**Positivas**

- Los errores de restricciones indican de inmediato la tabla y las columnas implicadas.
- Entidades y tablas se emparejan sin configuración extra.

**Negativas o costes**

- Es fácil arrastrar nombres al copiar una migración de otra entidad (ocurrió con una restricción de `team` que conservaba el prefijo `championship`). Hay que revisar línea a línea.
- Una columna escrita en `camelCase` en SQL rompe el arranque: se crea en minúsculas y no coincide con lo que espera Hibernate.
