package dev.marcelo.racemanager.championship.dto;

import dev.marcelo.racemanager.championship.ChampionshipStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChampionshipRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotNull(message = "Season is required")
        @Min(value = 1950, message = "Season must be 1950 or later")
        @Max(value = 2100, message = "Season must be 2100 or earlier")
        Integer season,

        @Size(max = 80, message = "Country must not exceed 80 characters")
        String country,

        @NotNull(message = "Status is required")
        ChampionshipStatus status
) {
}
