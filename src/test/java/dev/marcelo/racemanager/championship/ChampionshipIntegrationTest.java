package dev.marcelo.racemanager.championship;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Transactional
class ChampionshipIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChampionshipRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created championship is persisted and can be retrieved")
    void createAndRetrieveChampionship() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Formula 1",
                                  "season": 2026,
                                  "country": "International",
                                  "status": "UPCOMING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Formula 1"))
                .andExpect(jsonPath("$.season").value(2026));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("creating the same name and season twice returns 409")
    void duplicateChampionshipReturnsConflict() throws Exception {
        String body = """
                {
                  "name": "Formula 1",
                  "season": 2026,
                  "country": "International",
                  "status": "UPCOMING"
                }
                """;

        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("the unique constraint ignores case differences in the name")
    void duplicateDetectionIgnoresCase() throws Exception {
        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Formula 1", "season": 2026, "status": "UPCOMING"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "FORMULA 1", "season": 2026, "status": "UPCOMING"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("the same name in a different season is allowed")
    void sameNameDifferentSeasonIsAllowed() throws Exception {
        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Formula 1", "season": 2025, "status": "FINISHED"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Formula 1", "season": 2026, "status": "UPCOMING"}
                                """))
                .andExpect(status().isCreated());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("a deleted championship is removed from the database")
    void deleteRemovesChampionship() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Formula 1",
                                  "season": 2026,
                                  "country": "International",
                                  "status": "UPCOMING"
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
}