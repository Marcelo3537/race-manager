package dev.marcelo.racemanager.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface ChampionshipEntryRepository extends JpaRepository<ChampionshipEntry, Long> {

    @Query("""
            SELECT e FROM ChampionshipEntry e
            JOIN FETCH e.championship
            JOIN FETCH e.driver
            JOIN FETCH e.team
            WHERE e.championship.id = :championshipId
            ORDER BY e.carNumber
            """)
    List<ChampionshipEntry> findAllByChampionshipIdWithRelations(Long championshipId);

    @Query("""
            SELECT e FROM ChampionshipEntry e
            JOIN FETCH e.championship
            JOIN FETCH e.driver
            JOIN FETCH e.team
            WHERE e.id = :id AND e.championship.id = :championshipId
            """)
    Optional<ChampionshipEntry> findByIdAndChampionshipIdWithRelations(Long id, Long championshipId);

    Optional<ChampionshipEntry> findByIdAndChampionshipId(Long id, Long championshipId);

    boolean existsByChampionshipIdAndDriverId(Long championshipId, Long driverId);

    boolean existsByChampionshipIdAndDriverIdAndIdNot(Long championshipId, Long driverId, Long id);

    boolean existsByChampionshipIdAndCarNumber(Long championshipId, Integer carNumber);

    boolean existsByChampionshipIdAndCarNumberAndIdNot(Long championshipId, Integer carNumber, Long id);

    boolean existsByChampionshipId(Long championshipId);

    boolean existsByDriverId(Long driverId);

    boolean existsByTeamId(Long teamId);
}