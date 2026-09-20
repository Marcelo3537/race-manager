package dev.marcelo.racemanager.team;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Transactional
class TeamIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TeamRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created team is persisted and can be retrieved")
    void createAndRetrieveTeam() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercedes"))
                .andExpect(jsonPath("$.constructorName").value("Mercedes-AMG Petronas"))
                .andExpect(jsonPath("$.country").value("Germany"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("creating the same team name twice returns 409")
    void createDuplicateTeamReturnsConflict() throws Exception {

        String body = """
                {
                  "name": "Mercedes",
                  "constructorName": "Mercedes-AMG Petronas",
                  "country": "Germany"
                }
                """;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("create a team with the same name, including case insensitivity, returns 409")
    void createDuplicateTeamCaseInsensitiveReturnsConflict() throws Exception {
        String body1 = """
                {
                  "name": "MERCEDES",
                  "constructorName": "Mercedes-AMG Petronas",
                  "country": "Germany"
                }
                """;

        String body2 = """
                {
                  "name": "Mercedes",
                  "constructorName": "Mercedes-AMG Petronas",
                  "country": "Germany"
                }
                """;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a deleted team is removed from the database")
    void deleteRemovesTeam() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(repository.count()).isEqualTo(1);

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                        .andExpect(status().isNotFound());

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("updating a team while keeping its original name should not return 409")
    void updateTeamWithSameNameDoesNotReturnConflict() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas Updated",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercedes"))
                .andExpect(jsonPath("$.constructorName").value("Mercedes-AMG Petronas Updated"))
                .andExpect(jsonPath("$.country").value("Germany"));


        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercedes"))
                .andExpect(jsonPath("$.constructorName").value("Mercedes-AMG Petronas Updated"))
                .andExpect(jsonPath("$.country").value("Germany"));

    }
}
