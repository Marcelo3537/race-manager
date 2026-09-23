package dev.marcelo.racemanager.race.dto;

import dev.marcelo.racemanager.race.RaceStatus;

import java.time.OffsetDateTime;

public record RaceResponse(
        Long id,
        Long championshipId,
        String championshipName,
        Long circuitId,
        String circuitName,
        String name,
        Integer round,
        OffsetDateTime scheduledAt,
        RaceStatus status
) {
}