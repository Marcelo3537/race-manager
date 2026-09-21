# 0021 — Despliegue en Render con la base de datos en Neon

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0002](0002-slices-verticales-y-despliegue-temprano.md), [0020](0020-configuracion-por-entorno.md)

## Contexto

La API tiene que estar publicada en una URL pública, con su base de datos, **durante meses**: el enlace forma parte de un portfolio y de un currículum. Un enlace que deja de funcionar causa peor impresión que no poner ninguno. El presupuesto es cero.

## Decisión

- **Aplicación en Render:** servicio web del plan gratuito, región Frankfurt, construido desde un Dockerfile y desplegado automáticamente con cada push a `main`.
- **Base de datos en Neon:** plan gratuito permanente, región Frankfurt. Conexión por variables de entorno ([0020](0020-configuracion-por-entorno.md)) con `sslmode=require`.
- **Dockerfile multietapa:**
  - Etapa de compilación con `maven:3.9-eclipse-temurin-21`. Se copia primero el `pom.xml` y se descargan las dependencias, y después el código, para que la capa de dependencias se reutilice mientras no cambie el `pom.xml`. Compila con `-DskipTests`.
  - Etapa de ejecución con `eclipse-temurin:21-jre-alpine`, que solo recibe el `.jar`.
- **`.dockerignore`** para no enviar `target/`, `.git/` ni la configuración del IDE al build.
- **Health check en `/api/health`** mediante un controller propio.

## Alternativas consideradas

- **PostgreSQL gratuito de Render.** En el momento de la decisión, sus bases de datos gratuitas expiraban 30 días después de crearse. El enlace del portfolio moriría al mes y medio.
- **Railway.** Funcionaba con un crédito de prueba único; no sirve para mantener algo publicado indefinidamente.
- **Spring Boot Actuator para el health check.** Es la opción estándar, pero añade una dependencia que hoy no hace falta. Se puede incorporar más adelante.
- **Imagen de una sola etapa.** Unos 800 MB frente a unos 200, y con el código fuente y el compilador dentro de la imagen de producción.

## Consecuencias

**Positivas**

- Coste cero y sin fecha de caducidad.
- Aplicación y base de datos en proveedores distintos, que es lo habitual fuera de un proyecto de estudiante.

**Negativas o costes**

- El servicio gratuito de Render se duerme tras unos minutos sin tráfico y la primera petición tarda alrededor de un minuto en responder. Está avisado en el README.
- Los tests no se ejecutan al construir la imagen. Hasta que haya integración continua, un push puede desplegar código con tests rotos.
- Un despliegue fallido no tira la aplicación: Render mantiene viva la versión anterior (comprobado cuando falló uno).
