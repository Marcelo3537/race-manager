package dev.marcelo.racemanager.team;

import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.team.dto.TeamRequest;
import dev.marcelo.racemanager.team.dto.TeamResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository repository;

    @InjectMocks
    private TeamService service;

    @Test
    @DisplayName("findById returns the team when it exists")
    void findByIdReturnsTeam() {
        Team team = new Team("Mercedes", "Mercedes-AMG Petronas", "Germany");
        when(repository.findById(1L)).thenReturn(Optional.of(team));

        TeamResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Mercedes");
        assertThat(response.constructorName()).isEqualTo("Mercedes-AMG Petronas");
        assertThat(response.country()).isEqualTo("Germany");
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
    @DisplayName("create saves the team when there is no duplicate")
    void createSavesTeam() {
        TeamRequest request = new TeamRequest("Mercedes", "Mercedes-AMG Petronas", "Germany");
        when(repository.existsByNameIgnoreCase("Mercedes")).thenReturn(false);
        when(repository.save(any(Team.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Mercedes");
        verify(repository).save(any(Team.class));
    }

    @Test
    @DisplayName("create throws DuplicateResourceException when the team name already exists")
    void createThrowsWhenDuplicate() {
        TeamRequest request = new TeamRequest("Mercedes", "Mercedes-AMG Petronas", "Germany");
        when(repository.existsByNameIgnoreCase("Mercedes")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Mercedes");

        verify(repository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("delete removes the team when it exists")
    void deleteRemovesTeam() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException and deletes nothing when the team does not exist")
    void deleteThrowsWhenMissing() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("findAll returns every team mapped to a response")
    void findAllReturnsEveryTeam() {
        when(repository.findAll()).thenReturn(List.of(
                new Team("Mercedes", "Mercedes-AMG Petronas", "Germany"),
                new Team("Red Bull Racing", "Red Bull Racing RBPT", "Austria")));

        List<TeamResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TeamResponse::name)
                .containsExactly("Mercedes", "Red Bull Racing");
    }

    @Test
    @DisplayName("update modifies the team when it exists and there is no duplicate")
    void updateModifiesTeam() {
        TeamRequest request = new TeamRequest("Mercedes", "Mercedes-AMG Petronas", "Germany");
        Team existingTeam = new Team("Red Bull Racing", "Red Bull Racing RBPT", "Austria");
        when(repository.findById(1L)).thenReturn(Optional.of(existingTeam));
        when(repository.existsByNameIgnoreCaseAndIdNot("Mercedes", 1L)).thenReturn(false);

        TeamResponse response = service.update(1L, request);

        assertThat(response.name()).isEqualTo("Mercedes");
        assertThat(response.constructorName()).isEqualTo("Mercedes-AMG Petronas");
        assertThat(response.country()).isEqualTo("Germany");

        assertThat(existingTeam.getName()).isEqualTo("Mercedes");
        assertThat(existingTeam.getConstructorName()).isEqualTo("Mercedes-AMG Petronas");
        assertThat(existingTeam.getCountry()).isEqualTo("Germany");

        verify(repository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the team does not exist")
    void updateThrowsWhenMissing() {
        TeamRequest request = new TeamRequest("Mercedes", "Mercedes-AMG Petronas", "Germany");
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).existsByNameIgnoreCaseAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when the team name already exists")
    void updateThrowsWhenDuplicate() {
        TeamRequest request = new TeamRequest("Mercedes", "Mercedes-AMG Petronas", "Germany");
        Team existingTeam = new Team("Mercedes", "Mercedes-AMG", "Austria");
        when(repository.findById(1L)).thenReturn(Optional.of(existingTeam));
        when(repository.existsByNameIgnoreCaseAndIdNot("Mercedes", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Mercedes");

        verify(repository, never()).save(any());
    }
}





























