package dev.marcelo.racemanager.result.dto;

import dev.marcelo.racemanager.result.ResultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RaceResultRequest(

        @NotNull(message = "Entry id is required")
        Long entryId,

        @Positive(message = "Position must be greater than zero")
        Integer position,

        @NotNull(message = "Laps completed is required")
        @PositiveOrZero(message = "Laps completed must be zero or greater")
        Integer lapsCompleted,

        @NotNull(message = "Fastest lap is required")
        Boolean fastestLap,

        @NotNull(message = "Status is required")
        ResultStatus status
) {
}