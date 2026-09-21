# 0020 — Configuración por variables de entorno y perfiles

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0006](0006-postgresql-local-con-docker.md), [0021](0021-despliegue-render-y-neon.md)

## Contexto

La misma aplicación se ejecuta en local (contra PostgreSQL en Docker) y en producción (contra Neon). Las credenciales de producción no pueden estar en el repositorio.

## Decisión

- La configuración va en **`application.yml`**, con valores de desarrollo y marcadores con valor por defecto: `${DB_USERNAME:racemanager}`, `${DB_PASSWORD:racemanager}`, `server.port: ${PORT:8080}`.
- En producción, la conexión se configura con **variables de entorno** que Spring asocia automáticamente a sus propiedades (`spring.datasource.url` ↔ `SPRING_DATASOURCE_URL`). Las variables de entorno tienen prioridad sobre el `application.yml`, así que no hace falta tocar el fichero.
- Un **perfil `prod`** (`application-prod.yml`, activado con `SPRING_PROFILES_ACTIVE=prod`) contiene solo ajustes no secretos, como desactivar el log de SQL.
- **Ningún secreto en el repositorio.** El `.gitignore` excluye `.env` y `application-local.*`.

## Alternativas consideradas

- **Poner la URL y las credenciales de producción en `application-prod.yml`.** Es el camino habitual por el que acaban contraseñas en GitHub.
- **`application.properties`.** Funciona igual, pero con la configuración anidada el YAML se lee mejor.

## Consecuencias

**Positivas**

- **Existe un único artefacto:** el mismo jar y la misma imagen corren en local y en producción. Solo cambia el entorno, y lo que se despliega es exactamente lo que se probó.
- Sin secretos en el historial de Git.

**Negativas o costes**

- La configuración efectiva depende del entorno. Para depurar hay que conocer el orden de prioridad de las fuentes de configuración de Spring.
