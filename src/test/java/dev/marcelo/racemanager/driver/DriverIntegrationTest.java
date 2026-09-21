package dev.marcelo.racemanager.driver;

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
class DriverIntegrationTest {

    private static final String ALONSO = """
            {
              "name": "Fernando",
              "surname": "Alonso",
              "nationality": "Spanish",
              "dateOfBirth": "1981-07-29"
            }
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DriverRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("a created driver is persisted and its date of birth survives the round trip")
    void createAndRetrieveDriver() throws Exception {
        String location = createDriver(ALONSO);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Alonso"))
                .andExpect(jsonPath("$.dateOfBirth").value("1981-07-29"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("two drivers with the same name are allowed")
    void driversWithSameNameAreAllowed() throws Exception {
        createDriver(ALONSO);
        createDriver(ALONSO);

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("an updated driver is persisted")
    void updatePersistsChanges() throws Exception {
        String location = createDriver(ALONSO);

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "Alonso Díaz",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "1981-07-29"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Alonso Díaz"));
    }

    @Test
    @DisplayName("a deleted driver is removed from the database")
    void deleteRemovesDriver() throws Exception {
        String location = createDriver(ALONSO);
        assertThat(repository.count()).isEqualTo(1);

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound());

        assertThat(repository.count()).isZero();
    }

    private String createDriver(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getHeader("Location");
    }
}