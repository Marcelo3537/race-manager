package dev.marcelo.racemanager.race;

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
class RaceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RaceRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created race is persisted with its relations and its instant is normalised to UTC")
    void createAndRetrieveRace() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long circuitId = createCircuit("Monza");

        String location = createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Italian Grand Prix"))
                .andExpect(jsonPath("$.championshipId").value(championshipId))
                .andExpect(jsonPath("$.championshipName").value("F1 2026"))
                .andExpect(jsonPath("$.circuitId").value(circuitId))
                .andExpect(jsonPath("$.circuitName").value("Monza"))
                .andExpect(jsonPath("$.scheduledAt").value("2026-09-06T13:00:00Z"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("two races with the same round in the same championship return 409")
    void duplicateRoundReturnsConflict() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long circuitId = createCircuit("Monza");

        createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(raceBody(championshipId, circuitId, 1, "Another Grand Prix")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("the same round in a different championship is allowed")
    void sameRoundInDifferentChampionshipIsAllowed() throws Exception {
        Long firstChampionship = createChampionship("F1 2026", 2026);
        Long secondChampionship = createChampionship("GT World", 2026);
        Long circuitId = createCircuit("Monza");

        createRace(firstChampionship, circuitId, 1, "Italian Grand Prix");
        createRace(secondChampionship, circuitId, 1, "Monza 500");

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("creating a race for a championship that does not exist returns 404")
    void unknownChampionshipReturnsNotFound() throws Exception {
        Long circuitId = createCircuit("Monza");

        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(raceBody(9999L, circuitId, 1, "Ghost Grand Prix")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Championship with id 9999 not found"));

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("a championship with races cannot be deleted")
    void championshipWithRacesCannotBeDeleted() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long circuitId = createCircuit("Monza");
        createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        mockMvc.perform(delete("/api/championships/" + championshipId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("a circuit with races cannot be deleted")
    void circuitWithRacesCannotBeDeleted() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long circuitId = createCircuit("Monza");
        createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        mockMvc.perform(delete("/api/circuits/" + circuitId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("once its races are deleted, the championship can be deleted")
    void championshipCanBeDeletedAfterRemovingItsRaces() throws Exception {
        Long championshipId = createChampionship("F1 2026", 2026);
        Long circuitId = createCircuit("Monza");
        String raceLocation = createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        mockMvc.perform(delete(raceLocation))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/championships/" + championshipId))
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

    private Long createCircuit(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "country": "Italy",
                                  "city": "Monza",
                                  "lengthKm": 5.793
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return idFromLocation(result.getResponse().getHeader("Location"));
    }

    private String createRace(Long championshipId, Long circuitId, int round, String name)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(raceBody(championshipId, circuitId, round, name)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getHeader("Location");
    }

    private String raceBody(Long championshipId, Long circuitId, int round, String name) {
        return """
                {
                  "championshipId": %d,
                  "circuitId": %d,
                  "name": "%s",
                  "round": %d,
                  "scheduledAt": "2026-09-06T15:00:00+02:00",
                  "status": "SCHEDULED"
                }
                """.formatted(championshipId, circuitId, name, round);
    }

    private Long idFromLocation(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}