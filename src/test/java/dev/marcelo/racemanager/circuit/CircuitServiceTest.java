package dev.marcelo.racemanager.circuit;

import dev.marcelo.racemanager.circuit.dto.CircuitRequest;
import dev.marcelo.racemanager.circuit.dto.CircuitResponse;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceInUseException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.RaceUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {

    private static final BigDecimal MONZA_LENGTH = new BigDecimal("5.793");

    @Mock
    private CircuitRepository repository;

    @Mock
    private RaceUsageService raceUsageService;

    @InjectMocks
    private CircuitService service;

    @Test
    @DisplayName("findById returns the circuit when it exists")
    void findByIdReturnsCircuit() {
        Circuit circuit = new Circuit("Monza", "Italy", "Monza", MONZA_LENGTH);
        when(repository.findById(1L)).thenReturn(Optional.of(circuit));

        CircuitResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Monza");
        assertThat(response.country()).isEqualTo("Italy");
        assertThat(response.lengthKm()).isEqualByComparingTo("5.793");
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("findAll returns every circuit mapped to a response")
    void findAllReturnsEveryCircuit() {
        when(repository.findAll()).thenReturn(List.of(
                new Circuit("Monza", "Italy", "Monza", MONZA_LENGTH),
                new Circuit("Silverstone", "United Kingdom", "Silverstone", new BigDecimal("5.891"))));

        List<CircuitResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CircuitResponse::name)
                .containsExactly("Monza", "Silverstone");
    }

    @Test
    @DisplayName("create saves the circuit when there is no duplicate")
    void createSavesCircuit() {
        CircuitRequest request = new CircuitRequest("Monza", "Italy", "Monza", MONZA_LENGTH);
        when(repository.existsByNameIgnoreCase("Monza")).thenReturn(false);
        when(repository.save(any(Circuit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CircuitResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Monza");
        assertThat(response.lengthKm()).isEqualByComparingTo("5.793");
        verify(repository).save(any(Circuit.class));
    }

    @Test
    @DisplayName("create throws DuplicateResourceException and saves nothing when the name already exists")
    void createThrowsWhenDuplicate() {
        CircuitRequest request = new CircuitRequest("Monza", "Italy", "Monza", MONZA_LENGTH);
        when(repository.existsByNameIgnoreCase("Monza")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Monza");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update modifies the circuit when it exists and there is no duplicate")
    void updateModifiesCircuit() {
        CircuitRequest request = new CircuitRequest("Monza", "Italy", "Monza", MONZA_LENGTH);
        Circuit existingCircuit = new Circuit("Monza", "Italia", null, new BigDecimal("5.800"));
        when(repository.findById(1L)).thenReturn(Optional.of(existingCircuit));
        when(repository.existsByNameIgnoreCaseAndIdNot("Monza", 1L)).thenReturn(false);

        CircuitResponse response = service.update(1L, request);

        assertThat(response.country()).isEqualTo("Italy");
        assertThat(response.lengthKm()).isEqualByComparingTo("5.793");

        assertThat(existingCircuit.getCountry()).isEqualTo("Italy");
        assertThat(existingCircuit.getCity()).isEqualTo("Monza");
        assertThat(existingCircuit.getLengthKm()).isEqualByComparingTo("5.793");

        // update relies on dirty checking, so save() is never called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the circuit does not exist")
    void updateThrowsWhenMissing() {
        CircuitRequest request = new CircuitRequest("Monza", "Italy", "Monza", MONZA_LENGTH);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).existsByNameIgnoreCaseAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when another circuit has the same name")
    void updateThrowsWhenDuplicate() {
        CircuitRequest request = new CircuitRequest("Monza", "Italy", "Monza", MONZA_LENGTH);
        Circuit existingCircuit = new Circuit("Imola", "Italy", "Imola", new BigDecimal("4.909"));
        when(repository.findById(1L)).thenReturn(Optional.of(existingCircuit));
        when(repository.existsByNameIgnoreCaseAndIdNot("Monza", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Monza");

        assertThat(existingCircuit.getName()).isEqualTo("Imola");
    }

    @Test
    @DisplayName("delete removes the circuit when it exists and has no races")
    void deleteRemovesCircuit() {
        when(repository.existsById(1L)).thenReturn(true);
        when(raceUsageService.existsForCircuit(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException and deletes nothing when the circuit does not exist")
    void deleteThrowsWhenMissing() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete throws ResourceInUseException when the circuit has races")
    void deleteThrowsWhenInUse() {
        when(repository.existsById(1L)).thenReturn(true);
        when(raceUsageService.existsForCircuit(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("races");

        verify(repository, never()).deleteById(anyLong());
    }
}