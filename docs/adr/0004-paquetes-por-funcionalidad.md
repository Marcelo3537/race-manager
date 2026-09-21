# 0004 — Organización en paquetes por funcionalidad

- **Estado:** Aceptada
- **Fecha:** septiembre de 2026

## Contexto

Hay dos formas habituales de organizar un proyecto Spring: por capa técnica (`controller/`, `service/`, `repository/`...) o por funcionalidad (`championship/`, `team/`...). Con unas nueve entidades previstas, la organización por capa produce carpetas con muchas clases de temas distintos, y trabajar en una sola funcionalidad obliga a recorrer media docena de carpetas.

## Decisión

El código se organiza **por funcionalidad**:

```
dev.marcelo.racemanager
├── RaceManagerApplication.java
├── championship/   (entidad, repositorio, servicio, controller, dto/)
├── team/
├── driver/
├── circuit/
└── common/
    ├── exception/  (excepciones de dominio y manejador global)
    └── health/     (endpoint de salud)
```

- Los repositorios son **package-private** (sin `public`): solo el servicio de su propio paquete puede usarlos. Si un controller de otro paquete intentara inyectarlo, el código no compilaría.
- La clase con `@SpringBootApplication` vive en el paquete raíz, porque el escaneo de componentes empieza en su paquete y solo baja hacia los subpaquetes.

## Alternativas consideradas

- **Paquetes por capa.** Es lo que muestran la mayoría de tutoriales y resulta familiar, pero no escala bien con muchas entidades y no permite restringir la visibilidad de los repositorios.

## Consecuencias

**Positivas**

- Todo lo de una funcionalidad está junto: se entiende, se modifica y se revisa en un único sitio.
- La regla "los controllers no usan repositorios" la garantiza el compilador, no la memoria del desarrollador.

**Negativas o costes**

- Es menos habitual en los ejemplos que se encuentran buscando en internet.
- Si la clase principal sale del paquete raíz, parte del código deja de escanearse **sin ningún error**. Ocurrió una vez; el síntoma fue `Found 0 JPA repository interfaces` en el log de arranque. Ese contador es la comprobación rápida.
