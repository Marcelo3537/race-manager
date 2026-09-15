package dev.marcelo.racemanager.team;

import java.util.List;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.team.dto.TeamRequest;
import dev.marcelo.racemanager.team.dto.TeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private static final String RESOURCE = "Team";

    private final TeamRepository repository;

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    public List<TeamResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TeamResponse findById(Long id) {
        Team team = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
        return toResponse(team);
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(
                    "Team '%s' already exists".formatted(request.name()));
        }

        Team team = new Team(
                request.name(),
                request.constructorName(),
                request.country()
        );

        return toResponse(repository.save(team));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));

        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Team '%s' already exists".formatted(request.name()));
        }

        team.setName(request.name());
        team.setConstructorName(request.constructorName());
        team.setCountry(request.country());

        return toResponse(team);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RESOURCE, id);
        }
        repository.deleteById(id);
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getConstructorName(),
                team.getCountry()
        );
    }

}
