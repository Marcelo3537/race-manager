package dev.marcelo.racemanager.team;

import dev.marcelo.racemanager.team.dto.TeamRequest;
import dev.marcelo.racemanager.team.dto.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @GetMapping
    public List<TeamResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TeamResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(
            @Valid @RequestBody TeamRequest request,
            UriComponentsBuilder uriBuilder) {

        TeamResponse created = service.create(request);

        URI location = uriBuilder
                .path("/api/teams/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public TeamResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
