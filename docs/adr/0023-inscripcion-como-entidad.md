# 0023 — La inscripción en un campeonato es una entidad

- **Estado:** Aceptada, pendiente de implementar
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0024](0024-resultados-vinculados-a-la-inscripcion.md)

## Contexto

Un piloto participa en muchos campeonatos y un campeonato tiene muchos pilotos: una relación N:M. Pero esa relación tiene **datos propios**: un piloto cambia de equipo entre temporadas, y su número de coche también puede cambiar. Decir "Alonso está en la F1 2026" no basta; hay que decir con qué equipo y con qué número.

## Decisión

Una entidad `championship_entry` con `championship_id`, `driver_id`, `team_id` y `car_number`, en lugar de relaciones `@ManyToMany`.

- El **número del piloto pertenece a la inscripción**, no a `driver`.
- El **equipo de un piloto se define por campeonato**, no de forma global.

**Criterio general:** si una relación N:M tiene columnas propias, o si otra tabla necesita referenciarla, deja de ser una tabla intermedia y se convierte en una entidad.

## Alternativas consideradas

- **`@ManyToMany` entre campeonato y piloto, y entre campeonato y equipo.** La tabla intermedia que genera JPA solo guarda las dos claves; no puede expresar con qué equipo corre cada piloto en cada campeonato.
- **`team_id` y número en `driver`.** Un piloto tendría un único equipo y número para siempre, y se perdería la historia de las temporadas anteriores.

## Consecuencias

**Positivas**

- Refleja correctamente los cambios de equipo y de número entre temporadas.
- Es la base sobre la que se calcularán los resultados y las clasificaciones de equipos ([0024](0024-resultados-vinculados-a-la-inscripcion.md)).

**Negativas o costes**

- Una entidad más, con restricciones únicas compuestas que se definirán al implementarla.
