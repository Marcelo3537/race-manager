package dev.marcelo.racemanager.race;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.championship.ChampionshipService;
import dev.marcelo.racemanager.championship.ChampionshipStatus;
import dev.marcelo.racemanager.circuit.Circuit;
import dev.marcelo.racemanager.circuit.CircuitService;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.dto.RaceRequest;
import dev.marcelo.racemanager.race.dto.RaceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceServiceTest {

    private static final OffsetDateTime SCHEDULED_AT =
            OffsetDateTime.parse("2026-09-06T15:00:00+02:00");

    @Mock
    private RaceRepository repository;

    @Mock
    private ChampionshipService championshipService;

    @Mock
    private CircuitService circuitService;

    @InjectMocks
    private RaceService service;

    private Championship championship() {
        return new Championship("F1 2026", 2026, "International", ChampionshipStatus.ONGOING);
    }

    private Circuit circuit() {
        return new Circuit("Monza", "Italy", "Monza", new BigDecimal("5.793"));
    }

    private RaceRequest request() {
        return new RaceRequest(1L, 1L, "Italian Grand Prix", 1, SCHEDULED_AT, RaceStatus.SCHEDULED);
    }

    @Test
    @DisplayName("findById returns the race with its relations when it exists")
    void findByIdReturnsRace() {
        Race race = new Race(championship(), circuit(), "Italian Grand Prix", 1,
                SCHEDULED_AT, RaceStatus.SCHEDULED);
        when(repository.findByIdWithRelations(1L)).thenReturn(Optional.of(race));

        RaceResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Italian Grand Prix");
        assertThat(response.championshipName()).isEqualTo("F1 2026");
        assertThat(response.circuitName()).isEqualTo("Monza");
        assertThat(response.round()).isEqualTo(1);
        assertThat(response.scheduledAt()).isEqualTo(SCHEDULED_AT);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        when(repository.findByIdWithRelations(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("findAll returns every race mapped to a response")
    void findAllReturnsEveryRace() {
        when(repository.findAllWithRelations()).thenReturn(List.of(
                new Race(championship(), circuit(), "Italian Grand Prix", 1,
                        SCHEDULED_AT, RaceStatus.SCHEDULED),
                new Race(championship(), circuit(), "Monza Sprint", 2,
                        SCHEDULED_AT, RaceStatus.SCHEDULED)));

        List<RaceResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(RaceResponse::name)
                .containsExactly("Italian Grand Prix", "Monza Sprint");
    }

    @Test
    @DisplayName("create saves the race when the round is free")
    void createSavesRace() {
        when(championshipService.getEntityById(1L)).thenReturn(championship());
        when(circuitService.getEntityById(1L)).thenReturn(circuit());
        when(repository.existsByChampionshipIdAndRound(1L, 1)).thenReturn(false);
        when(repository.save(any(Race.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RaceResponse response = service.create(request());

        assertThat(response.name()).isEqualTo("Italian Grand Prix");
        assertThat(response.championshipName()).isEqualTo("F1 2026");
        assertThat(response.circuitName()).isEqualTo("Monza");
        verify(repository).save(any(Race.class));
    }

    @Test
    @DisplayName("create throws DuplicateResourceException and saves nothing when the round is taken")
    void createThrowsWhenRoundIsTaken() {
        when(championshipService.getEntityById(1L)).thenReturn(championship());
        when(circuitService.getEntityById(1L)).thenReturn(circuit());
        when(repository.existsByChampionshipIdAndRound(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Round 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create propagates the not found error and saves nothing when the championship does not exist")
    void createThrowsWhenChampionshipIsMissing() {
        when(championshipService.getEntityById(999L))
                .thenThrow(new ResourceNotFoundException("Championship", 999L));

        RaceRequest request = new RaceRequest(999L, 1L, "Ghost Grand Prix", 1,
                SCHEDULED_AT, RaceStatus.SCHEDULED);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Championship")
                .hasMessageContaining("999");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update modifies the race when it exists and the round is free")
    void updateModifiesRace() {
        Race existingRace = new Race(championship(), circuit(), "Old name", 5,
                OffsetDateTime.parse("2025-01-01T10:00:00Z"), RaceStatus.FINISHED);
        when(repository.findById(1L)).thenReturn(Optional.of(existingRace));
        when(championshipService.getEntityById(1L)).thenReturn(championship());
        when(circuitService.getEntityById(1L)).thenReturn(circuit());
        when(repository.existsByChampionshipIdAndRoundAndIdNot(1L, 1, 1L)).thenReturn(false);

        RaceResponse response = service.update(1L, request());

        assertThat(response.name()).isEqualTo("Italian Grand Prix");
        assertThat(response.round()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(RaceStatus.SCHEDULED);

        assertThat(existingRace.getName()).isEqualTo("Italian Grand Prix");
        assertThat(existingRace.getRound()).isEqualTo(1);
        assertThat(existingRace.getScheduledAt()).isEqualTo(SCHEDULED_AT);
        assertThat(existingRace.getStatus()).isEqualTo(RaceStatus.SCHEDULED);

        // update relies on dirty checking, so save() is never called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the race does not exist")
    void updateThrowsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never())
                .existsByChampionshipIdAndRoundAndIdNot(anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when another race has that round")
    void updateThrowsWhenRoundIsTaken() {
        Race existingRace = new Race(championship(), circuit(), "Old name", 5,
                SCHEDULED_AT, RaceStatus.SCHEDULED);
        when(repository.findById(1L)).thenReturn(Optional.of(existingRace));
        when(championshipService.getEntityById(1L)).thenReturn(championship());
        when(circuitService.getEntityById(1L)).thenReturn(circuit());
        when(repository.existsByChampionshipIdAndRoundAndIdNot(1L, 1, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request()))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(existingRace.getName()).isEqualTo("Old name");
    }

    @Test
    @DisplayName("delete removes the race when it exists")
    void deleteRemovesRace() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException and deletes nothing when the race does not exist")
    void deleteThrowsWhenMissing() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).deleteById(anyLong());
    }
}