package dev.marcelo.racemanager.team.dto;

public record TeamResponse(
        Long id,
        String name,
        String constructorName,
        String country
) {
}
