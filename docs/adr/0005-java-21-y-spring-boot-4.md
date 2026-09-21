# 0005 — Java 21 y Spring Boot 4

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0019](0019-estrategia-de-tests.md)

## Contexto

Spring Initializr ofrecía por defecto Spring Boot 4.1.1. La mayor parte de la documentación, los tutoriales y las respuestas en foros se refieren todavía a Spring Boot 3, así que usar la versión nueva implica encontrar diferencias por el camino.

## Decisión

- **Java 21** (LTS, distribución Temurin).
- **Spring Boot 4.1.1**, que trae Spring Framework 7, Hibernate 7, Jackson 3 y Flyway 12.
- El JDK con el que el IDE ejecuta la aplicación debe ser el mismo con el que se compila y el de la imagen Docker: 21.

## Alternativas consideradas

- **Spring Boot 3.5.** Mucho más material de consulta. Se descartó para trabajar con la versión actual; si apareciera un bloqueo serio, el cambio es una línea en el `pom.xml`.
- **Java 17.** Válido para Spring Boot, pero más antiguo sin ninguna ventaja para este proyecto.
- **Java 23.** No es LTS. Al principio la aplicación se ejecutaba por error con un JDK 23 que el IDE tenía instalado; se corrigió para que ejecución y compilación coincidieran.

## Consecuencias

**Positivas**

- El proyecto usa la versión vigente del ecosistema.

**Negativas o costes**

Diferencias ya encontradas respecto a Spring Boot 3:

- `@MockBean` ya no existe; se usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- Testcontainers 2 renombró sus artefactos con el prefijo `testcontainers-` (`testcontainers-postgresql`, `testcontainers-junit-jupiter`).
- `@WebMvcTest` está en `org.springframework.boot.webmvc.test.autoconfigure`.
- Jackson 3 cambia el paquete principal a `tools.jackson`.
- Los starters son más granulares (por ejemplo, `spring-boot-starter-webmvc`).

Al buscar documentación hay que comprobar siempre a qué versión se refiere.
