package dev.marcelo.racemanager.entry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChampionshipEntryRequest(

        @NotNull(message = "Driver id is required")
        Long driverId,

        @NotNull(message = "Team id is required")
        Long teamId,

        @NotNull(message = "Car number is required")
        @Min(value = 1, message = "Car number must be between 1 and 99")
        @Max(value = 99, message = "Car number must be between 1 and 99")
        Integer carNumber
) {
}