package dev.marcelo.racemanager.driver;

import dev.marcelo.racemanager.common.exception.ResourceInUseException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.dto.DriverRequest;
import dev.marcelo.racemanager.driver.dto.DriverResponse;
import dev.marcelo.racemanager.entry.ChampionshipEntryUsageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DriverService {

    private static final String RESOURCE = "Driver";

    private final DriverRepository repository;
    private final ChampionshipEntryUsageService entryUsageService;

    public DriverService(DriverRepository repository, ChampionshipEntryUsageService entryUsageService) {
        this.repository = repository;
        this.entryUsageService = entryUsageService;
    }

    public List<DriverResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DriverResponse findById(Long id) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse create(DriverRequest request) {
        Driver driver = new Driver(
                request.name(),
                request.surname(),
                request.nationality(),
                request.dateOfBirth());

        return toResponse(repository.save(driver));
    }

    @Transactional
    public DriverResponse update(Long id, DriverRequest request) {
        Driver driver = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        driver.setName(request.name());
        driver.setSurname(request.surname());
        driver.setNationality(request.nationality());
        driver.setDateOfBirth(request.dateOfBirth());

        return toResponse(driver);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        if (entryUsageService.existsForDriver(id)) {
            throw new ResourceInUseException(
                    "Driver %d cannot be deleted because it has championship entries".formatted(id));
        }
        repository.deleteById(id);
    }

    private DriverResponse toResponse(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getSurname(),
                driver.getNationality(),
                driver.getDateOfBirth());
    }

    @Transactional(readOnly = true)
    public Driver getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}