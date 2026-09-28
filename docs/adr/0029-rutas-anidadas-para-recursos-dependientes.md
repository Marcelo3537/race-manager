# 0029 — Rutas anidadas para recursos dependientes

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0018](0018-codigos-de-estado-y-respuestas.md), [0023](0023-inscripcion-como-entidad.md), [0027](0027-relaciones-unidireccionales-y-borrado-restringido.md)

## Contexto

Hasta la fase 8, todos los recursos de la API eran de primer nivel: `/api/drivers`, `/api/races`. `ChampionshipEntry` es distinto: una inscripción no significa nada fuera de su campeonato, y una petición como "todas las inscripciones del sistema" no tiene ningún uso.

## Decisión

Los recursos que solo existen dentro de otro se exponen con **rutas anidadas**:

GET /api/championships/{championshipId}/entries
POST /api/championships/{championshipId}/entries
GET /api/championships/{championshipId}/entries/{id}
PUT /api/championships/{championshipId}/entries/{id}
DELETE /api/championships/{championshipId}/entries/{id}


**Criterio para elegir entre anidada y plana:** si el recurso no significa nada sin su padre, anidada; si tiene identidad propia, plana. La prueba práctica es preguntarse si `GET /api/<recurso>` a secas tendría algún sentido.

Por eso `Race` sigue siendo plana aunque también pertenezca a un campeonato: un Gran Premio se menciona por sí solo, y listar todas las carreras es una consulta útil.

Consecuencias de diseño que acompañan a la decisión:

1. **El identificador del padre no se repite en el cuerpo.** `ChampionshipEntryRequest` no tiene `championshipId`, lo que elimina la pregunta de qué hacer si el del cuerpo y el de la URL no coinciden.
2. **Validación de coherencia.** Como el id de la inscripción es único por sí solo, hay que comprobar que pertenece al campeonato de la URL. Se resuelve consultando por las dos cosas a la vez (`findByIdAndChampionshipId`): si no coinciden, la consulta no devuelve nada y la respuesta es **404**. Desde otro campeonato, esa inscripción no existe.
3. **404 cuando el padre no existe.** `GET /api/championships/9999/entries` responde 404, no `200 []`. Una colección vacía significa "existe y no tiene elementos"; aquí el contenedor no existe. El servicio lo garantiza consultando el campeonato antes de listar.
4. **La cabecera `Location` incluye las dos partes de la ruta**, construida con `buildAndExpand(championshipId, created.id())`.

## Alternativas consideradas

- **Ruta plana** (`/api/entries` con el `championshipId` en el cuerpo). Coherente con el resto de la API y más simple, pero la URL no expresa la pertenencia y `GET /api/entries` sería una lista global sin utilidad.
- **Híbrido**: anidada para crear y listar, plana para ver, editar y borrar un elemento. Evita la validación de coherencia, a cambio de una API menos uniforme.

## Consecuencias

**Positivas**

- La URL expresa la relación y la API se explica sola.
- No hay duplicación de identificadores entre la ruta y el cuerpo.
- La validación de coherencia impide leer o modificar una inscripción desde un campeonato ajeno.

**Negativas o costes**

- URLs más largas y dos `@PathVariable` del mismo tipo en cada método del controller, con el riesgo de invertirlos al pasarlos al servicio. Los tests lo verifican con `verify(service).delete(8L, 1L)`.
- La API mezcla los dos estilos. Es intencionado y el criterio queda escrito aquí.