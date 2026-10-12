package dev.marcelo.racemanager.result;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.championship.ChampionshipStatus;
import dev.marcelo.racemanager.common.exception.BusinessRuleException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.Driver;
import dev.marcelo.racemanager.entry.ChampionshipEntry;
import dev.marcelo.racemanager.entry.ChampionshipEntryService;
import dev.marcelo.racemanager.race.Race;
import dev.marcelo.racemanager.race.RaceService;
import dev.marcelo.racemanager.race.RaceStatus;
import dev.marcelo.racemanager.result.dto.RaceResultRequest;
import dev.marcelo.racemanager.result.dto.RaceResultResponse;
import dev.marcelo.racemanager.result.dto.RaceResultsRequest;
import dev.marcelo.racemanager.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceResultServiceTest {

    @Mock
    private RaceResultRepository repository;

    @Mock
    private RaceService raceService;

    @Mock
    private ChampionshipEntryService entryService;

    @InjectMocks
    private RaceResultService service;

    private Race race;
    private ChampionshipEntry entry1;
    private ChampionshipEntry entry2;

    @BeforeEach
    void setUp() {
        Championship championship =
                new Championship("F1 2026", 2026, "International", ChampionshipStatus.ONGOING);
        setId(championship, 8L);

        race = new Race(championship, null, "Italian Grand Prix", 1,
                OffsetDateTime.parse("2026-09-06T15:00:00+02:00"), RaceStatus.SCHEDULED);
        setId(race, 1L);

        entry1 = entry(championship, "Alonso", "Ferrari", 14, 101L);
        entry2 = entry(championship, "Norris", "McLaren", 4, 102L);
    }

    private ChampionshipEntry entry(Championship championship, String surname,
                                    String teamName, int carNumber, Long id) {
        Driver driver = new Driver("Name", surname, "Spanish", LocalDate.of(1990, 1, 1));
        setId(driver, id);
        Team team = new Team(teamName, teamName + " Racing", "Italy");
        setId(team, id);

        ChampionshipEntry entry = new ChampionshipEntry(championship, driver, team, carNumber);
        setId(entry, id);
        return entry;
    }

    /** Asigna el id por reflexión: las entidades no exponen setId porque lo asigna la base de datos. */
    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private RaceResultsRequest validResults() {
        return new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, true, ResultStatus.FINISHED),
                new RaceResultRequest(102L, 2, 53, false, ResultStatus.FINISHED)));
    }

    private void givenEntries() {
        when(entryService.getEntitiesByChampionship(8L)).thenReturn(List.of(entry1, entry2));
    }

    @Test
    @DisplayName("registerResults saves every result and marks the race as finished")
    void registerResultsSavesAndFinishesRace() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<RaceResultResponse> responses = service.registerResults(1L, validResults());

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().driverSurname()).isEqualTo("Alonso");
        assertThat(responses.getFirst().teamName()).isEqualTo("Ferrari");
        assertThat(responses.getFirst().carNumber()).isEqualTo(14);
        assertThat(responses.getFirst().points()).isZero();

        assertThat(race.getStatus()).isEqualTo(RaceStatus.FINISHED);
    }

    @Test
    @DisplayName("registerResults throws when the race already has results")
    void registerResultsThrowsWhenAlreadyRegistered() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registerResults(1L, validResults()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has results");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registerResults propagates the not found error when the race does not exist")
    void registerResultsThrowsWhenRaceIsMissing() {
        when(raceService.getEntityById(999L))
                .thenThrow(new ResourceNotFoundException("Race", 999L));

        assertThatThrownBy(() -> service.registerResults(999L, validResults()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("results must include every entry of the championship")
    void throwsWhenAnEntryIsMissing() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest incomplete = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, false, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, incomplete))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("every entry");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("an entry cannot appear twice in the results")
    void throwsWhenAnEntryIsRepeated() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest repeated = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, false, ResultStatus.FINISHED),
                new RaceResultRequest(101L, 2, 53, false, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, repeated))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("more than once");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("an entry from another championship cannot be in the results")
    void throwsWhenAnEntryIsFromAnotherChampionship() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest foreign = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, false, ResultStatus.FINISHED),
                new RaceResultRequest(999L, 2, 53, false, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, foreign))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not registered in this championship");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("two entries cannot share the same position")
    void throwsWhenTwoEntriesSharePosition() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest samePosition = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, false, ResultStatus.FINISHED),
                new RaceResultRequest(102L, 1, 53, false, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, samePosition))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("more than one entry");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("a finished entry must have a position")
    void throwsWhenFinishedHasNoPosition() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest noPosition = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, null, 53, false, ResultStatus.FINISHED),
                new RaceResultRequest(102L, 2, 53, false, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, noPosition))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must have a position");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("an entry that did not finish must not have a position")
    void throwsWhenNotFinishedHasPosition() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest dnfWithPosition = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, false, ResultStatus.FINISHED),
                new RaceResultRequest(102L, 2, 12, false, ResultStatus.DNF)));

        assertThatThrownBy(() -> service.registerResults(1L, dnfWithPosition))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must not have a position");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("several entries can retire in the same race")
    void allowsSeveralRetirements() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        RaceResultsRequest twoRetirements = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, null, 12, false, ResultStatus.DNF),
                new RaceResultRequest(102L, null, 0, false, ResultStatus.DNS)));

        List<RaceResultResponse> responses = service.registerResults(1L, twoRetirements);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.position() == null);
    }

    @Test
    @DisplayName("only one entry can have the fastest lap")
    void throwsWhenTwoFastestLaps() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);
        givenEntries();

        RaceResultsRequest twoFastestLaps = new RaceResultsRequest(List.of(
                new RaceResultRequest(101L, 1, 53, true, ResultStatus.FINISHED),
                new RaceResultRequest(102L, 2, 53, true, ResultStatus.FINISHED)));

        assertThatThrownBy(() -> service.registerResults(1L, twoFastestLaps))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fastest lap");

        verify(repository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("replaceResults deletes the previous results before saving the new ones")
    void replaceResultsDeletesFirst() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        givenEntries();
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        service.replaceResults(1L, validResults());

        verify(repository).deleteAllByRaceId(1L);
        verify(repository).flush();
        verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("deleteResults removes them and sets the race back to scheduled")
    void deleteResultsResetsRaceStatus() {
        race.setStatus(RaceStatus.FINISHED);
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(true);

        service.deleteResults(1L);

        verify(repository).deleteAllByRaceId(1L);
        assertThat(race.getStatus()).isEqualTo(RaceStatus.SCHEDULED);
    }

    @Test
    @DisplayName("deleteResults throws ResourceNotFoundException when the race has no results")
    void deleteResultsThrowsWhenEmpty() {
        when(raceService.getEntityById(1L)).thenReturn(race);
        when(repository.existsByRaceId(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteResults(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteAllByRaceId(any());
    }
}