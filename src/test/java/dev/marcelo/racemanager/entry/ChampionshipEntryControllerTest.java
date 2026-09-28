package dev.marcelo.racemanager.entry;

import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryRequest;
import dev.marcelo.racemanager.entry.dto.ChampionshipEntryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChampionshipEntryController.class)
class ChampionshipEntryControllerTest {

    private static final String VALID_BODY = """
            {
              "driverId": 1,
              "teamId": 1,
              "carNumber": 14
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChampionshipEntryService service;

    private ChampionshipEntryResponse response(Long id) {
        return new ChampionshipEntryResponse(id, 8L, "F1 2026", 1L, "Fernando", "Alonso",
                1L, "Ferrari", 14);
    }

    @Test
    @DisplayName("GET /api/championships/{id}/entries returns 200 with the list")
    void findAllReturnsOk() throws Exception {
        when(service.findAllByChampionship(8L)).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/championships/8/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].driverSurname").value("Alonso"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /api/championships/{id}/entries returns 404 when the championship does not exist")
    void findAllReturnsNotFoundWhenChampionshipIsMissing() throws Exception {
        when(service.findAllByChampionship(9999L))
                .thenThrow(new ResourceNotFoundException("Championship", 9999L));

        mockMvc.perform(get("/api/championships/9999/entries"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Championship with id 9999 not found"));
    }

    @Test
    @DisplayName("GET /api/championships/{championshipId}/entries/{id} returns 200 with the related names")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(8L, 1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/championships/8/entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.championshipName").value("F1 2026"))
                .andExpect(jsonPath("$.driverName").value("Fernando"))
                .andExpect(jsonPath("$.driverSurname").value("Alonso"))
                .andExpect(jsonPath("$.teamName").value("Ferrari"))
                .andExpect(jsonPath("$.carNumber").value(14));
    }

    @Test
    @DisplayName("GET returns 404 when the entry belongs to another championship")
    void findByIdReturnsNotFoundForAnotherChampionship() throws Exception {
        when(service.findById(3L, 1L))
                .thenThrow(new ResourceNotFoundException("Championship entry", 1L));

        mockMvc.perform(get("/api/championships/3/entries/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST returns 201 with a nested Location header")
    void createReturnsCreated() throws Exception {
        when(service.create(eq(8L), any(ChampionshipEntryRequest.class))).thenReturn(response(5L));

        mockMvc.perform(post("/api/championships/8/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/championships/8/entries/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("POST returns 400 and does not reach the service when the driver id is missing")
    void createReturnsBadRequestWhenDriverIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/championships/8/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teamId": 1,
                                  "carNumber": 14
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.driverId").value("Driver id is required"));

        verify(service, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST returns 400 when the car number is out of range")
    void createReturnsBadRequestWhenCarNumberIsOutOfRange() throws Exception {
        mockMvc.perform(post("/api/championships/8/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": 1,
                                  "teamId": 1,
                                  "carNumber": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.carNumber")
                        .value("Car number must be between 1 and 99"));

        verify(service, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST returns 409 when the car number is taken")
    void createReturnsConflict() throws Exception {
        when(service.create(eq(8L), any(ChampionshipEntryRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "Car number 14 is already taken in championship 8"));

        mockMvc.perform(post("/api/championships/8/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"));
    }

    @Test
    @DisplayName("PUT returns 200 with the updated entry")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(8L), eq(1L), any(ChampionshipEntryRequest.class)))
                .thenReturn(response(1L));

        mockMvc.perform(put("/api/championships/8/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carNumber").value(14));

        verify(service).update(eq(8L), eq(1L), any(ChampionshipEntryRequest.class));
    }

    @Test
    @DisplayName("DELETE returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/championships/8/entries/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(8L, 1L);
    }

    @Test
    @DisplayName("DELETE returns 404 when the entry belongs to another championship")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Championship entry", 1L))
                .when(service).delete(eq(3L), eq(1L));

        mockMvc.perform(delete("/api/championships/3/entries/1"))
                .andExpect(status().isNotFound());
    }
}