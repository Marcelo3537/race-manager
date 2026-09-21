# 0016 — Manejo global de errores con `ProblemDetail`

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0014](0014-validacion-en-capas.md), [0017](0017-informacion-de-error-expuesta.md), [0018](0018-codigos-de-estado-y-respuestas.md)

## Contexto

Sin un manejo centralizado, cualquier excepción que sale de un controller se convierte en un 500, aunque el problema sea del cliente (un id inexistente, un duplicado, datos incompletos). Hacía falta traducir cada tipo de error a su código HTTP con un formato de respuesta consistente.

## Decisión

Un único `GlobalExceptionHandler` (`@RestControllerAdvice`, en `common/exception`) traduce las excepciones a respuestas **`ProblemDetail`**, el formato estándar de errores para APIs HTTP (RFC 9457), con `Content-Type: application/problem+json`.

- `title`: descripción genérica del tipo de problema, igual para todos los errores de ese tipo.
- `detail`: descripción del caso concreto.
- Propiedad extendida `errors` para los fallos de validación, con un mensaje por campo.

**Excepciones de dominio genéricas y reutilizables**, en `common/exception`:

- `ResourceNotFoundException(recurso, id)`, que compone el mensaje "Championship with id 999 not found".
- `DuplicateResourceException(mensaje)`.

Ambas extienden de `RuntimeException`, porque Spring solo revierte automáticamente una transacción ante excepciones no comprobadas.

| Excepción | Código | `title` |
|---|---|---|
| `ResourceNotFoundException` | 404 | Resource not found |
| `DuplicateResourceException` | 409 | Resource already exists |
| `MethodArgumentNotValidException` | 400 | Validation failed |
| `HttpMessageNotReadableException` | 400 | Invalid request body |
| `DataIntegrityViolationException` | 409 | Data integrity violation |
| `Exception` (cualquier otra) | 500 | Internal Server Error |

## Alternativas consideradas

- **Un record `ErrorResponse` propio.** Era el formato del planteamiento inicial y habría funcionado igual, pero `ProblemDetail` es un estándar con nombre, no requiere mantener una clase más y se documenta solo en OpenAPI.
- **`try/catch` en cada controller.** Duplicación y respuestas inconsistentes.
- **Excepciones comprobadas.** Harían que `@Transactional` confirmara los cambios en lugar de revertirlos.

## Consecuencias

**Positivas**

- Un solo manejador sirve a todas las entidades: `Team`, `Driver` y `Circuit` se añadieron sin tocarlo.
- Formato de error estándar y documentable.

**Negativas o costes**

- Solo captura lo que ocurre dentro del manejo de la petición por Spring MVC. Los errores que se produzcan en filtros (como el de autenticación JWT) no pasarán por aquí y necesitarán su propio tratamiento.
