# 0011 — Tipos para fechas y magnitudes decimales

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0014](0014-validacion-en-capas.md), [0019](0019-estrategia-de-tests.md)

## Contexto

`Driver` tiene una fecha de nacimiento y `Circuit` una longitud en kilómetros con decimales. Los tipos elegidos determinan si los valores se guardan y se comparan exactamente como los envió el cliente.

## Decisión

**Fechas sin hora**

- `LocalDate` en Java y `DATE` en PostgreSQL.
- En JSON viajan en formato ISO 8601 (`"1981-07-29"`). Cualquier otro formato no se puede deserializar y devuelve un 400.
- La fecha de nacimiento se valida con `@NotNull` y `@Past`.

**Magnitudes decimales**

- `BigDecimal` en Java y `NUMERIC(p, s)` en PostgreSQL. La longitud de un circuito es `NUMERIC(5, 3)`: hasta 99,999 km con tres decimales.
- Validación con `@NotNull`, `@Positive` y `@Digits(integer = 2, fraction = 3)`, más un `CHECK (length_km > 0)` en la tabla.
- Los `BigDecimal` se construyen siempre a partir de un `String`, nunca de un `double`, y se comparan con `compareTo` (en los tests, `isEqualByComparingTo`).

`@Digits` es necesario porque PostgreSQL **redondea sin avisar** los decimales que sobran (`5.4321` se guardaría como `5.432`) y lanza un error de desbordamiento si la parte entera no cabe. Con la anotación, ambos casos se rechazan con un 400 claro.

## Alternativas consideradas

- **`LocalDateTime` o `TIMESTAMP` para la fecha de nacimiento.** Obliga a inventar una hora y abre la puerta a desfases de zona horaria.
- **`@PastOrPresent`.** Aceptaría el día de hoy, que no tiene sentido como fecha de nacimiento de un piloto.
- **`double` para la longitud.** Trabaja en base 2, no representa exactamente muchos decimales y convierte en cada viaje hacia y desde `NUMERIC`.

## Consecuencias

**Positivas**

- Lo que se guarda es exactamente lo que se envió, y las comparaciones son exactas.
- Los valores que no encajan en la columna se rechazan antes de llegar a la base de datos.

**Negativas o costes**

- `BigDecimal` es más verboso que un `double`.
- Su `equals` tiene en cuenta la escala: `5.8` y `5.800` no son iguales con `equals`. Hay que usar `compareTo`.
- En los tests con `jsonPath` conviene usar valores sin ceros finales para no depender de cómo se serializa la escala.
