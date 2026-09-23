package dev.marcelo.racemanager.race;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RaceUsageService {

    private final RaceRepository repository;

    public RaceUsageService(RaceRepository repository) {
        this.repository = repository;
    }

    public boolean existsForChampionship(Long championshipId) {
        return repository.existsByChampionshipId(championshipId);
    }

    public boolean existsForCircuit(Long circuitId) {
        return repository.existsByCircuitId(circuitId);
    }
}