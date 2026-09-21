package dev.marcelo.racemanager.circuit;

import org.springframework.data.jpa.repository.JpaRepository;

interface CircuitRepository extends JpaRepository<Circuit, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}