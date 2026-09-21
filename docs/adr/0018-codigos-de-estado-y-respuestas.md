# 0018 — Códigos de estado y forma de las respuestas

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0016](0016-errores-con-problemdetail.md)

## Contexto

Una API REST comunica tanto con el código de estado y las cabeceras como con el cuerpo. Hacía falta un contrato consistente para todas las entidades, incluido el caso discutible de borrar algo que no existe.

## Decisión

| Operación | Respuesta de éxito |
|---|---|
| `GET` colección | 200 con la lista (una lista vacía es `200 []`, no un 404) |
| `GET` elemento | 200 con el recurso |
| `POST` | **201**, cabecera **`Location`** con la URI absoluta del recurso creado, y el recurso en el cuerpo |
| `PUT` | 200 con el recurso ya actualizado |
| `DELETE` | **204** sin cuerpo |

| Situación | Error |
|---|---|
| El recurso no existe (también en `PUT` y `DELETE`) | 404 |
| Nombre duplicado | 409 |
| Datos inválidos o cuerpo ilegible | 400 |

- Los métodos que siempre responden 200 devuelven el objeto directamente. `ResponseEntity` se usa solo cuando hay que controlar el código o las cabeceras (`POST` y `DELETE`).
- La URI de `Location` se construye con el `UriComponentsBuilder` que Spring MVC inyecta como parámetro del método.
- Como `deleteById` no falla si el id no existe, el servicio comprueba antes con `existsById` para poder responder 404.

## Alternativas consideradas

- **`DELETE` de un recurso inexistente con 204.** `DELETE` es idempotente en HTTP: repetirlo deja el sistema en el mismo estado, así que puede considerarse un éxito. Se descartó por coherencia (un `GET` a la misma URL da 404) y porque avisar al cliente de que pedía algo inexistente le ayuda a detectar datos obsoletos o errores propios.
- **`PUT` con 204 sin cuerpo.** Obligaría al cliente a hacer otro `GET` para ver el resultado.
- **`ServletUriComponentsBuilder.fromCurrentRequest()`.** Hace lo mismo leyendo la petición de una variable del hilo; el parámetro explícito deja la dependencia visible en la firma y es más fácil de usar en tests.

## Consecuencias

**Positivas**

- Contrato idéntico en todas las entidades.
- El cliente puede seguir la cabecera `Location` sin conocer el esquema de rutas (los tests de integración lo hacen).

**Negativas o costes**

- El borrado hace una consulta adicional de existencia antes de borrar.
