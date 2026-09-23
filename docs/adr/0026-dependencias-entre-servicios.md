# 0026 — Dependencias entre servicios en un solo sentido

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0004](0004-paquetes-por-funcionalidad.md), [0018](0018-codigos-de-estado-y-respuestas.md)

## Contexto

Con la llegada de `Race` aparecieron dos necesidades cruzadas entre paquetes:

- `RaceService` necesita `ChampionshipService` y `CircuitService` para obtener las entidades relacionadas al crear o actualizar una carrera.
- `ChampionshipService` y `CircuitService` necesitan saber si existen carreras asociadas antes de borrar, para devolver un 409 en lugar de dejar que falle la clave foránea.

Como los repositorios son *package-private* ([0004](0004-paquetes-por-funcionalidad.md)), un servicio no puede consultar el repositorio de otro paquete.

La primera solución fue que `ChampionshipService` y `CircuitService` dependieran directamente de `RaceService`. Eso creó una **dependencia circular**: `ChampionshipService → RaceService → ChampionshipService`. Spring no pudo construir ninguno de los dos beans y **el contexto de la aplicación dejó de arrancar**, lo que hizo fallar los veinte tests de integración del proyecto.

## Decisión

Los servicios pueden depender de otros servicios, pero **la dependencia tiene que ir en un solo sentido**. Cuando aparece un ciclo, se extrae la parte que consultan los demás a un bean propio con dependencias mínimas.

En este caso se creó `RaceUsageService` en el paquete `race`: una clase pública cuya única dependencia es `RaceRepository`, con los métodos `existsForChampionship` y `existsForCircuit`. `ChampionshipService` y `CircuitService` dependen de ella y no de `RaceService`.

El grafo de dependencias queda dirigido:
RaceService → ChampionshipService, CircuitService
ChampionshipService → RaceUsageService → RaceRepository
CircuitService → RaceUsageService → RaceRepository


Además, para que otro servicio pueda obtener una entidad relacionada, cada servicio expone un método `getEntityById` que devuelve la **entidad** y lanza `ResourceNotFoundException` si no existe. Es una excepción deliberada a la regla de que los servicios trabajan con DTOs: quien construye la relación necesita el objeto real, no su representación.

## Alternativas consideradas

- **`@Lazy` en la inyección.** Rompe el ciclo con una anotación y una línea de código. Se descartó porque no elimina el problema de diseño: siguen existiendo dos clases que se necesitan mutuamente, con riesgo de comportamientos extraños en la inicialización.
- **Hacer público `RaceRepository`.** Simple, pero anula la garantía que da el compilador de que solo los servicios usan repositorios.
- **Consultar carreras desde `ChampionshipRepository` con JPQL.** Sin beans nuevos ni ciclo, pero el repositorio de campeonatos pasaría a conocer la entidad `Race`.
- **No comprobar nada en Java** y traducir el fallo de la clave foránea a un 409 genérico en el manejador global. Menos código, a cambio de un mensaje que no explica el motivo real.

## Consecuencias

**Positivas**

- Se mantiene el aislamiento entre paquetes: cada repositorio sigue siendo accesible solo desde su propio paquete.
- El ciclo desaparece de verdad, no se tapa.
- Un ciclo suele indicar que una clase tiene dos responsabilidades. `RaceService` gestionaba carreras **y** servía de fuente de consulta para otros; separarlas dejó las dos cosas más claras.

**Negativas o costes**

- Una clase más por cada servicio que otros necesiten consultar.
- Hay que vigilar la dirección de las dependencias al añadir servicios nuevos. La señal de alarma es inconfundible: el contexto de Spring no arranca y **todos** los tests de integración fallan a la vez.