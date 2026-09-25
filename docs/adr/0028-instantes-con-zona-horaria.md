# 0028 — Instantes con zona horaria para fechas de eventos

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0008](0008-convenciones-de-nombres-en-base-de-datos.md), [0011](0011-tipos-para-fechas-y-decimales.md)

## Contexto

`Driver` usa `LocalDate` para la fecha de nacimiento ([0011](0011-tipos-para-fechas-y-decimales.md)). Una carrera es distinta: empieza a una hora concreta y la siguen personas en husos horarios distintos. Guardar "15:00" sin más no dice de dónde son esas tres de la tarde.

## Decisión

Los campos que representan **un momento concreto** usan `OffsetDateTime` en Java y `TIMESTAMPTZ` en PostgreSQL. Los que representan **una fecha de calendario** siguen usando `LocalDate` y `DATE`.

**Criterio para decidir:** ¿tiene sentido convertir este valor a otra zona horaria? "Las 15:00 en Monza son las 22:00 en Tokio" tiene sentido: es un instante. "Nació el 29 de julio, o sea el 30 en Japón" no lo tiene: es una fecha de calendario.

PostgreSQL convierte lo que recibe a UTC y almacena ese instante, sin conservar el desplazamiento original. La base de datos guarda **un momento**, y cada cliente lo muestra en su hora local.

El campo se llama `scheduledAt` y no `date`, siguiendo [0008](0008-convenciones-de-nombres-en-base-de-datos.md): el nombre indica que contiene un momento, no un día.

## Alternativas consideradas

- **`LocalDateTime` y `TIMESTAMP`.** Guardan fecha y hora sin zona. Ambiguo por definición y fuente habitual de errores al desplegar en servidores con otra configuración regional.
- **`LocalDate`.** Perdería la hora de inicio de la carrera.
- **`ZonedDateTime`.** Guarda la zona con sus reglas (`Europe/Rome`), lo que permite saber qué ocurre si cambia el horario de verano. Es más de lo que este proyecto necesita.
- **Guardar la hora local más un campo con la zona.** Reinventar a mano lo que `TIMESTAMPTZ` ya hace.

## Consecuencias

**Positivas**

- No hay ambigüedad: el valor almacenado es un instante único.
- Jackson serializa en ISO 8601 (`"2026-09-06T13:00:00Z"`), un formato estándar que cualquier cliente sabe interpretar.
- Un formato de fecha incorrecto en la petición se rechaza con un 400 a través del manejador de cuerpos ilegibles.

**Negativas o costes**

- Lo que se envía y lo que se lee no coinciden literalmente: enviar `15:00+02:00` devuelve `13:00Z`. Es correcto y desconcierta la primera vez; el test de integración lo documenta con una aserción explícita.
- Mostrar la hora local de cada carrera en su circuito exigiría guardar además la zona del circuito. No está en el alcance actual.