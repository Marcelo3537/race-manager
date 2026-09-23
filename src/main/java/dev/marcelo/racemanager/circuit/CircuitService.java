package dev.marcelo.racemanager.circuit;

import dev.marcelo.racemanager.circuit.dto.CircuitRequest;
import dev.marcelo.racemanager.circuit.dto.CircuitResponse;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceInUseException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.RaceUsageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CircuitService {

    private static final String RESOURCE = "Circuit";

    private final CircuitRepository repository;

    private final RaceUsageService raceUsageService;

    public CircuitService(CircuitRepository repository, RaceUsageService raceUsageService) {
        this.repository = repository;
        this.raceUsageService = raceUsageService;
    }

    public List<CircuitResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CircuitResponse findById(Long id) {
        Circuit circuit = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(circuit);
    }

    @Transactional
    public CircuitResponse create(CircuitRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(
                    "Circuit '%s' already exists".formatted(request.name()));
        }

        Circuit circuit = new Circuit(
                request.name(),
                request.country(),
                request.city(),
                request.lengthKm());

        return toResponse(repository.save(circuit));
    }

    @Transactional
    public CircuitResponse update(Long id, CircuitRequest request) {
        Circuit circuit = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Circuit '%s' already exists".formatted(request.name()));
        }

        circuit.setName(request.name());
        circuit.setCountry(request.country());
        circuit.setCity(request.city());
        circuit.setLengthKm(request.lengthKm());

        return toResponse(circuit);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        if (raceUsageService.existsForCircuit(id)) {
            throw new ResourceInUseException(
                    "Circuit %d cannot be deleted because it has races".formatted(id));
        }
        repository.deleteById(id);
    }

    private CircuitResponse toResponse(Circuit circuit) {
        return new CircuitResponse(
                circuit.getId(),
                circuit.getName(),
                circuit.getCountry(),
                circuit.getCity(),
                circuit.getLengthKm());
    }

    @Transactional(readOnly = true)
    public Circuit getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}