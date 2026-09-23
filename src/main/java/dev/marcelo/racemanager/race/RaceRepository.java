package dev.marcelo.racemanager.race;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface RaceRepository extends JpaRepository<Race, Long> {

    @Query("""
            SELECT r FROM Race r
            JOIN FETCH r.championship
            JOIN FETCH r.circuit
            ORDER BY r.championship.id, r.round
            """)
    List<Race> findAllWithRelations();

    @Query("""
            SELECT r FROM Race r
            JOIN FETCH r.championship
            JOIN FETCH r.circuit
            WHERE r.id = :id
            """)
    Optional<Race> findByIdWithRelations(Long id);

    boolean existsByChampionshipIdAndRound(Long championshipId, Integer round);

    boolean existsByChampionshipIdAndRoundAndIdNot(Long championshipId, Integer round, Long id);

    boolean existsByChampionshipId(Long championshipId);

    boolean existsByCircuitId(Long circuitId);
}