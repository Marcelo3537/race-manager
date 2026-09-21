# Registro de decisiones de arquitectura (ADR)

Cada fichero de esta carpeta documenta **una** decisión de diseño de Race Manager: el contexto en que se tomó, lo que se decidió, las alternativas que se descartaron y lo que implica. El objetivo es que cualquiera (incluido el autor dentro de unos meses) pueda saber no solo cómo está hecho el proyecto, sino **por qué**.

## Índice

| Nº | Decisión | Estado |
|---|---|---|
| [0001](0001-registrar-decisiones-con-adr.md) | Registrar las decisiones de arquitectura con ADR | Aceptada |
| [0002](0002-slices-verticales-y-despliegue-temprano.md) | Construcción por slices verticales y despliegue temprano | Aceptada |
| [0003](0003-alcance-del-mvp.md) | Alcance del MVP | Aceptada |
| [0004](0004-paquetes-por-funcionalidad.md) | Organización en paquetes por funcionalidad | Aceptada |
| [0005](0005-java-21-y-spring-boot-4.md) | Java 21 y Spring Boot 4 | Aceptada |
| [0006](0006-postgresql-local-con-docker.md) | PostgreSQL local en Docker | Aceptada |
| [0007](0007-flyway-y-validacion-de-hibernate.md) | Flyway para el esquema e Hibernate en modo `validate` | Aceptada |
| [0008](0008-convenciones-de-nombres-en-base-de-datos.md) | Convenciones de nombres en la base de datos | Aceptada |
| [0009](0009-diseno-de-entidades-jpa.md) | Diseño de las entidades JPA | Aceptada |
| [0010](0010-enums-persistidos-como-texto.md) | Enums persistidos como texto | Aceptada |
| [0011](0011-tipos-para-fechas-y-decimales.md) | Tipos para fechas y magnitudes decimales | Aceptada |
| [0012](0012-dtos-con-records-y-mappers-manuales.md) | DTOs con records y mappers manuales | Aceptada |
| [0013](0013-open-in-view-desactivado.md) | `open-in-view` desactivado | Aceptada |
| [0014](0014-validacion-en-capas.md) | Validación en capas | Aceptada |
| [0015](0015-unicidad-de-nombres.md) | Unicidad de nombres | Aceptada |
| [0016](0016-errores-con-problemdetail.md) | Manejo global de errores con `ProblemDetail` | Aceptada |
| [0017](0017-informacion-de-error-expuesta.md) | Qué información de error llega al cliente | Aceptada |
| [0018](0018-codigos-de-estado-y-respuestas.md) | Códigos de estado y forma de las respuestas | Aceptada |
| [0019](0019-estrategia-de-tests.md) | Estrategia de tests en tres niveles | Aceptada |
| [0020](0020-configuracion-por-entorno.md) | Configuración por variables de entorno y perfiles | Aceptada |
| [0021](0021-despliegue-render-y-neon.md) | Despliegue en Render con la base de datos en Neon | Aceptada |
| [0022](0022-flujo-de-trabajo-con-git.md) | Flujo de trabajo con Git | Aceptada |
| [0023](0023-inscripcion-como-entidad.md) | La inscripción en un campeonato es una entidad | Aceptada, pendiente de implementar |
| [0024](0024-resultados-vinculados-a-la-inscripcion.md) | Los resultados apuntan a la inscripción | Aceptada, pendiente de implementar |
| [0025](0025-reglas-de-puntuacion-configurables.md) | Reglas de puntuación configurables por campeonato | Aceptada, pendiente de implementar |

## Qué merece un ADR

Una decisión con **alternativas razonables**, que **alguien podría cuestionar** o que sería **caro revertir**. Las convenciones estándar que nadie discutiría no llevan ADR propio.

## Convenciones del proyecto sin ADR propio

Se aplican en todo el código, pero son práctica habitual y no tienen alternativas que merezca la pena documentar:

- Inyección de dependencias por constructor, con campos `final` y sin `@Autowired`.
- Servicios anotados con `@Transactional(readOnly = true)` a nivel de clase y `@Transactional` en los métodos de escritura.
- Controllers y servicios sin estado mutable: los beans de Spring son instancias únicas compartidas entre peticiones.
- Imports explícitos, sin asteriscos.
- Tests con un `@DisplayName` que describe el comportamiento en una frase.

## Cómo añadir un ADR

1. Copia [`plantilla.md`](plantilla.md) con el siguiente número libre y un título corto en minúsculas separado por guiones.
2. Rellénalo y añádelo al índice de este fichero.
3. **Un ADR aceptado no se reescribe.** Si la decisión cambia, se crea uno nuevo que la sustituye y el antiguo pasa a estado *Reemplazada por NNNN*. Se permiten correcciones menores (erratas, enlaces) y actualizar el estado cuando algo pendiente se implementa.

Es el mismo principio que las migraciones de Flyway: el historial no se edita, se amplía.
