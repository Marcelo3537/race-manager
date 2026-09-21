# 0010 — Enums persistidos como texto

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0014](0014-validacion-en-capas.md)

## Contexto

Varios conceptos del dominio tienen un conjunto cerrado de valores (el estado de un campeonato, de una carrera, de un resultado). JPA guarda los enums por defecto como su **posición** dentro de la declaración.

## Decisión

- Los enums se anotan con `@Enumerated(EnumType.STRING)` y se guardan por su nombre en una columna `VARCHAR`.
- La columna lleva una restricción `CHECK` que enumera los valores válidos (por ejemplo, `ck_championship_status`).

## Alternativas consideradas

- **`EnumType.ORDINAL` (el valor por defecto).** Guarda 0, 1, 2... Si alguien inserta un valor nuevo en medio del enum, todas las filas existentes cambian de significado en silencio, sin error.
- **Tipo `ENUM` nativo de PostgreSQL.** Añadir valores exige migraciones específicas y configuración adicional en Hibernate.

## Consecuencias

**Positivas**

- Reordenar el enum es inofensivo.
- La tabla es legible al consultarla a mano.
- Un `INSERT` manual con un valor inventado lo rechaza la base de datos.

**Negativas o costes**

- Ocupa más espacio que un entero (irrelevante a esta escala).
- Añadir un valor al enum requiere una migración nueva que actualice el `CHECK`.
