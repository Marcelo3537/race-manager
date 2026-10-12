package dev.marcelo.racemanager.result;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface RaceResultRepository extends JpaRepository<RaceResult, Long> {

    @Query("""
            SELECT r FROM RaceResult r
            JOIN FETCH r.entry e
            JOIN FETCH e.driver
            JOIN FETCH e.team
            WHERE r.race.id = :raceId
            ORDER BY r.position NULLS LAST, r.id
            """)
    List<RaceResult> findAllByRaceIdWithRelations(Long raceId);

    List<RaceResult> findAllByRaceId(Long raceId);

    boolean existsByRaceId(Long raceId);

    boolean existsByEntryId(Long entryId);

    void deleteAllByRaceId(Long raceId);
}