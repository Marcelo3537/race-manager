# Modelo de datos

## Entidades
README.md

## Relaciones

1. Un campeonato tiene muchas carreras, y cada carrera pertenece a un único campeonato.
2. Un circuito acoge muchas carreras, y cada carrera se disputa en un único circuito.
3. Un campeonato tiene varias reglas de puntuación, y una regla de puntuacion solo puede tener un campeonato
4. Un campeonato puede tener varias inscripciones, y una inscripción solo puede tener un único campeonato
5. Un piloto puede tener varias inscripciones, y una inscripcion solo puede tener un único piloto
6. Un equipo puede tener varias inscripciones, y una inscripcion tiene solo un equipo
7. Una carrera tiene varios resultados, y un resultado solo pertenece a una carrera
8. Una inscripcion puede tener varios resultados de carrera, pero un resultado de carrera solo puede tener una inscripcion

## Decisiones de diseño

### Por qué existe `championship_entry`
Porque se necesita saber el equipo en el que estaba un piloto para calcular cuantos puntos ha conseguido para ese equipo (por ejemplo
para calcular el campeonato de constructores). Y porque un piloto cambia de equipo entre temporadas, así que el equipo no es una característica del piloto, sino de su participación en un campeonato

### Por qué `race_result` apunta a `championship_entry` y no a `driver`
Porque para apuntar el resultado se necesita saber a que equipo pertenecia el piloto y con que numero iba