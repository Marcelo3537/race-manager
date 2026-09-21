# 0001 — Registrar las decisiones de arquitectura con ADR

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026

## Contexto

Las decisiones del proyecto se estaban razonando fuera del repositorio. El código mostraba qué se había hecho, pero no por qué, ni qué alternativas se habían descartado. Eso tiene dos riesgos: dentro de unos meses nadie recordará el motivo de decisiones deliberadas, y cualquier cambio "para simplificar" puede deshacerlas sin saberlo.

Además, Race Manager es un proyecto de portfolio. El razonamiento detrás del diseño es tan parte del entregable como el propio código.

## Decisión

Cada decisión relevante se registra como un ADR (*Architecture Decision Record*) en `docs/adr/`:

- Un fichero por decisión, numerado de forma correlativa.
- Estructura común: estado, contexto, decisión, alternativas consideradas y consecuencias (ver [`plantilla.md`](plantilla.md)).
- En español, como el resto de la documentación.
- Un ADR aceptado no se reescribe. Si la decisión cambia, se escribe uno nuevo que lo reemplaza y el antiguo se marca como *Reemplazada por*.

Se documentan las decisiones con alternativas razonables, discutibles o caras de revertir. Las convenciones estándar se listan en el índice sin ADR propio.

## Alternativas consideradas

- **Un único fichero de decisiones.** Más simple al principio, pero crece hasta ser un documento que no se lee entero y no permite enlazar una decisión concreta desde un commit o una revisión.
- **Comentarios en el código.** Útiles para detalles locales, pero se dispersan y no cubren decisiones transversales como el despliegue o la estrategia de tests.
- **No documentar.** Las razones quedarían solo en la memoria del autor.

## Consecuencias

**Positivas**

- El historial explica cómo evolucionó el proyecto, no solo cómo está hoy.
- Sirve como guion para explicar y defender el diseño en una entrevista o una presentación.
- Cada decisión se puede enlazar individualmente.

**Negativas o costes**

- Exige la disciplina de escribir el ADR al tomar la decisión, no semanas después.
- Si no se mantiene el estado de los pendientes, el registro deja de reflejar la realidad.
