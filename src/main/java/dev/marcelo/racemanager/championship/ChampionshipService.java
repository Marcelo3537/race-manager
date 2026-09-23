package dev.marcelo.racemanager.championship;

import dev.marcelo.racemanager.championship.dto.ChampionshipRequest;
import dev.marcelo.racemanager.championship.dto.ChampionshipResponse;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceInUseException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.RaceUsageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChampionshipService {

    private static final String RESOURCE = "Championship";

    private final ChampionshipRepository repository;

    private final RaceUsageService raceUsageService;

    public ChampionshipService(ChampionshipRepository repository, RaceUsageService raceUsageService) {
        this.repository = repository;
        this.raceUsageService = raceUsageService;
    }

    public List<ChampionshipResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChampionshipResponse findById(Long id) {
        Championship championship = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(championship);
    }

    @Transactional
    public ChampionshipResponse create(ChampionshipRequest request) {
        if (repository.existsByNameIgnoreCaseAndSeason(request.name(), request.season())) {
            throw new DuplicateResourceException(
                    "Championship '%s' already exists for season %d"
                            .formatted(request.name(), request.season()));
        }

        Championship championship = new Championship(
                request.name(),
                request.season(),
                request.country(),
                request.status());

        return toResponse(repository.save(championship));
    }

    @Transactional
    public ChampionshipResponse update(Long id, ChampionshipRequest request) {
        Championship championship = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        if (repository.existsByNameIgnoreCaseAndSeasonAndIdNot(
                request.name(), request.season(), id)) {
            throw new DuplicateResourceException(
                    "Championship '%s' already exists for season %d"
                            .formatted(request.name(), request.season()));
        }

        championship.setName(request.name());
        championship.setSeason(request.season());
        championship.setCountry(request.country());
        championship.setStatus(request.status());

        return toResponse(championship);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        if (raceUsageService.existsForChampionship(id)) {
            throw new ResourceInUseException(
                    "Championship %d cannot be deleted because it has races".formatted(id));
        }
        repository.deleteById(id);
    }

    private ChampionshipResponse toResponse(Championship championship) {
        return new ChampionshipResponse(
                championship.getId(),
                championship.getName(),
                championship.getSeason(),
                championship.getCountry(),
                championship.getStatus());
    }

    @Transactional(readOnly = true)
    public Championship getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}