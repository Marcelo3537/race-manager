# 0015 — Unicidad de nombres

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0014](0014-validacion-en-capas.md), [0016](0016-errores-con-problemdetail.md)

## Contexto

Algunos recursos no pueden repetirse: no tiene sentido tener dos equipos "Ferrari". Otros sí: dos pilotos pueden llamarse igual. Además, la comprobación tiene que dar un mensaje útil al cliente y, a la vez, ser fiable ante peticiones simultáneas.

## Decisión

**Qué es único:**

| Entidad | Unicidad |
|---|---|
| `Championship` | La pareja nombre + temporada (`uk_championship_name_season`) |
| `Team` | El nombre (`uk_team_name`) |
| `Circuit` | El nombre (`uk_circuit_name`) |
| `Driver` | Nada: dos pilotos pueden tener el mismo nombre |

**Cómo se garantiza, con dos mecanismos complementarios:**

1. **Comprobación en el servicio** con un método derivado `existsBy...`, que produce un 409 con un mensaje legible.
2. **Restricción `UNIQUE` en la tabla**, que protege frente a peticiones simultáneas (las dos podrían pasar la comprobación en Java antes de que ninguna guarde) y frente a escrituras que no pasan por la API.

**Sin distinguir mayúsculas:** las comprobaciones usan `IgnoreCase`. Dos valores que solo difieren en mayúsculas ("Mercedes" y "MERCEDES") representan la misma entidad del mundo real; la diferencia es ruido de escritura, no información.

**Al actualizar** se usa la variante `...AndIdNot`, que excluye el propio registro. Sin ella, editar un campeonato sin cambiarle el nombre daría un 409 porque "ya existe", cuando el que existe es él mismo.

## Alternativas consideradas

- **Solo la restricción de la base de datos.** El error llega como una excepción técnica que solo puede traducirse a un 409 genérico.
- **Solo la comprobación en Java.** Dos peticiones simultáneas podrían insertar el mismo nombre.
- **Distinguir mayúsculas.** Permitiría "Ferrari" y "FERRARI" como equipos distintos.

## Consecuencias

**Positivas**

- Mensajes claros para el cliente e integridad garantizada por la base de datos.

**Negativas o costes**

- **Grieta conocida:** la restricción `UNIQUE` de PostgreSQL sí distingue mayúsculas. Un `INSERT` hecho directamente en la base de datos podría crear "Ferrari" y "FERRARI". Se cerraría con un índice único sobre `LOWER(name)`; se pospone porque solo puede ocurrir saltándose la API.
- Cada entidad con unicidad necesita dos métodos en el repositorio.
