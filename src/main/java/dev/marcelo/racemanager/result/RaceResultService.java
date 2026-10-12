package dev.marcelo.racemanager.result;

import dev.marcelo.racemanager.common.exception.BusinessRuleException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.entry.ChampionshipEntry;
import dev.marcelo.racemanager.entry.ChampionshipEntryService;
import dev.marcelo.racemanager.race.Race;
import dev.marcelo.racemanager.race.RaceService;
import dev.marcelo.racemanager.race.RaceStatus;
import dev.marcelo.racemanager.result.dto.RaceResultRequest;
import dev.marcelo.racemanager.result.dto.RaceResultResponse;
import dev.marcelo.racemanager.result.dto.RaceResultsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RaceResultService {

    private static final String RESOURCE = "Race result";

    private final RaceResultRepository repository;
    private final RaceService raceService;
    private final ChampionshipEntryService entryService;

    public RaceResultService(RaceResultRepository repository,
                             RaceService raceService,
                             ChampionshipEntryService entryService) {
        this.repository = repository;
        this.raceService = raceService;
        this.entryService = entryService;
    }

    public List<RaceResultResponse> findAllByRace(Long raceId) {
        Race race = raceService.getEntityById(raceId);

        return repository.findAllByRaceIdWithRelations(raceId)
                .stream()
                .map(result -> toResponse(race, result))
                .toList();
    }

    @Transactional
    public List<RaceResultResponse> registerResults(Long raceId, RaceResultsRequest request) {
        Race race = raceService.getEntityById(raceId);

        if (repository.existsByRaceId(raceId)) {
            throw new BusinessRuleException(
                    "Race %d already has results. Use PUT to replace them".formatted(raceId));
        }

        return saveResults(race, request.results());
    }

    @Transactional
    public List<RaceResultResponse> replaceResults(Long raceId, RaceResultsRequest request) {
        Race race = raceService.getEntityById(raceId);

        repository.deleteAllByRaceId(raceId);
        repository.flush();

        return saveResults(race, request.results());
    }

    @Transactional
    public void deleteResults(Long raceId) {
        Race race = raceService.getEntityById(raceId);

        if (!repository.existsByRaceId(raceId)) {
            throw new ResourceNotFoundException(RESOURCE, raceId);
        }

        repository.deleteAllByRaceId(raceId);
        race.setStatus(RaceStatus.SCHEDULED);
    }

    private List<RaceResultResponse> saveResults(Race race, List<RaceResultRequest> requests) {
        Map<Long, ChampionshipEntry> entriesById = entryService
                .getEntitiesByChampionship(race.getChampionship().getId())
                .stream()
                .collect(Collectors.toMap(ChampionshipEntry::getId, Function.identity()));

        validate(requests, entriesById.keySet());

        List<RaceResult> results = requests.stream()
                .map(r -> new RaceResult(
                        race,
                        entriesById.get(r.entryId()),
                        r.position(),
                        r.lapsCompleted(),
                        r.fastestLap(),
                        r.status()))
                .toList();

        List<RaceResult> saved = repository.saveAll(results);

        race.setStatus(RaceStatus.FINISHED);

        return saved.stream()
                .map(result -> toResponse(race, result))
                .toList();
    }

    private void validate(List<RaceResultRequest> requests, Set<Long> championshipEntryIds) {
        Set<Long> submittedEntryIds = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        int fastestLaps = 0;

        for (RaceResultRequest request : requests) {
            if (!submittedEntryIds.add(request.entryId())) {
                throw new BusinessRuleException(
                        "Entry %d appears more than once in the results"
                                .formatted(request.entryId()));
            }

            if (!championshipEntryIds.contains(request.entryId())) {
                throw new BusinessRuleException(
                        "Entry %d is not registered in this championship"
                                .formatted(request.entryId()));
            }

            if (request.status() == ResultStatus.FINISHED) {
                if (request.position() == null) {
                    throw new BusinessRuleException(
                            "Entry %d finished the race and must have a position"
                                    .formatted(request.entryId()));
                }
                if (!positions.add(request.position())) {
                    throw new BusinessRuleException(
                            "Position %d is assigned to more than one entry"
                                    .formatted(request.position()));
                }
            } else if (request.position() != null) {
                throw new BusinessRuleException(
                        "Entry %d did not finish the race and must not have a position"
                                .formatted(request.entryId()));
            }

            if (Boolean.TRUE.equals(request.fastestLap())) {
                fastestLaps++;
            }
        }

        if (fastestLaps > 1) {
            throw new BusinessRuleException("Only one entry can have the fastest lap");
        }

        if (submittedEntryIds.size() != championshipEntryIds.size()) {
            throw new BusinessRuleException(
                    "The results must include every entry of the championship: expected %d, received %d"
                            .formatted(championshipEntryIds.size(), submittedEntryIds.size()));
        }
    }

    private RaceResultResponse toResponse(Race race, RaceResult result) {
        ChampionshipEntry entry = result.getEntry();

        return new RaceResultResponse(
                result.getId(),
                race.getId(),
                race.getName(),
                entry.getId(),
                entry.getDriver().getId(),
                entry.getDriver().getName(),
                entry.getDriver().getSurname(),
                entry.getTeam().getId(),
                entry.getTeam().getName(),
                entry.getCarNumber(),
                result.getPosition(),
                result.getPoints(),
                result.getLapsCompleted(),
                result.getFastestLap(),
                result.getStatus());
    }
}