# 0019 — Estrategia de tests en tres niveles

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0002](0002-slices-verticales-y-despliegue-temprano.md), [0005](0005-java-21-y-spring-boot-4.md), [0006](0006-postgresql-local-con-docker.md), [0007](0007-flyway-y-validacion-de-hibernate.md)

## Contexto

Cada capa puede fallar de maneras que las demás no ven: el orden de las operaciones en el servicio, el contrato HTTP del controller, el SQL real contra PostgreSQL. Un único tipo de test no las cubre todas.

## Decisión

Cada funcionalidad tiene tres ficheros de test:

**1. Unitario del servicio** (JUnit 5, Mockito, AssertJ). Sin Spring y sin base de datos; se ejecuta en milisegundos.

- Un test por cada decisión del código: cada `if`, cada excepción.
- En los métodos que modifican estado se comprueba **el efecto**, no solo el valor devuelto: que la entidad cambió, y que no se llamó a `save()` o `deleteById()` cuando no correspondía (`verify(..., never())`).
- Los datos preparados difieren de los del request, para que las aserciones sobre la entidad no puedan pasar por casualidad.
- En los errores se comprueba también el mensaje (por ejemplo, que contiene el id pedido).

**2. De controller** (`@WebMvcTest(XController.class)`, `@MockitoBean`, MockMvc). Contexto de Spring recortado a la capa web, con el servicio simulado.

- Rutas, verbos, códigos de estado, cabecera `Location`, `application/problem+json` y el `title` exacto de cada error.
- Que la validación corta la petición antes de llegar al servicio.
- Los cuerpos JSON de las peticiones se escriben literalmente, como los enviaría un cliente, sin serializarlos desde los DTOs.

**3. De integración** (`@SpringBootTest`, Testcontainers con `postgres:16-alpine`). La aplicación completa contra un PostgreSQL real y efímero.

- Contenedor `static` con `@ServiceConnection`: se arranca una vez por clase y Spring recibe su URL automáticamente.
- `@Transactional` en la clase: cada test se revierte al terminar.
- Comprueba lo que solo existe con una base de datos real: las migraciones desde cero, el SQL de los métodos derivados (por ejemplo, que `IgnoreCase` funciona de verdad) y las restricciones.
- Se reutiliza la cabecera `Location` devuelta por la API y **nunca se inventan identificadores**.
- El estado real se verifica con el repositorio (`count()`).

Una forma de auditar los tests: romper el código a propósito y comprobar que se pone en rojo el test que corresponde.

## Alternativas consideradas

- **H2 en memoria para los tests.** Más rápido y sin Docker, pero es otro motor de base de datos, con otro dialecto y otros tipos. El resultado típico son tests verdes y producción rota.
- **Solo tests de integración.** Lentos y, cuando fallan, no señalan qué capa es la culpable.
- **Solo tests unitarios.** No ven ni el HTTP ni el SQL.

## Consecuencias

**Positivas**

- Cada nivel detecta errores que los otros no pueden ver.
- Los unitarios y los de controller se ejecutan sin Docker.

**Negativas o costes**

- Los de integración necesitan Docker encendido y tardan decenas de segundos.
- **Pendiente:** el test generado por Spring Initializr (`RaceManagerApplicationTests`) arranca sin Testcontainers y se conecta a la base de datos local de Docker Compose. Fallará en un entorno de integración continua y habrá que adaptarlo cuando se monte.
