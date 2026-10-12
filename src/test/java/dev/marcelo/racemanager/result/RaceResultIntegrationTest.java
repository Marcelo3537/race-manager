package dev.marcelo.racemanager.result;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Transactional
class RaceResultIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RaceResultRepository repository;

    private MockMvc mockMvc;

    private Long championshipId;
    private Long raceId;
    private Long firstEntryId;
    private Long secondEntryId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        championshipId = createChampionship("F1 2026");
        Long circuitId = createCircuit("Monza");
        raceId = createRace(championshipId, circuitId, 1, "Italian Grand Prix");

        firstEntryId = createEntry(championshipId, createDriver("Alonso"), createTeam("Ferrari"), 14);
        secondEntryId = createEntry(championshipId, createDriver("Norris"), createTeam("McLaren"), 4);
    }

    @Test
    @DisplayName("registering results persists them with the driver and team of each entry")
    void registerResultsPersistsThem() throws Exception {
        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bothFinished()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].driverSurname").value("Alonso"))
                .andExpect(jsonPath("$[0].teamName").value("Ferrari"))
                .andExpect(jsonPath("$[0].carNumber").value(14))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].points").value(0));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("registering results marks the race as finished")
    void registerResultsFinishesTheRace() throws Exception {
        mockMvc.perform(get("/api/races/" + raceId))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bothFinished()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/races/" + raceId))
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    @DisplayName("registering twice returns 422 and keeps the original results")
    void registeringTwiceReturnsUnprocessable() throws Exception {
        registerResults();

        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bothFinished()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violated"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("an incomplete block returns 422 and saves nothing")
    void incompleteBlockSavesNothing() throws Exception {
        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    { "entryId": %d, "position": 1, "lapsCompleted": 53,
                                      "fastestLap": false, "status": "FINISHED" }
                                  ]
                                }
                                """.formatted(firstEntryId)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("several retirements in the same race are allowed")
    void severalRetirementsAreAllowed() throws Exception {
        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    { "entryId": %d, "position": null, "lapsCompleted": 12,
                                      "fastestLap": false, "status": "DNF" },
                                    { "entryId": %d, "position": null, "lapsCompleted": 0,
                                      "fastestLap": false, "status": "DNS" }
                                  ]
                                }
                                """.formatted(firstEntryId, secondEntryId)))
                .andExpect(status().isCreated());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("an entry from another championship returns 422")
    void entryFromAnotherChampionshipReturnsUnprocessable() throws Exception {
        Long otherChampionship = createChampionship("GT World");
        Long foreignEntry = createEntry(otherChampionship,
                createDriver("Hamilton"), createTeam("Alpine"), 44);

        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    { "entryId": %d, "position": 1, "lapsCompleted": 53,
                                      "fastestLap": false, "status": "FINISHED" },
                                    { "entryId": %d, "position": 2, "lapsCompleted": 53,
                                      "fastestLap": false, "status": "FINISHED" }
                                  ]
                                }
                                """.formatted(firstEntryId, foreignEntry)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value("Entry %d is not registered in this championship"
                                .formatted(foreignEntry)));

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("replacing the results swaps them without leaving duplicates")
    void replaceResultsSwapsThem() throws Exception {
        registerResults();

        mockMvc.perform(put("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    { "entryId": %d, "position": 2, "lapsCompleted": 53,
                                      "fastestLap": false, "status": "FINISHED" },
                                    { "entryId": %d, "position": 1, "lapsCompleted": 53,
                                      "fastestLap": true, "status": "FINISHED" }
                                  ]
                                }
                                """.formatted(firstEntryId, secondEntryId)))
                .andExpect(status().isOk());

        assertThat(repository.count()).isEqualTo(2);

        mockMvc.perform(get("/api/races/" + raceId + "/results"))
                .andExpect(jsonPath("$[0].driverSurname").value("Norris"))
                .andExpect(jsonPath("$[0].position").value(1));
    }

    @Test
    @DisplayName("deleting the results sets the race back to scheduled")
    void deleteResultsResetsTheRace() throws Exception {
        registerResults();

        mockMvc.perform(delete("/api/races/" + raceId + "/results"))
                .andExpect(status().isNoContent());

        assertThat(repository.count()).isZero();

        mockMvc.perform(get("/api/races/" + raceId))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("a race with results cannot be deleted")
    void raceWithResultsCannotBeDeleted() throws Exception {
        registerResults();

        mockMvc.perform(delete("/api/races/" + raceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("an entry with results cannot be deleted")
    void entryWithResultsCannotBeDeleted() throws Exception {
        registerResults();

        mockMvc.perform(delete("/api/championships/" + championshipId + "/entries/" + firstEntryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    @Test
    @DisplayName("the results are ordered by position with retirements last")
    void resultsAreOrderedByPosition() throws Exception {
        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    { "entryId": %d, "position": null, "lapsCompleted": 12,
                                      "fastestLap": false, "status": "DNF" },
                                    { "entryId": %d, "position": 1, "lapsCompleted": 53,
                                      "fastestLap": false, "status": "FINISHED" }
                                  ]
                                }
                                """.formatted(firstEntryId, secondEntryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/races/" + raceId + "/results"))
                .andExpect(jsonPath("$[0].driverSurname").value("Norris"))
                .andExpect(jsonPath("$[1].driverSurname").value("Alonso"))
                .andExpect(jsonPath("$[1].position").doesNotExist());
    }

    private void registerResults() throws Exception {
        mockMvc.perform(post("/api/races/" + raceId + "/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bothFinished()))
                .andExpect(status().isCreated());
    }

    private String bothFinished() {
        return """
                {
                  "results": [
                    { "entryId": %d, "position": 1, "lapsCompleted": 53,
                      "fastestLap": true, "status": "FINISHED" },
                    { "entryId": %d, "position": 2, "lapsCompleted": 53,
                      "fastestLap": false, "status": "FINISHED" }
                  ]
                }
                """.formatted(firstEntryId, secondEntryId);
    }

    private Long createChampionship(String name) throws Exception {
        return idFrom(mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "season": 2026,
                                  "country": "International", "status": "UPCOMING" }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long createCircuit(String name) throws Exception {
        return idFrom(mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "country": "Italy",
                                  "city": "%s", "lengthKm": 5.793 }
                                """.formatted(name, name)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long createRace(Long championship, Long circuit, int round, String name)
            throws Exception {
        return idFrom(mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "championshipId": %d, "circuitId": %d, "name": "%s",
                                  "round": %d, "scheduledAt": "2026-09-06T15:00:00+02:00",
                                  "status": "SCHEDULED" }
                                """.formatted(championship, circuit, name, round)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long createDriver(String surname) throws Exception {
        return idFrom(mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Name", "surname": "%s",
                                  "nationality": "Spanish", "dateOfBirth": "1990-01-01" }
                                """.formatted(surname)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long createTeam(String name) throws Exception {
        return idFrom(mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "constructorName": "%s Racing",
                                  "country": "Italy" }
                                """.formatted(name, name)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long createEntry(Long championship, Long driverId, Long teamId, int carNumber)
            throws Exception {
        return idFrom(mockMvc.perform(post("/api/championships/" + championship + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "driverId": %d, "teamId": %d, "carNumber": %d }
                                """.formatted(driverId, teamId, carNumber)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private Long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}