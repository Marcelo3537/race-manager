# Race Manager

API REST para la gestión de campeonatos de automovilismo: campeonatos,
circuitos, carreras, equipos y pilotos, con cálculo automático de puntos,
clasificaciones y estadísticas.

Proyecto personal desarrollado para profundizar en Spring Boot y en el
desarrollo backend con Java.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL |
| Migraciones | Flyway |
| Seguridad | Spring Security + JWT |
| Tests | JUnit 5, Mockito, Testcontainers |
| Documentación | OpenAPI / Swagger UI |
| Build | Maven |

## Estado del proyecto

En desarrollo. Ver [docs/data-model.md](docs/data-model.md) para el modelo de datos.

## Entidades

- app_user: Un usuario de la aplicación, con su rol (USER o ADMIN)
- championship: Un campeonato (por ejemplo F1 2026)
- scoring_rule: Representa una fila de resultados de puntos a un piloto dentro de un campeonato
- championship_entry-  Representa la participación de un piloto en un campeonato, con un equipo y un número.
- driver: Representa un conductor
- team: Representa un equipo
- circuit: Representa un circuito
- race: Representa una carrera
- race_result: Representa el resultado obtenido por una inscripción en una carrera concreta


## Endpoints

### Autenticación
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registrar un usuario |
| POST | `/api/auth/login` | Iniciar sesión y obtener un token JWT |

### Campeonatos
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/championships` | Público |
| GET | `/api/championships/{id}` | Público |
| POST | `/api/championships` | ADMIN |
| PUT | `/api/championships/{id}` | ADMIN |
| DELETE | `/api/championships/{id}` | ADMIN |

### Pilotos
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/drivers` | Público |
| GET | `/api/drivers/{id}` | Público |
| POST | `/api/drivers` | ADMIN |
| PUT | `/api/drivers/{id}` | ADMIN |
| DELETE | `/api/drivers/{id}` | ADMIN |

### Equipos
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/teams` | Público |
| GET | `/api/teams/{id}` | Público |
| POST | `/api/teams` | ADMIN |
| PUT | `/api/teams/{id}` | ADMIN |
| DELETE | `/api/teams/{id}` | ADMIN |

### Circuitos
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/circuits` | Público |
| GET | `/api/circuits/{id}` | Público |
| POST | `/api/circuits` | ADMIN |
| PUT | `/api/circuits/{id}` | ADMIN |
| DELETE | `/api/circuits/{id}` | ADMIN |

### Carreras
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/races` | Público |
| GET | `/api/races/{id}` | Público |
| POST | `/api/races` | ADMIN |
| PUT | `/api/races/{id}` | ADMIN |
| DELETE | `/api/races/{id}` | ADMIN |

### Resultados
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/races/{id}/results` | Público |
| POST | `/api/races/{id}/results` | ADMIN |

### Clasificaciones
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/championships/{id}/standings/drivers` | Público |
| GET | `/api/championships/{id}/standings/teams` | Público |

### Estadísticas
| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/drivers/{id}/statistics` | Público |
| GET | `/api/teams/{id}/statistics` | Público |

> La columna de roles es orientativa y se implementa en la fase 14.

## Cómo ejecutarlo en local

<!-- Lo completaremos en la fase 1 -->