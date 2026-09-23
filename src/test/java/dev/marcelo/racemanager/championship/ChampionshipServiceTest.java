package dev.marcelo.racemanager.championship;

import dev.marcelo.racemanager.championship.dto.ChampionshipRequest;
import dev.marcelo.racemanager.championship.dto.ChampionshipResponse;
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
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChampionshipServiceTest {

    @Mock
    private ChampionshipRepository repository;

    @Mock
    private RaceUsageService raceUsageService;

    @InjectMocks
    private ChampionshipService service;

    @Test
    @DisplayName("findById returns the championship when it exists")
    void findByIdReturnsChampionship() {
        Championship championship =
                new Championship("Formula 1", 2026, "International", ChampionshipStatus.ONGOING);
        when(repository.findById(1L)).thenReturn(Optional.of(championship));

        ChampionshipResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Formula 1");
        assertThat(response.season()).isEqualTo(2026);
        assertThat(response.status()).isEqualTo(ChampionshipStatus.ONGOING);
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
    @DisplayName("create saves the championship when there is no duplicate")
    void createSavesChampionship() {
        ChampionshipRequest request =
                new ChampionshipRequest("Formula 1", 2026, "International", ChampionshipStatus.UPCOMING);
        when(repository.existsByNameIgnoreCaseAndSeason("Formula 1", 2026)).thenReturn(false);
        when(repository.save(any(Championship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChampionshipResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Formula 1");
        verify(repository).save(any(Championship.class));
    }

    @Test
    @DisplayName("create throws DuplicateResourceException and saves nothing when the name and season already exist")
    void createThrowsWhenDuplicate() {
        ChampionshipRequest request =
                new ChampionshipRequest("Formula 1", 2026, "International", ChampionshipStatus.UPCOMING);
        when(repository.existsByNameIgnoreCaseAndSeason("Formula 1", 2026)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Formula 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes the championship when it exists")
    void deleteRemovesChampionship() {
        when(repository.existsById(1L)).thenReturn(true);
        when(raceUsageService.existsForChampionship(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException and deletes nothing when the id does not exist")
    void deleteThrowsWhenMissing() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("findAll returns every championship mapped to a response")
    void findAllReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(
                new Championship("Formula 1", 2026, "International", ChampionshipStatus.ONGOING),
                new Championship("GT World", 2026, "International", ChampionshipStatus.UPCOMING)));

        List<ChampionshipResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ChampionshipResponse::name)
                .containsExactly("Formula 1", "GT World");
    }

    @Test
    @DisplayName("update modifies the championship when it exists and there is no duplicate")
    void updateModifiesChampionship() {
        ChampionshipRequest request =
                new ChampionshipRequest("Formula 1", 2026, "International", ChampionshipStatus.ONGOING);
        Championship existingChampionship =
                new Championship("Formula 1", 2025, "International", ChampionshipStatus.UPCOMING);
        when(repository.findById(1L)).thenReturn(Optional.of(existingChampionship));
        when(repository.existsByNameIgnoreCaseAndSeasonAndIdNot("Formula 1", 2026, 1L)).thenReturn(false);

        ChampionshipResponse response = service.update(1L, request);

        assertThat(response.season()).isEqualTo(2026);
        assertThat(response.status()).isEqualTo(ChampionshipStatus.ONGOING);

        assertThat(existingChampionship.getSeason()).isEqualTo(2026);
        assertThat(existingChampionship.getStatus()).isEqualTo(ChampionshipStatus.ONGOING);

        // update relies on dirty checking, so save() is never called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the championship does not exist")
    void updateThrowsWhenMissing() {
        ChampionshipRequest request =
                new ChampionshipRequest("Formula 1", 2026, "International", ChampionshipStatus.ONGOING);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).existsByNameIgnoreCaseAndSeasonAndIdNot(anyString(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when the name and season already exist for another championship")
    void updateThrowsWhenDuplicate() {
        ChampionshipRequest request =
                new ChampionshipRequest("Formula 1", 2026, "International", ChampionshipStatus.ONGOING);
        Championship existingChampionship =
                new Championship("Formula 1", 2025, "International", ChampionshipStatus.UPCOMING);
        when(repository.findById(1L)).thenReturn(Optional.of(existingChampionship));
        when(repository.existsByNameIgnoreCaseAndSeasonAndIdNot("Formula 1", 2026, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Formula 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete throws ResourceInUseException when the championship has races")
    void deleteThrowsWhenInUse() {
        when(repository.existsById(1L)).thenReturn(true);
        when(raceUsageService.existsForChampionship(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("races");

        verify(repository, never()).deleteById(anyLong());
    }
}