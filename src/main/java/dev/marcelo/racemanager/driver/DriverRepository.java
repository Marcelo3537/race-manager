package dev.marcelo.racemanager.driver;

import org.springframework.data.jpa.repository.JpaRepository;

interface DriverRepository extends JpaRepository<Driver, Long> {
}