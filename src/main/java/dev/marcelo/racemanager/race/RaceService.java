package dev.marcelo.racemanager.race;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.championship.ChampionshipService;
import dev.marcelo.racemanager.circuit.Circuit;
import dev.marcelo.racemanager.circuit.CircuitService;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.dto.RaceRequest;
import dev.marcelo.racemanager.race.dto.RaceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RaceService {

    private static final String RESOURCE = "Race";

    private final RaceRepository repository;
    private final ChampionshipService championshipService;
    private final CircuitService circuitService;

    public RaceService(RaceRepository repository,
                       ChampionshipService championshipService,
                       CircuitService circuitService) {
        this.repository = repository;
        this.championshipService = championshipService;
        this.circuitService = circuitService;
    }

    public List<RaceResponse> findAll() {
        return repository.findAllWithRelations()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RaceResponse findById(Long id) {
        Race race = repository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(race);
    }

    @Transactional
    public RaceResponse create(RaceRequest request) {
        Championship championship = championshipService.getEntityById(request.championshipId());
        Circuit circuit = circuitService.getEntityById(request.circuitId());

        if (repository.existsByChampionshipIdAndRound(request.championshipId(), request.round())) {
            throw new DuplicateResourceException(
                    "Round %d already exists for championship %d"
                            .formatted(request.round(), request.championshipId()));
        }

        Race race = new Race(
                championship,
                circuit,
                request.name(),
                request.round(),
                request.scheduledAt(),
                request.status());

        return toResponse(repository.save(race));
    }

    @Transactional
    public RaceResponse update(Long id, RaceRequest request) {
        Race race = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        Championship championship = championshipService.getEntityById(request.championshipId());
        Circuit circuit = circuitService.getEntityById(request.circuitId());

        if (repository.existsByChampionshipIdAndRoundAndIdNot(
                request.championshipId(), request.round(), id)) {
            throw new DuplicateResourceException(
                    "Round %d already exists for championship %d"
                            .formatted(request.round(), request.championshipId()));
        }

        race.setChampionship(championship);
        race.setCircuit(circuit);
        race.setName(request.name());
        race.setRound(request.round());
        race.setScheduledAt(request.scheduledAt());
        race.setStatus(request.status());

        return toResponse(race);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        repository.deleteById(id);
    }

    private RaceResponse toResponse(Race race) {
        return new RaceResponse(
                race.getId(),
                race.getChampionship().getId(),
                race.getChampionship().getName(),
                race.getCircuit().getId(),
                race.getCircuit().getName(),
                race.getName(),
                race.getRound(),
                race.getScheduledAt(),
                race.getStatus());
    }
}