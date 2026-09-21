package dev.marcelo.racemanager.circuit;

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
class CircuitIntegrationTest {

    private static final String MONZA = """
            {
              "name": "Monza",
              "country": "Italy",
              "city": "Monza",
              "lengthKm": 5.793
            }
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CircuitRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created circuit is persisted and its length survives the round trip exactly")
    void createAndRetrieveCircuit() throws Exception {
        String location = createCircuit(MONZA);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monza"))
                .andExpect(jsonPath("$.lengthKm").value(5.793));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().getFirst().getLengthKm())
                .isEqualByComparingTo("5.793");
    }

    @Test
    @DisplayName("creating the same circuit name twice returns 409")
    void duplicateCircuitReturnsConflict() throws Exception {
        createCircuit(MONZA);

        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONZA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate detection ignores case differences in the name")
    void duplicateDetectionIgnoresCase() throws Exception {
        createCircuit(MONZA);

        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "MONZA",
                                  "country": "Italy",
                                  "lengthKm": 5.793
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("updating a circuit while keeping its own name does not return 409")
    void updateKeepingSameNameDoesNotConflict() throws Exception {
        String location = createCircuit(MONZA);

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "city": "Monza",
                                  "lengthKm": 5.794
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lengthKm").value(5.794));
    }

    @Test
    @DisplayName("a deleted circuit is removed from the database")
    void deleteRemovesCircuit() throws Exception {
        String location = createCircuit(MONZA);
        assertThat(repository.count()).isEqualTo(1);

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound());

        assertThat(repository.count()).isZero();
    }

    private String createCircuit(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getHeader("Location");
    }
}