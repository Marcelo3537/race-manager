package dev.marcelo.racemanager.race.dto;

import dev.marcelo.racemanager.race.RaceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RaceRequest(

        @NotNull(message = "Championship id is required")
        Long championshipId,

        @NotNull(message = "Circuit id is required")
        Long circuitId,

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotNull(message = "Round is required")
        @Positive(message = "Round must be greater than zero")
        Integer round,

        @NotNull(message = "Scheduled date is required")
        OffsetDateTime scheduledAt,

        @NotNull(message = "Status is required")
        RaceStatus status
) {
}