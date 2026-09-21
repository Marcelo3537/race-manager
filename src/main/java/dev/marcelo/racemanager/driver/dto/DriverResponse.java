package dev.marcelo.racemanager.driver.dto;

import java.time.LocalDate;

public record DriverResponse(
        Long id,
        String name,
        String surname,
        String nationality,
        LocalDate dateOfBirth
) {
}