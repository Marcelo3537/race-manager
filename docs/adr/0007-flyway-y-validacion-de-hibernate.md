# 0007 — Flyway para el esquema e Hibernate en modo `validate`

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0008](0008-convenciones-de-nombres-en-base-de-datos.md), [0019](0019-estrategia-de-tests.md)

## Contexto

El esquema de la base de datos tiene que evolucionar junto con el código y aplicarse igual en local, en los tests y en producción. Sin control de versiones, los cambios hechos a mano en un entorno no llegan a los demás y nadie sabe con certeza cómo está el esquema en cada sitio.

## Decisión

- **Flyway es el único responsable del esquema.** Los cambios se escriben como migraciones SQL `V<n>__<descripcion>.sql` en `src/main/resources/db/migration`. Flyway las aplica en orden al arrancar y registra en `flyway_schema_history` cuáles ha ejecutado, junto con un checksum de cada fichero.
- **Hibernate solo verifica:** `spring.jpa.hibernate.ddl-auto: validate`. Al arrancar compara las entidades con las tablas reales y se niega a iniciar si no coinciden.
- **Una migración aplicada no se edita nunca.** Cualquier corrección va en una migración nueva.
- **No se usa `flyway repair`** para silenciar un checksum distinto: actualiza la huella pero no aplica ningún cambio, y deja una migración que dice una cosa y una base de datos que tiene otra.

## Alternativas consideradas

- **`ddl-auto: update`.** Hibernate deduce el esquema de las entidades. No borra ni modifica columnas existentes, no se puede revisar lo que va a hacer y no deja registro. Peligroso en producción.
- **`ddl-auto: create-drop`.** Solo sirve para entornos desechables.
- **Scripts SQL manuales.** Sin historial ni aplicación automática.

## Consecuencias

**Positivas**

- El esquema está versionado en Git como el resto del código.
- La secuencia completa de migraciones se ejecuta desde cero en cada test de integración, así que se sabe que funciona sobre una base de datos vacía.
- Cualquier desajuste entre entidad y tabla se detecta al arrancar, no en producción.

**Negativas o costes**

- Las migraciones se escriben en SQL a mano.
- Editar por error una migración aplicada bloquea el arranque con un error de checksum. Es el comportamiento deseado, pero exige saber cómo resolverlo.
