# 0003 — Alcance del MVP

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026
- **Relacionadas:** [0002](0002-slices-verticales-y-despliegue-temprano.md)

## Contexto

La lista inicial de funcionalidades era amplia. Un proyecto personal sin plazo corre el riesgo de crecer en anchura sin llegar nunca a estar terminado, y un proyecto a medias demuestra menos que uno pequeño y completo.

## Decisión

**Dentro del MVP:** CRUD de campeonatos, equipos, pilotos, circuitos y carreras; inscripciones de pilotos en campeonatos; registro de resultados; cálculo automático de puntos con reglas configurables; clasificaciones de pilotos y de equipos; estadísticas básicas; registro, login con JWT y roles; validación y errores; paginación y filtros; tests; documentación OpenAPI; despliegue.

**Fuera del MVP:** entidad `Car`, favoritos, pole positions, pilotos retirados, historial de equipos, coordenadas de circuitos, búsqueda avanzada y campeonatos históricos.

**Matices:**

- La vuelta rápida sí entra, como un booleano en el resultado: su coste es mínimo y alimenta las estadísticas.
- No se añaden métodos de consulta "por si acaso" (por ejemplo, buscar pilotos por nacionalidad) hasta la fase de filtros, donde se resolverán de forma general.
- Si hubiera que recortar, las estadísticas se quitarían antes que los tests o la seguridad. Un proyecto sin estadísticas está completo; uno sin tests, no.

## Alternativas consideradas

- **Incluir `Car`.** No interviene en el cálculo de puntos ni en las clasificaciones; solo añade una relación que mantener.
- **Implementar también las funcionalidades opcionales.** Más vistoso, pero aleja el momento de tener un núcleo sólido y terminado.

## Consecuencias

**Positivas**

- Foco en lo que demuestra conocimientos de backend: modelo de datos, lógica de negocio, seguridad, tests y despliegue.
- El modelo no impide añadir después lo que ha quedado fuera.

**Negativas o costes**

- Algunas funcionalidades atractivas para una demo quedan aplazadas.
