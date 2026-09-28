package dev.marcelo.racemanager.entry;

import dev.marcelo.racemanager.entry.dto.ChampionshipEntryRequest;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryResponse;
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
@RequestMapping("/api/championships/{championshipId}/entries")
public class ChampionshipEntryController {

    private final ChampionshipEntryService service;

    public ChampionshipEntryController(ChampionshipEntryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChampionshipEntryResponse> findAll(@PathVariable Long championshipId) {
        return service.findAllByChampionship(championshipId);
    }

    @GetMapping("/{id}")
    public ChampionshipEntryResponse findById(@PathVariable Long championshipId,
                                              @PathVariable Long id) {
        return service.findById(championshipId, id);
    }

    @PostMapping
    public ResponseEntity<ChampionshipEntryResponse> create(
            @PathVariable Long championshipId,
            @Valid @RequestBody ChampionshipEntryRequest request,
            UriComponentsBuilder uriBuilder) {

        ChampionshipEntryResponse created = service.create(championshipId, request);

        URI location = uriBuilder
                .path("/api/championships/{championshipId}/entries/{id}")
                .buildAndExpand(championshipId, created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ChampionshipEntryResponse update(
            @PathVariable Long championshipId,
            @PathVariable Long id,
            @Valid @RequestBody ChampionshipEntryRequest request) {

        return service.update(championshipId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long championshipId,
                                       @PathVariable Long id) {
        service.delete(championshipId, id);
        return ResponseEntity.noContent().build();
    }
}