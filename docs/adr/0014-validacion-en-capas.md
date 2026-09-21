# 0014 — Validación en capas

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0010](0010-enums-persistidos-como-texto.md), [0011](0011-tipos-para-fechas-y-decimales.md), [0015](0015-unicidad-de-nombres.md), [0016](0016-errores-con-problemdetail.md)

## Contexto

Los datos que llegan por la API pueden ser incorrectos de dos maneras distintas: mal formados (un nombre vacío, una temporada fuera de rango) o incompatibles con el estado actual del sistema (un nombre que ya existe). Si solo la base de datos los rechaza, el cliente recibe errores genéricos y la petición recorre todas las capas antes de fallar.

## Decisión

**Criterio para decidir dónde va cada regla:**

> Si la regla se puede comprobar mirando **solo el objeto que llega**, es Bean Validation en el DTO. Si necesita **consultar la base de datos**, es lógica de negocio y va en el servicio.

**Bean Validation en la frontera.** Las anotaciones van en el `Request` y se activan con `@Valid` en el controller, así que los datos mal formados se rechazan **antes** de entrar al método: el servicio nunca los ve.

- `@NotBlank` para cadenas obligatorias (`@NotNull` aceptaría `""` o `"   "`).
- `@NotNull` para el resto de tipos obligatorios.
- Mensajes escritos por nosotros en el atributo `message`, para que la respuesta sea estable.

**Defensa en profundidad.** Los límites se declaran a propósito en tres sitios, porque cada uno actúa en un momento distinto:

| Capa | Cuándo actúa | Qué aporta que las otras no |
|---|---|---|
| DTO (`@Size`, `@Positive`...) | En la frontera, antes del controller | Rechazo inmediato con un 400 que indica el campo |
| Entidad (`@Column`) | Al arrancar la aplicación | Detecta que código y esquema se han desincronizado |
| Tabla (`NOT NULL`, `VARCHAR(n)`, `CHECK`) | En cada escritura, venga de donde venga | Nadie puede saltársela, ni otra aplicación ni un `INSERT` manual |

## Alternativas consideradas

- **Validar solo en la base de datos.** El cliente recibe un 409 o un 500 opaco y la petición hace trabajo inútil antes de fallar.
- **`if` manuales en el servicio.** Repetitivos, fáciles de olvidar en un endpoint y mezclan formato con negocio.

## Consecuencias

**Positivas**

- Errores precisos: el cliente sabe qué campo corregir.
- El servicio puede asumir que lo que recibe está bien formado.

**Negativas o costes**

- Los límites están en tres sitios y hay que mantenerlos coherentes.
- **Limitación conocida:** si un mismo campo viola dos restricciones a la vez, el manejador de errores conserva solo un mensaje (`putIfAbsent`), y Hibernate Validator no garantiza en qué orden evalúa las restricciones. El mensaje devuelto puede variar. Los tests están diseñados para violar una sola restricción por campo.
