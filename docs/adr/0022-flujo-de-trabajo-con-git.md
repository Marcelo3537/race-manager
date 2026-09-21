# 0022 — Flujo de trabajo con Git

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0002](0002-slices-verticales-y-despliegue-temprano.md), [0021](0021-despliegue-render-y-neon.md)

## Contexto

El proyecto lo desarrolla una sola persona, y el historial de Git forma parte de lo que ve quien revise el repositorio. Además, cada push a `main` despliega a producción.

## Decisión

- **Conventional Commits:** `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`, con la descripción en inglés y en imperativo (`add team repository`).
- **Un commit por paso o capa**, separando código de producción y tests cuando se entregan juntos.
- **Trabajo directo en `main`** por ahora. A partir de la fase de seguridad, la más invasiva, se trabajará en rama y se fusionará con Pull Request.
- **Arrancar la aplicación en local antes de hacer push**, porque `main` despliega a producción.
- Si hay que dejar algo a medias, se sube igualmente con un commit `wip:`.
- Documentación y README en **español**; mensajes de commit en **inglés**, por convención.

## Alternativas consideradas

- **Ramas por funcionalidad desde el principio.** Aportan aislamiento, pero para una sola persona que construye cada fase sobre la anterior suponen un sobrecoste sin beneficio claro.
- **Commits grandes al final de cada fase.** Historial ilegible, que no muestra cómo se construyó el proyecto.

## Consecuencias

**Positivas**

- El historial cuenta el orden de construcción y separa funcionalidad, tests y documentación.

**Negativas o costes**

- Con `main` desplegando a producción, un descuido llega a Render. Ocurrió una vez: un repositorio con un método derivado mal nombrado impidió el arranque y el despliegue falló. Un arranque local lo habría detectado.
