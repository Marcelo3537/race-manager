package dev.marcelo.racemanager.circuit;

import dev.marcelo.racemanager.circuit.dto.CircuitRequest;
import dev.marcelo.racemanager.circuit.dto.CircuitResponse;
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
@RequestMapping("/api/circuits")
public class CircuitController {

    private final CircuitService service;

    public CircuitController(CircuitService service) {
        this.service = service;
    }

    @GetMapping
    public List<CircuitResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CircuitResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<CircuitResponse> create(
            @Valid @RequestBody CircuitRequest request,
            UriComponentsBuilder uriBuilder) {

        CircuitResponse created = service.create(request);

        URI location = uriBuilder
                .path("/api/circuits/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public CircuitResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CircuitRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}