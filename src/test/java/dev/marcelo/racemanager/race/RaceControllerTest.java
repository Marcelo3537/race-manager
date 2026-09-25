package dev.marcelo.racemanager.race;

import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.race.dto.RaceRequest;
import dev.marcelo.racemanager.race.dto.RaceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
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

@WebMvcTest(RaceController.class)
class RaceControllerTest {

    private static final OffsetDateTime SCHEDULED_AT =
            OffsetDateTime.parse("2026-09-06T15:00:00+02:00");

    private static final String VALID_BODY = """
            {
              "championshipId": 1,
              "circuitId": 1,
              "name": "Italian Grand Prix",
              "round": 1,
              "scheduledAt": "2026-09-06T15:00:00+02:00",
              "status": "SCHEDULED"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceService service;

    private RaceResponse response(Long id) {
        return new RaceResponse(id, 1L, "F1 2026", 1L, "Monza",
                "Italian Grand Prix", 1, SCHEDULED_AT, RaceStatus.SCHEDULED);
    }

    @Test
    @DisplayName("GET /api/races returns 200 with the list of races")
    void findAllReturnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/races"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /api/races/{id} returns 200 with the related names")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/races/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.championshipId").value(1))
                .andExpect(jsonPath("$.championshipName").value("F1 2026"))
                .andExpect(jsonPath("$.circuitName").value("Monza"))
                .andExpect(jsonPath("$.round").value(1))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("GET /api/races/{id} returns 404 when the race does not exist")
    void findByIdReturnsNotFound() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Race", 999L));

        mockMvc.perform(get("/api/races/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Race with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/races returns 201 with the Location header")
    void createReturnsCreated() throws Exception {
        when(service.create(any(RaceRequest.class))).thenReturn(response(3L));

        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/races/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.championshipName").value("F1 2026"));
    }

    @Test
    @DisplayName("POST /api/races returns 400 and does not reach the service when the championship id is missing")
    void createReturnsBadRequestWhenChampionshipIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circuitId": 1,
                                  "name": "Italian Grand Prix",
                                  "round": 1,
                                  "scheduledAt": "2026-09-06T15:00:00+02:00",
                                  "status": "SCHEDULED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.championshipId").value("Championship id is required"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/races returns 400 when the round is zero")
    void createReturnsBadRequestWhenRoundIsZero() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "championshipId": 1,
                                  "circuitId": 1,
                                  "name": "Italian Grand Prix",
                                  "round": 0,
                                  "scheduledAt": "2026-09-06T15:00:00+02:00",
                                  "status": "SCHEDULED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.round").value("Round must be greater than zero"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/races returns 400 when the date has an invalid format")
    void createReturnsBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "championshipId": 1,
                                  "circuitId": 1,
                                  "name": "Italian Grand Prix",
                                  "round": 1,
                                  "scheduledAt": "06/09/2026 15:00",
                                  "status": "SCHEDULED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/races returns 404 when the championship does not exist")
    void createReturnsNotFoundWhenChampionshipIsMissing() throws Exception {
        when(service.create(any(RaceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Championship", 999L));

        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Championship with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/races returns 409 when the round is already taken")
    void createReturnsConflictWhenRoundIsTaken() throws Exception {
        when(service.create(any(RaceRequest.class)))
                .thenThrow(new DuplicateResourceException("Round 1 already exists for championship 1"));

        mockMvc.perform(post("/api/races")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value("Round 1 already exists for championship 1"));
    }

    @Test
    @DisplayName("PUT /api/races/{id} returns 200 with the updated race")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(1L), any(RaceRequest.class))).thenReturn(response(1L));

        mockMvc.perform(put("/api/races/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Italian Grand Prix"));

        verify(service).update(eq(1L), any(RaceRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/races/{id} returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/races/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/races/{id} returns 404 when the race does not exist")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Race", 999L)).when(service).delete(eq(999L));

        mockMvc.perform(delete("/api/races/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }
}