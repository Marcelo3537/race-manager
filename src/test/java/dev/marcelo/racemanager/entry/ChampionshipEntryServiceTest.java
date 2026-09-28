package dev.marcelo.racemanager.entry;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.championship.ChampionshipService;
import dev.marcelo.racemanager.championship.ChampionshipStatus;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.Driver;
import dev.marcelo.racemanager.driver.DriverService;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryRequest;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryResponse;
import dev.marcelo.racemanager.team.Team;
import dev.marcelo.racemanager.team.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChampionshipEntryServiceTest {

    @Mock
    private ChampionshipEntryRepository repository;

    @Mock
    private ChampionshipService championshipService;

    @Mock
    private DriverService driverService;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private ChampionshipEntryService service;

    private Championship championship() {
        return new Championship("F1 2026", 2026, "International", ChampionshipStatus.ONGOING);
    }

    private Driver driver() {
        return new Driver("Fernando", "Alonso", "Spanish", LocalDate.of(1981, 7, 29));
    }

    private Team team() {
        return new Team("Ferrari", "Scuderia Ferrari", "Italy");
    }

    private ChampionshipEntry entry() {
        return new ChampionshipEntry(championship(), driver(), team(), 14);
    }

    private ChampionshipEntryRequest request() {
        return new ChampionshipEntryRequest(1L, 1L, 14);
    }

    @Test
    @DisplayName("findAllByChampionship returns every entry with its related names")
    void findAllReturnsEveryEntry() {
        when(championshipService.getEntityById(8L)).thenReturn(championship());
        when(repository.findAllByChampionshipIdWithRelations(8L))
                .thenReturn(List.of(entry()));

        List<ChampionshipEntryResponse> responses = service.findAllByChampionship(8L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().driverSurname()).isEqualTo("Alonso");
        assertThat(responses.getFirst().teamName()).isEqualTo("Ferrari");
        assertThat(responses.getFirst().carNumber()).isEqualTo(14);
    }

    @Test
    @DisplayName("findAllByChampionship throws ResourceNotFoundException when the championship does not exist")
    void findAllThrowsWhenChampionshipIsMissing() {
        when(championshipService.getEntityById(9999L))
                .thenThrow(new ResourceNotFoundException("Championship", 9999L));

        assertThatThrownBy(() -> service.findAllByChampionship(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Championship");

        verify(repository, never()).findAllByChampionshipIdWithRelations(any());
    }

    @Test
    @DisplayName("findById returns the entry when it belongs to the championship")
    void findByIdReturnsEntry() {
        when(repository.findByIdAndChampionshipIdWithRelations(1L, 8L))
                .thenReturn(Optional.of(entry()));

        ChampionshipEntryResponse response = service.findById(8L, 1L);

        assertThat(response.driverName()).isEqualTo("Fernando");
        assertThat(response.championshipName()).isEqualTo("F1 2026");
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the entry belongs to another championship")
    void findByIdThrowsWhenEntryBelongsToAnotherChampionship() {
        when(repository.findByIdAndChampionshipIdWithRelations(1L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(3L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("create saves the entry when the driver and the car number are free")
    void createSavesEntry() {
        when(championshipService.getEntityById(8L)).thenReturn(championship());
        when(driverService.getEntityById(1L)).thenReturn(driver());
        when(teamService.getEntityById(1L)).thenReturn(team());
        when(repository.existsByChampionshipIdAndDriverId(8L, 1L)).thenReturn(false);
        when(repository.existsByChampionshipIdAndCarNumber(8L, 14)).thenReturn(false);
        when(repository.save(any(ChampionshipEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChampionshipEntryResponse response = service.create(8L, request());

        assertThat(response.driverSurname()).isEqualTo("Alonso");
        assertThat(response.carNumber()).isEqualTo(14);
        verify(repository).save(any(ChampionshipEntry.class));
    }

    @Test
    @DisplayName("create throws DuplicateResourceException when the driver is already entered")
    void createThrowsWhenDriverAlreadyEntered() {
        when(championshipService.getEntityById(8L)).thenReturn(championship());
        when(driverService.getEntityById(1L)).thenReturn(driver());
        when(teamService.getEntityById(1L)).thenReturn(team());
        when(repository.existsByChampionshipIdAndDriverId(8L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(8L, request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Driver");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create throws DuplicateResourceException when the car number is taken")
    void createThrowsWhenCarNumberIsTaken() {
        when(championshipService.getEntityById(8L)).thenReturn(championship());
        when(driverService.getEntityById(1L)).thenReturn(driver());
        when(teamService.getEntityById(1L)).thenReturn(team());
        when(repository.existsByChampionshipIdAndDriverId(8L, 1L)).thenReturn(false);
        when(repository.existsByChampionshipIdAndCarNumber(8L, 14)).thenReturn(true);

        assertThatThrownBy(() -> service.create(8L, request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Car number");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create propagates the not found error when the driver does not exist")
    void createThrowsWhenDriverIsMissing() {
        when(championshipService.getEntityById(8L)).thenReturn(championship());
        when(driverService.getEntityById(999L))
                .thenThrow(new ResourceNotFoundException("Driver", 999L));

        ChampionshipEntryRequest request = new ChampionshipEntryRequest(999L, 1L, 14);

        assertThatThrownBy(() -> service.create(8L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update modifies the entry when it exists and there is no conflict")
    void updateModifiesEntry() {
        ChampionshipEntry existingEntry =
                new ChampionshipEntry(championship(), driver(), team(), 55);
        Team newTeam = new Team("McLaren", "McLaren F1 Team", "United Kingdom");

        when(repository.findByIdAndChampionshipId(1L, 8L)).thenReturn(Optional.of(existingEntry));
        when(driverService.getEntityById(1L)).thenReturn(driver());
        when(teamService.getEntityById(2L)).thenReturn(newTeam);
        when(repository.existsByChampionshipIdAndDriverIdAndIdNot(8L, 1L, 1L)).thenReturn(false);
        when(repository.existsByChampionshipIdAndCarNumberAndIdNot(8L, 4, 1L)).thenReturn(false);

        ChampionshipEntryRequest request = new ChampionshipEntryRequest(1L, 2L, 4);
        ChampionshipEntryResponse response = service.update(8L, 1L, request);

        assertThat(response.teamName()).isEqualTo("McLaren");
        assertThat(response.carNumber()).isEqualTo(4);

        assertThat(existingEntry.getTeam().getName()).isEqualTo("McLaren");
        assertThat(existingEntry.getCarNumber()).isEqualTo(4);

        // update relies on dirty checking, so save() is never called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the entry does not belong to the championship")
    void updateThrowsWhenMissing() {
        when(repository.findByIdAndChampionshipId(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(3L, 1L, request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(driverService, never()).getEntityById(any());
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when the car number belongs to another entry")
    void updateThrowsWhenCarNumberIsTaken() {
        ChampionshipEntry existingEntry = entry();

        when(repository.findByIdAndChampionshipId(1L, 8L)).thenReturn(Optional.of(existingEntry));
        when(driverService.getEntityById(1L)).thenReturn(driver());
        when(teamService.getEntityById(1L)).thenReturn(team());
        when(repository.existsByChampionshipIdAndDriverIdAndIdNot(8L, 1L, 1L)).thenReturn(false);
        when(repository.existsByChampionshipIdAndCarNumberAndIdNot(8L, 14, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(8L, 1L, request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Car number");

        assertThat(existingEntry.getCarNumber()).isEqualTo(14);
    }

    @Test
    @DisplayName("delete removes the entry when it belongs to the championship")
    void deleteRemovesEntry() {
        ChampionshipEntry existingEntry = entry();
        when(repository.findByIdAndChampionshipId(1L, 8L)).thenReturn(Optional.of(existingEntry));

        service.delete(8L, 1L);

        verify(repository).delete(existingEntry);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException when the entry belongs to another championship")
    void deleteThrowsWhenMissing() {
        when(repository.findByIdAndChampionshipId(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
    }
}