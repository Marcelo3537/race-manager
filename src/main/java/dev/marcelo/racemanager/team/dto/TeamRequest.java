package dev.marcelo.racemanager.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 80, message = "Name must not exceed 80 characters")
        String name,

        @NotBlank(message = "Constructor name is required")
        @Size(max = 80, message = "Constructor name must not exceed 80 characters")
        String constructorName,

        @Size(max = 80, message = "Country must not exceed 80 characters")
        String country
) {
}
