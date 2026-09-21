# 0006 — PostgreSQL local en Docker

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0019](0019-estrategia-de-tests.md), [0020](0020-configuracion-por-entorno.md)

## Contexto

La aplicación se despliega sobre PostgreSQL, así que en desarrollo también debe usar PostgreSQL. En el equipo de desarrollo el puerto 5432 ya estaba ocupado por otra instalación de PostgreSQL, lo que provocó que la aplicación se conectara al servidor equivocado y fallara con un error de autenticación.

## Decisión

PostgreSQL de desarrollo en un contenedor definido en `docker-compose.yml`:

- Imagen `postgres:16-alpine`, con la versión mayor fijada.
- Volumen nombrado para que los datos sobrevivan al contenedor.
- Publicado en el puerto **5433** del host (5432 dentro del contenedor).
- Credenciales de desarrollo sin valor real, sobrescribibles por variables de entorno.

## Alternativas consideradas

- **PostgreSQL instalado en el sistema.** Difícil de igualar en versión con producción y de limpiar si se rompe.
- **H2 en memoria.** Es otro motor de base de datos (ver [0019](0019-estrategia-de-tests.md)).
- **`postgres:latest`.** Una actualización de versión mayor podría llegar sin darse cuenta.
- **Liberar el 5432 desinstalando la otra instalación.** Invasivo e innecesario.

## Consecuencias

**Positivas**

- Entorno reproducible y desechable: si se estropea, se borra y se levanta en segundos.
- Misma versión mayor que los tests de integración.

**Negativas o costes**

- Puerto no estándar, documentado en el README.
- La imagen solo lee `POSTGRES_USER`, `POSTGRES_PASSWORD` y `POSTGRES_DB` la primera vez que inicializa el volumen. Cambiarlas después no tiene efecto salvo que se recree el volumen con `docker compose down -v`.
