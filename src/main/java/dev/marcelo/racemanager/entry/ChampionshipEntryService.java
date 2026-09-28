package dev.marcelo.racemanager.entry;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.championship.ChampionshipService;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.Driver;
import dev.marcelo.racemanager.driver.DriverService;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryRequest;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryResponse;
import dev.marcelo.racemanager.team.Team;
import dev.marcelo.racemanager.team.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChampionshipEntryService {

    private static final String RESOURCE = "Championship entry";

    private final ChampionshipEntryRepository repository;
    private final ChampionshipService championshipService;
    private final DriverService driverService;
    private final TeamService teamService;

    public ChampionshipEntryService(ChampionshipEntryRepository repository,
                                    ChampionshipService championshipService,
                                    DriverService driverService,
                                    TeamService teamService) {
        this.repository = repository;
        this.championshipService = championshipService;
        this.driverService = driverService;
        this.teamService = teamService;
    }

    public List<ChampionshipEntryResponse> findAllByChampionship(Long championshipId) {
        championshipService.getEntityById(championshipId);

        return repository.findAllByChampionshipIdWithRelations(championshipId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChampionshipEntryResponse findById(Long championshipId, Long id) {
        ChampionshipEntry entry = repository
                .findByIdAndChampionshipIdWithRelations(id, championshipId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(entry);
    }

    @Transactional
    public ChampionshipEntryResponse create(Long championshipId,
                                            ChampionshipEntryRequest request) {
        Championship championship = championshipService.getEntityById(championshipId);
        Driver driver = driverService.getEntityById(request.driverId());
        Team team = teamService.getEntityById(request.teamId());

        if (repository.existsByChampionshipIdAndDriverId(championshipId, request.driverId())) {
            throw new DuplicateResourceException(
                    "Driver %d is already entered in championship %d"
                            .formatted(request.driverId(), championshipId));
        }

        if (repository.existsByChampionshipIdAndCarNumber(championshipId, request.carNumber())) {
            throw new DuplicateResourceException(
                    "Car number %d is already taken in championship %d"
                            .formatted(request.carNumber(), championshipId));
        }

        ChampionshipEntry entry =
                new ChampionshipEntry(championship, driver, team, request.carNumber());

        return toResponse(repository.save(entry));
    }

    @Transactional
    public ChampionshipEntryResponse update(Long championshipId,
                                            Long id,
                                            ChampionshipEntryRequest request) {
        ChampionshipEntry entry = repository.findByIdAndChampionshipId(id, championshipId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        Driver driver = driverService.getEntityById(request.driverId());
        Team team = teamService.getEntityById(request.teamId());

        if (repository.existsByChampionshipIdAndDriverIdAndIdNot(
                championshipId, request.driverId(), id)) {
            throw new DuplicateResourceException(
                    "Driver %d is already entered in championship %d"
                            .formatted(request.driverId(), championshipId));
        }

        if (repository.existsByChampionshipIdAndCarNumberAndIdNot(
                championshipId, request.carNumber(), id)) {
            throw new DuplicateResourceException(
                    "Car number %d is already taken in championship %d"
                            .formatted(request.carNumber(), championshipId));
        }

        entry.setDriver(driver);
        entry.setTeam(team);
        entry.setCarNumber(request.carNumber());

        return toResponse(entry);
    }

    @Transactional
    public void delete(Long championshipId, Long id) {
        ChampionshipEntry entry = repository.findByIdAndChampionshipId(id, championshipId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        repository.delete(entry);
    }

    private ChampionshipEntryResponse toResponse(ChampionshipEntry entry) {
        return new ChampionshipEntryResponse(
                entry.getId(),
                entry.getChampionship().getId(),
                entry.getChampionship().getName(),
                entry.getDriver().getId(),
                entry.getDriver().getName(),
                entry.getDriver().getSurname(),
                entry.getTeam().getId(),
                entry.getTeam().getName(),
                entry.getCarNumber());
    }
}