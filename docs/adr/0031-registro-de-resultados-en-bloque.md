# 0031 — Registro de resultados en bloque

- **Estado:** Aceptada
- **Fecha:** octubre de 2026
- **Relacionadas:** [0018](0018-codigos-de-estado-y-respuestas.md), [0024](0024-resultados-vinculados-a-la-inscripcion.md), [0033](0033-codigo-422-para-reglas-de-negocio.md)

## Contexto

El resultado de una carrera no son hechos sueltos: cuando una carrera termina, se conocen a la vez las posiciones de todos los participantes. Registrarlos uno a uno dejaría la carrera en un estado incoherente mientras dura el proceso (unos pilotos con resultado y otros sin él), y un fallo a mitad dejaría datos parciales.

Además, hay reglas que **solo se pueden comprobar con el conjunto completo delante**: que no haya dos pilotos en la misma posición, que solo uno tenga la vuelta rápida, y que estén todos los inscritos.

## Decisión

Los resultados se manejan **como un bloque**, no como elementos individuales. El recurso es la colección entera:

| Método | Ruta | Efecto |
|---|---|---|
| `GET` | `/api/races/{raceId}/results` | Lista los resultados |
| `POST` | `/api/races/{raceId}/results` | Registra el bloque completo; 422 si ya existen |
| `PUT` | `/api/races/{raceId}/results` | Reemplaza el bloque completo |
| `DELETE` | `/api/races/{raceId}/results` | Borra todos los resultados de la carrera |

No hay rutas con `{id}` para un resultado individual.

**Reglas que valida el servicio antes de guardar nada:**

1. El bloque contiene exactamente una entrada por cada inscripción del campeonato, ni más ni menos.
2. Ninguna inscripción aparece dos veces.
3. Todas las inscripciones pertenecen al campeonato de la carrera.
4. No hay dos participantes en la misma posición.
5. Como máximo uno tiene la vuelta rápida.
6. Los `FINISHED` tienen posición y los demás no ([0032](0032-posicion-nula-para-quien-no-termina.md)).

Todo ocurre dentro de una transacción, así que **o se guarda el bloque entero o no se guarda nada**.

**Efecto asociado:** registrar resultados pone la carrera en `FINISHED`, y borrarlos la devuelve a `SCHEDULED`. Es un efecto secundario deliberado: si una carrera tiene resultados, se ha disputado. Exigir una segunda petición para cambiar el estado llevaría a carreras marcadas como `SCHEDULED` que ya se han corrido.

**Los puntos no los envía el cliente.** El constructor de `RaceResult` los fija a cero y no hay forma de pasarlos desde fuera; los calculará el backend en la fase 10 ([0025](0025-reglas-de-puntuacion-configurables.md)).

## Alternativas consideradas

- **CRUD individual** (un `POST` por resultado). Coherente con el resto de la API, pero deja la carrera a medias mientras se suben los resultados y hace imposible validar el conjunto.
- **Ambas cosas**: bloque para registrar y endpoints individuales para corregir. Más completo, con el doble de superficie que mantener y testear. Si hiciera falta corregir un solo resultado, se puede añadir después.
- **Que el cliente envíe los puntos.** Permitiría a cualquiera asignarse los puntos que quisiera.
- **Exigir que la carrera esté en `FINISHED` antes de registrar resultados.** Coherente, a cambio de dos peticiones y de que el cliente tenga que recordarlo.

## Consecuencias

**Positivas**

- Atomicidad real: nunca existe una carrera con resultados a medias. El test de integración lo verifica con un bloque incompleto que no guarda nada.
- Las reglas del conjunto se pueden validar porque todos los datos están presentes.
- La validación es lógica pura sobre una lista, así que se prueba entera en tests unitarios, sin base de datos.
- El estado de la carrera refleja siempre la realidad.

**Negativas o costes**

- Corregir un solo resultado obliga a reenviar la lista completa con un `PUT`.
- El reemplazo necesita un `flush` explícito entre el borrado y la inserción, porque si Hibernate reordenara las operaciones saltaría la restricción `uk_result_race_entry`.
- La API deja de ser uniforme: es el único recurso sin operaciones sobre elementos individuales.