package dev.marcelo.racemanager.result;

import dev.marcelo.racemanager.result.dto.RaceResultResponse;
import dev.marcelo.racemanager.result.dto.RaceResultsRequest;
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
@RequestMapping("/api/races/{raceId}/results")
public class RaceResultController {

    private final RaceResultService service;

    public RaceResultController(RaceResultService service) {
        this.service = service;
    }

    @GetMapping
    public List<RaceResultResponse> findAll(@PathVariable Long raceId) {
        return service.findAllByRace(raceId);
    }

    @PostMapping
    public ResponseEntity<List<RaceResultResponse>> register(
            @PathVariable Long raceId,
            @Valid @RequestBody RaceResultsRequest request,
            UriComponentsBuilder uriBuilder) {

        List<RaceResultResponse> created = service.registerResults(raceId, request);

        URI location = uriBuilder
                .path("/api/races/{raceId}/results")
                .buildAndExpand(raceId)
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping
    public List<RaceResultResponse> replace(
            @PathVariable Long raceId,
            @Valid @RequestBody RaceResultsRequest request) {

        return service.replaceResults(raceId, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long raceId) {
        service.deleteResults(raceId);
        return ResponseEntity.noContent().build();
    }
}