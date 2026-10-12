package dev.marcelo.racemanager.result.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RaceResultsRequest(

        @NotEmpty(message = "At least one result is required")
        @Valid
        List<RaceResultRequest> results
) {
}