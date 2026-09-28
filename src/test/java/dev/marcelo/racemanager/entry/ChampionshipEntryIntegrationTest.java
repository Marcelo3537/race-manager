package dev.marcelo.racemanager.entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Transactional
class ChampionshipEntryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChampionshipEntryRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created entry is persisted with its three relations")
    void createAndRetrieveEntry() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");

        String location = createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.championshipName").value("F1 2026"))
                .andExpect(jsonPath("$.driverSurname").value("Alonso"))
                .andExpect(jsonPath("$.teamName").value("Ferrari"))
                .andExpect(jsonPath("$.carNumber").value(14));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("the same driver cannot be entered twice in the same championship")
    void duplicateDriverReturnsConflict() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");

        createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(post("/api/championships/" + championshipId + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(driverId, teamId, 55)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Driver %d is already entered in championship %d"
                                .formatted(driverId, championshipId)));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("two drivers cannot share a car number in the same championship")
    void duplicateCarNumberReturnsConflict() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long firstDriver = createDriver("Alonso");
        Long secondDriver = createDriver("Norris");
        Long teamId = createTeam("Ferrari");

        createEntry(championshipId, firstDriver, teamId, 14);

        mockMvc.perform(post("/api/championships/" + championshipId + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(secondDriver, teamId, 14)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("the same driver and car number are allowed in a different championship")
    void sameDriverInAnotherChampionshipIsAllowed() throws Exception {
        Long firstChampionship = createChampionship("F1 2026", 2026);
        Long secondChampionship = createChampionship("GT World", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");

        createEntry(firstChampionship, driverId, teamId, 14);
        createEntry(secondChampionship, driverId, teamId, 14);

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("an entry cannot be read from a different championship")
    void entryIsNotVisibleFromAnotherChampionship() throws Exception {
        Long firstChampionship = createChampionship("F1 2026", 2026);
        Long secondChampionship = createChampionship("GT World", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");

        String location = createEntry(firstChampionship, driverId, teamId, 14);
        Long entryId = idFromLocation(location);

        mockMvc.perform(get("/api/championships/" + secondChampionship + "/entries/" + entryId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listing entries of a championship that does not exist returns 404")
    void unknownChampionshipReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/championships/9999/entries"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a driver with entries cannot be deleted")
    void driverWithEntriesCannotBeDeleted() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");
        createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(delete("/api/drivers/" + driverId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("a team with entries cannot be deleted")
    void teamWithEntriesCannotBeDeleted() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");
        createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(delete("/api/teams/" + teamId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("a championship with entries cannot be deleted")
    void championshipWithEntriesCannotBeDeleted() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");
        createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(delete("/api/championships/" + championshipId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Championship %d cannot be deleted because it has entries"
                                .formatted(championshipId)));
    }

    @Test
    @DisplayName("once its entry is deleted, the driver can be deleted")
    void driverCanBeDeletedAfterRemovingItsEntry() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long driverId = createDriver("Alonso");
        Long teamId = createTeam("Ferrari");
        String location = createEntry(championshipId, driverId, teamId, 14);

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/drivers/" + driverId))
                .andExpect(status().isNoContent());

        assertThat(repository.count()).isZero();
    }

    private Long createChampionship(String name, int season) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "season": %d,
                                  "country": "International",
                                  "status": "UPCOMING"
                                }
                                """.formatted(name, season)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFromLocation(result.getResponse().getHeader("Location"));
    }

    private Long createDriver(String surname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "%s",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "1981-07-29"
                                }
                                """.formatted(surname)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFromLocation(result.getResponse().getHeader("Location"));
    }

    private Long createTeam(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "constructorName": "%s Racing",
                                  "country": "Italy"
                                }
                                """.formatted(name, name)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFromLocation(result.getResponse().getHeader("Location"));
    }

    private String createEntry(Long championshipId, Long driverId, Long teamId, int carNumber)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/championships/" + championshipId + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody(driverId, teamId, carNumber)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getHeader("Location");
    }

    private String entryBody(Long driverId, Long teamId, int carNumber) {
        return """
                {
                  "driverId": %d,
                  "teamId": %d,
                  "carNumber": %d
                }
                """.formatted(driverId, teamId, carNumber);
    }

    private Long idFromLocation(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}