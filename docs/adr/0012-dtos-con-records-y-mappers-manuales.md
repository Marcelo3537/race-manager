# 0012 — DTOs con records y mappers manuales

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0009](0009-diseno-de-entidades-jpa.md), [0013](0013-open-in-view-desactivado.md)

## Contexto

Exponer las entidades JPA directamente en los controllers acopla la API al esquema de la base de datos y genera varios problemas: se filtran campos que no deberían salir, el cliente puede fijar valores que no le corresponden (como el id) y las relaciones perezosas pueden cargarse o fallar durante la serialización.

## Decisión

- Cada recurso tiene **dos DTOs**: `XRequest` (lo que envía el cliente, sin id) y `XResponse` (lo que devuelve la API, con id).
- Los DTOs son **records** de Java: inmutables, con constructor, accesores, `equals`, `hashCode` y `toString` generados.
- Las anotaciones de Bean Validation van en el `Request`.
- Las entidades nunca cruzan el controller.
- El mapeo entidad → respuesta es **manual**, en un método privado `toResponse` del servicio. Se extraerá a una clase propia cuando haya relaciones o varios formatos de salida.
- Sin Lombok y sin MapStruct por ahora.

## Alternativas consideradas

- **Devolver y recibir entidades.** Expone el esquema, permite fijar el id, filtraría campos sensibles (como el hash de la contraseña de `app_user`) y provoca problemas con las relaciones perezosas.
- **Un único DTO por recurso.** Campos que unas veces significan algo y otras no (el id al crear frente al id al leer).
- **MapStruct.** Menos código repetitivo, pero genera código en compilación cuyos errores son difíciles de entender mientras se aprende. Se puede reconsiderar más adelante.
- **Lombok.** Oculta el código que se ejecuta y no es necesario con records.

## Consecuencias

**Positivas**

- El contrato de la API es independiente del esquema de la base de datos.
- La entrada y la salida pueden evolucionar por separado.

**Negativas o costes**

- Más clases por recurso.
- Al añadir un campo hay que acordarse de mapearlo a mano.
