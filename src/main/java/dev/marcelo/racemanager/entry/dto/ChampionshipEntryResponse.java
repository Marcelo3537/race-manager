package dev.marcelo.racemanager.entry.dto;

public record ChampionshipEntryResponse(
        Long id,
        Long championshipId,
        String championshipName,
        Long driverId,
        String driverName,
        String driverSurname,
        Long teamId,
        String teamName,
        Integer carNumber
) {
}