package dev.marcelo.racemanager.championship;

import org.springframework.data.jpa.repository.JpaRepository;

interface ChampionshipRepository extends JpaRepository<Championship, Long> {

    boolean existsByNameIgnoreCaseAndSeason(String name, Integer season);

    boolean existsByNameIgnoreCaseAndSeasonAndIdNot(String name, Integer season, Long id);
}