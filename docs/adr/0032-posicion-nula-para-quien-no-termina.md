# 0032 — Posición nula para quien no termina

- **Estado:** Aceptada
- **Fecha:** octubre de 2026
- **Relacionadas:** [0010](0010-enums-persistidos-como-texto.md), [0031](0031-registro-de-resultados-en-bloque.md)

## Contexto

Un resultado tiene cuatro estados posibles: `FINISHED`, `DNF` (abandonó), `DNS` (no tomó la salida) y `DSQ` (descalificado). Había que decidir qué posición asignar a quien no termina la carrera.

## Decisión

**Solo los `FINISHED` tienen posición.** Para el resto, `position` es nula y los puntos son cero.

Como contrapartida, **el bloque de resultados debe incluir a todos los inscritos del campeonato**: quien no participó aparece con `DNS`. Así es imposible olvidarse de un piloto, porque un bloque incompleto se rechaza; sin esa regla, un piloto omitido por error sería indistinguible de uno omitido a propósito.

La coherencia entre estado y posición se garantiza en dos capas:

- En el servicio, al validar el bloque, con un 422 que indica qué inscripción falla.
- En la base de datos, con una restricción que relaciona las dos columnas:

```sql
CONSTRAINT ck_result_position_matches_status CHECK (
    (status = 'FINISHED' AND position IS NOT NULL)
    OR (status <> 'FINISHED' AND position IS NULL)
)
```

La restricción `UNIQUE (race_id, position)` sigue impidiendo dos pilotos en la misma posición y, a la vez, **permite varios abandonos en la misma carrera**, porque en SQL dos nulos no se consideran iguales.

## Alternativas consideradas

- **Posición obligatoria para todos**, clasificando a los que no terminan según las vueltas completadas, como hace la Fórmula 1 en su resultado oficial. Simplifica el modelo (ninguna columna anulable) y traslada al cliente el cálculo de esas posiciones, además de hacer que un abandono ocupe una posición que podría contarse como podio.
- **No incluir en el bloque a quien no participó.** Más flexible y permite perder silenciosamente a un piloto por error.

## Consecuencias

**Positivas**

- Un `DNS` no ocupa ninguna posición, así que las clasificaciones y las estadísticas de la fase 11 no tendrán que filtrar casos raros.
- Es imposible registrar un resultado internamente contradictorio.
- El resultado de una carrera es siempre la foto completa de la parrilla.

**Negativas o costes**

- La columna `position` es anulable, y cualquier consulta que la ordene debe decidir dónde van los nulos. El repositorio usa `ORDER BY r.position NULLS LAST`.
- Registrar una carrera exige conocer a todos los inscritos, también a los ausentes.