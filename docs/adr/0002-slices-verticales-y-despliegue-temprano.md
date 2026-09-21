# 0002 — Construcción por slices verticales y despliegue temprano

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0019](0019-estrategia-de-tests.md), [0021](0021-despliegue-render-y-neon.md), [0022](0022-flujo-de-trabajo-con-git.md)

## Contexto

El plan inicial construía el proyecto por capas horizontales (todas las entidades, luego todos los repositorios, luego todos los servicios...) y dejaba los tests y el despliegue para las últimas fases. Con ese orden, durante semanas no existe nada que funcione de punta a punta, los errores de diseño se descubren cuando ya están replicados en todas las entidades, y el primer despliegue llega con decenas de piezas que pueden fallar a la vez.

## Decisión

1. **Slice vertical primero.** Se construye una funcionalidad completa de arriba abajo (migración, entidad, repositorio, servicio, DTOs, controller, validación, errores y tests) con `Championship`. Solo cuando funciona entera se replica el patrón con `Team`, `Driver` y `Circuit`.
2. **Tests junto al código.** Cada funcionalidad se entrega con sus tests; no hay una fase final de testing.
3. **Despliegue en la fase 2.** Se despliega un esqueleto con un único endpoint de salud antes de escribir lógica de negocio. Desde entonces, cada push a `main` despliega.
4. **Seguridad en una fase tardía.** Spring Security y JWT se añaden cuando la lógica de negocio ya existe.

## Alternativas consideradas

- **Capas horizontales.** Parece ordenado, pero retrasa la primera funcionalidad utilizable y multiplica los errores antes de detectarlos.
- **Tests al final.** El código escrito sin tests suele ser difícil de testear después y obliga a reescribir.
- **Despliegue al final.** Concentra en un solo momento todos los problemas de configuración, variables de entorno y red.
- **Seguridad desde el principio.** Protege antes, pero obliga a gestionar tokens en cada prueba manual y en cada test durante todas las fases de negocio.

## Consecuencias

**Positivas**

- El patrón se valida una vez antes de multiplicarlo. En la fase 6 los errores fueron de descuido al copiar, no de diseño.
- El despliegue está probado desde el principio y cada cambio se despliega en pequeño.

**Negativas o costes**

- Al añadir la seguridad habrá que adaptar los tests de controller e integración para que se autentiquen.
- Como `main` despliega a producción en cada push, hay que arrancar la aplicación en local antes de subir. Un despliegue falló por un método derivado con un nombre incorrecto que un arranque local habría detectado.
