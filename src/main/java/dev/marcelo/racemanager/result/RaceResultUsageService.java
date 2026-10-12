package dev.marcelo.racemanager.result;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RaceResultUsageService {

    private final RaceResultRepository repository;

    public RaceResultUsageService(RaceResultRepository repository) {
        this.repository = repository;
    }

    public boolean existsForRace(Long raceId) {
        return repository.existsByRaceId(raceId);
    }

    public boolean existsForEntry(Long entryId) {
        return repository.existsByEntryId(entryId);
    }
}