# 0033 — El código 422 para violaciones de reglas de negocio

- **Estado:** Aceptada
- **Fecha:** octubre de 2026
- **Relacionadas:** [0014](0014-validacion-en-capas.md), [0016](0016-errores-con-problemdetail.md), [0018](0018-codigos-de-estado-y-respuestas.md), [0031](0031-registro-de-resultados-en-bloque.md)

## Contexto

Hasta la fase 9, los errores atribuibles al cliente eran de dos tipos: **400** cuando la petición estaba mal formada o le faltaban campos, y **409** cuando chocaba con algo que ya existía (un nombre duplicado, un recurso en uso).

El registro de resultados introduce un tercer tipo: una petición **sintácticamente impecable**, con todos los campos presentes y válidos uno a uno, que el servidor no puede procesar porque el conjunto viola una regla del dominio. Por ejemplo, dos pilotos en la posición 1.

## Decisión

Se añade `BusinessRuleException`, que el manejador global traduce a **422 Unprocessable Entity** con el título "Business rule violated".

Criterio de uso:

| Código | Cuándo | Quién lo detecta |
|---|---|---|
| 400 | La petición está mal formada o le faltan campos | Bean Validation, en la frontera |
| 422 | Está bien formada, pero viola una regla del dominio | El servicio |
| 409 | Choca con el estado actual: duplicado o recurso en uso | El servicio |

La frontera entre 422 y 409 es discutible. El criterio adoptado: **409 cuando el problema es un choque con algo que ya existe; 422 cuando el problema está dentro de la propia petición.**

## Alternativas consideradas

- **Reutilizar el 409 para todo.** Más simple, y mezcla dos cosas distintas: "ya existe un campeonato con ese nombre" y "tu lista de resultados es incoherente" no se parecen.
- **Usar el 400.** Es lo que hace mucha gente, y pierde la distinción entre un error de formato (que el cliente corrige mirando el JSON) y uno de reglas (que exige entender el dominio).

## Consecuencias

**Positivas**

- El cliente distingue de un vistazo si el problema es cómo construyó la petición o qué pidió.
- Los dos casos están documentados con tests: un POST sin `entryId` da 400 con el campo señalado; un POST con posiciones repetidas da 422 con la regla violada.

**Negativas o costes**

- Un código de estado más que documentar y que el cliente debe conocer; el 422 es menos habitual que el 400 y el 409.
- Exige criterio al clasificar cada error nuevo. El criterio queda escrito aquí para que las decisiones futuras sean consistentes.