package dev.marcelo.racemanager.circuit.dto;

import java.math.BigDecimal;

public record CircuitResponse(
        Long id,
        String name,
        String country,
        String city,
        BigDecimal lengthKm
) {
}