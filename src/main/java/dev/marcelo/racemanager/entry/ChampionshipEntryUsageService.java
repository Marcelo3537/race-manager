package dev.marcelo.racemanager.entry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChampionshipEntryUsageService {

    private final ChampionshipEntryRepository repository;

    public ChampionshipEntryUsageService(ChampionshipEntryRepository repository) {
        this.repository = repository;
    }

    public boolean existsForChampionship(Long championshipId) {
        return repository.existsByChampionshipId(championshipId);
    }

    public boolean existsForDriver(Long driverId) {
        return repository.existsByDriverId(driverId);
    }

    public boolean existsForTeam(Long teamId) {
        return repository.existsByTeamId(teamId);
    }
}