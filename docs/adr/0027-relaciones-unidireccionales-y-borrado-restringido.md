# 0027 — Relaciones unidireccionales, carga perezosa y borrado restringido

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0009](0009-diseno-de-entidades-jpa.md), [0013](0013-open-in-view-desactivado.md), [0018](0018-codigos-de-estado-y-respuestas.md), [0026](0026-dependencias-entre-servicios.md)

## Contexto

`Race` es la primera entidad con relaciones: pertenece a un campeonato y se disputa en un circuito. Esa primera relación obliga a decidir tres cosas que condicionan todas las relaciones posteriores del proyecto: en qué dirección se navega, cuándo se cargan los datos relacionados y qué ocurre al borrar el lado referenciado.

## Decisión

**1. Relaciones unidireccionales.** `Race` tiene campos `championship` y `circuit`; `Championship` y `Circuit` **no** tienen colecciones de carreras. La consulta inversa se hace con el repositorio de carreras (`findByChampionshipId`, `existsByCircuitId`).

**2. Carga perezosa explícita.** Todas las relaciones `@ManyToOne` llevan `fetch = FetchType.LAZY`, porque el valor por defecto de JPA para ese tipo de relación es `EAGER`. Cuando hacen falta los datos relacionados, se piden explícitamente con `JOIN FETCH` en una consulta `@Query` del repositorio.

**3. Borrado restringido.** Las claves foráneas se declaran `ON DELETE RESTRICT`. El servicio del recurso referenciado comprueba antes si está en uso y lanza `ResourceInUseException`, que el manejador global traduce a **409** con el título "Resource in use".

**4. Índices de claves foráneas.** PostgreSQL no los crea automáticamente, así que se crean a mano salvo cuando ya están cubiertos por el índice de una restricción única (un índice compuesto sirve para su primera columna).

## Alternativas consideradas

**Sobre la dirección:**

- **Relaciones bidireccionales** (una `List<Race>` en `Championship`). Permite `championship.getRaces()`, a cambio de: mantener los dos lados sincronizados a mano, complicar `equals`/`hashCode`, cargar colecciones que crecen sin límite y no poder filtrar ni paginar esa colección. Además crea dos caminos distintos para obtener lo mismo.

**Sobre el borrado:**

- **`ON DELETE CASCADE`.** Borrar un campeonato eliminaría sus carreras y, más adelante, los resultados de esas carreras. Cómodo y peligroso: un `DELETE` a una URL podría destruir cientos de filas sin aviso.
- **Borrado lógico** (marcar como archivado en lugar de borrar). Es lo que hacen muchos sistemas con datos históricos y nunca se pierde nada, pero obligaría a filtrar los archivados en todas las consultas del proyecto.

## Consecuencias

**Positivas**

- Es imposible perder carreras o resultados por borrar un campeonato.
- Nada se carga sin pedirlo, y cada consulta declara qué necesita.
- El modelo tiene un único camino para cada consulta.

**Negativas o costes**

- La carga perezosa hace posible el problema **N+1**: mapear una lista de carreras a DTOs con los nombres del campeonato y del circuito dispararía dos consultas por elemento. Se evita con los métodos `findAllWithRelations` y `findByIdWithRelations`, que usan `JOIN FETCH`. Hay que recordar usar el método adecuado según lo que se vaya a leer.
- Para borrar un campeonato hay que borrar antes sus carreras.
- La comprobación de uso obligó a resolver una dependencia circular entre servicios ([0026](0026-dependencias-entre-servicios.md)).