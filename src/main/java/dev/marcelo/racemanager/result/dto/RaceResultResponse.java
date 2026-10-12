package dev.marcelo.racemanager.result.dto;

import dev.marcelo.racemanager.result.ResultStatus;

public record RaceResultResponse(
        Long id,
        Long raceId,
        String raceName,
        Long entryId,
        Long driverId,
        String driverName,
        String driverSurname,
        Long teamId,
        String teamName,
        Integer carNumber,
        Integer position,
        Integer points,
        Integer lapsCompleted,
        Boolean fastestLap,
        ResultStatus status
) {
}