# 0017 — Qué información de error llega al cliente

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0016](0016-errores-con-problemdetail.md)

## Contexto

Los mensajes de las excepciones técnicas contienen información interna: una violación de restricción de PostgreSQL incluye el nombre de la tabla, de las columnas y de la restricción, y a veces la consulta con sus valores. Con unas pocas peticiones mal formadas, alguien podría reconstruir el esquema de la base de datos sin haberlo visto nunca.

## Decisión

**Regla general: nunca se envía al cliente el mensaje de una excepción que no hayamos escrito nosotros.**

- Los mensajes de nuestras excepciones de dominio van al `detail`: sabemos exactamente qué dicen.
- Para las excepciones de librerías (Hibernate, el driver de PostgreSQL, Jackson), el cliente recibe un texto genérico escrito por nosotros y el detalle completo va al **log del servidor**.

**Niveles de log:**

| Nivel | Uso |
|---|---|
| `error` | Fallos inesperados del servidor, con la traza completa |
| `warn` | Problemas atribuibles al cliente (por ejemplo, un cuerpo ilegible), sin traza completa |
| `info` | Eventos normales: arranque, migraciones |
| `debug` | Detalle de desarrollo, como el SQL generado (solo en local) |

Registrar con `error` los fallos del cliente llenaría el log de falsas alarmas y escondería los errores reales.

## Alternativas consideradas

- **Devolver el mensaje original de la excepción.** Cómodo para depurar y peligroso: filtra el esquema, versiones y detalles internos.
- **Responder siempre un 500 genérico.** Seguro, pero el cliente no sabría qué corregir.

## Consecuencias

**Positivas**

- El cliente sabe **que** algo falló y, cuando es culpa suya, qué corregir. Solo el servidor sabe **qué** falló por dentro.

**Negativas o costes**

- Para depurar un error hay que mirar los logs del servidor, no la respuesta.
- **Pendiente de revisar:** que el manejador de cuerpo ilegible registre con `warn` y sin traza completa.
