package dev.marcelo.racemanager.circuit.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CircuitRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Country is required")
        @Size(max = 80, message = "Country must not exceed 80 characters")
        String country,

        @Size(max = 80, message = "City must not exceed 80 characters")
        String city,

        @NotNull(message = "Length is required")
        @Positive(message = "Length must be greater than zero")
        @Digits(integer = 2, fraction = 3,
                message = "Length must have at most 2 integer digits and 3 decimals")
        BigDecimal lengthKm
) {
}