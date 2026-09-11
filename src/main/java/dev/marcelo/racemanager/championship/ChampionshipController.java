package dev.marcelo.racemanager.championship;

import dev.marcelo.racemanager.championship.dto.ChampionshipRequest;
import dev.marcelo.racemanager.championship.dto.ChampionshipResponse;
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
@RequestMapping("/api/championships")
public class ChampionshipController {

    private final ChampionshipService service;

    public ChampionshipController(ChampionshipService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChampionshipResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ChampionshipResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ChampionshipResponse> create(
            @RequestBody ChampionshipRequest request,
            UriComponentsBuilder uriBuilder) {

        ChampionshipResponse created = service.create(request);

        URI location = uriBuilder
                .path("/api/championships/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ChampionshipResponse update(
            @PathVariable Long id,
            @RequestBody ChampionshipRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
