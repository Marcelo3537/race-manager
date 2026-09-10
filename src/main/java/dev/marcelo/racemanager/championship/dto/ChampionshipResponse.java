package dev.marcelo.racemanager.championship.dto;

import dev.marcelo.racemanager.championship.ChampionshipStatus;

public record ChampionshipResponse(
        Long id,
        String name,
        Integer season,
        String country,
        ChampionshipStatus status
) {
}