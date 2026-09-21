package dev.marcelo.racemanager.driver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DriverRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 60, message = "Name must not exceed 60 characters")
        String name,

        @NotBlank(message = "Surname is required")
        @Size(max = 60, message = "Surname must not exceed 60 characters")
        String surname,

        @NotBlank(message = "Nationality is required")
        @Size(max = 60, message = "Nationality must not exceed 60 characters")
        String nationality,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
) {
}