package dev.marcelo.racemanager.result;

import dev.marcelo.racemanager.common.exception.BusinessRuleException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.result.dto.RaceResultResponse;
import dev.marcelo.racemanager.result.dto.RaceResultsRequest;
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

@WebMvcTest(RaceResultController.class)
class RaceResultControllerTest {

    private static final String VALID_BODY = """
            {
              "results": [
                {
                  "entryId": 101,
                  "position": 1,
                  "lapsCompleted": 53,
                  "fastestLap": true,
                  "status": "FINISHED"
                },
                {
                  "entryId": 102,
                  "position": 2,
                  "lapsCompleted": 53,
                  "fastestLap": false,
                  "status": "FINISHED"
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceResultService service;

    private RaceResultResponse response(Long id, Long entryId, String surname, int position) {
        return new RaceResultResponse(id, 1L, "Italian Grand Prix", entryId,
                entryId, "Name", surname, entryId, "Ferrari", 14,
                position, 0, 53, false, ResultStatus.FINISHED);
    }

    private List<RaceResultResponse> twoResults() {
        return List.of(
                response(1L, 101L, "Alonso", 1),
                response(2L, 102L, "Norris", 2));
    }

    @Test
    @DisplayName("GET /api/races/{raceId}/results returns 200 with the results")
    void findAllReturnsOk() throws Exception {
        when(service.findAllByRace(1L)).thenReturn(twoResults());

        mockMvc.perform(get("/api/races/1/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].driverSurname").value("Alonso"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].points").value(0))
                .andExpect(jsonPath("$[1].driverSurname").value("Norris"));
    }

    @Test
    @DisplayName("GET returns 404 when the race does not exist")
    void findAllReturnsNotFound() throws Exception {
        when(service.findAllByRace(999L))
                .thenThrow(new ResourceNotFoundException("Race", 999L));

        mockMvc.perform(get("/api/races/999/results"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Race with id 999 not found"));
    }

    @Test
    @DisplayName("POST returns 201 with the Location header and the created results")
    void registerReturnsCreated() throws Exception {
        when(service.registerResults(eq(1L), any(RaceResultsRequest.class)))
                .thenReturn(twoResults());

        mockMvc.perform(post("/api/races/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/races/1/results"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].raceName").value("Italian Grand Prix"));
    }

    @Test
    @DisplayName("POST returns 422 when a business rule is violated")
    void registerReturnsUnprocessableEntity() throws Exception {
        when(service.registerResults(eq(1L), any(RaceResultsRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Position 1 is assigned to more than one entry"));

        mockMvc.perform(post("/api/races/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Business rule violated"))
                .andExpect(jsonPath("$.detail")
                        .value("Position 1 is assigned to more than one entry"));
    }

    @Test
    @DisplayName("POST returns 400 and does not reach the service when the list is empty")
    void registerReturnsBadRequestWhenListIsEmpty() throws Exception {
        mockMvc.perform(post("/api/races/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "results": [] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.results").value("At least one result is required"));

        verify(service, never()).registerResults(any(), any());
    }

    @Test
    @DisplayName("POST returns 400 when an element of the list is invalid")
    void registerReturnsBadRequestWhenAnElementIsInvalid() throws Exception {
        mockMvc.perform(post("/api/races/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "results": [
                                    {
                                      "position": 1,
                                      "lapsCompleted": 53,
                                      "fastestLap": false,
                                      "status": "FINISHED"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['results[0].entryId']")
                        .value("Entry id is required"));

        verify(service, never()).registerResults(any(), any());
    }

    @Test
    @DisplayName("PUT returns 200 with the replaced results")
    void replaceReturnsOk() throws Exception {
        when(service.replaceResults(eq(1L), any(RaceResultsRequest.class)))
                .thenReturn(twoResults());

        mockMvc.perform(put("/api/races/1/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverSurname").value("Alonso"));

        verify(service).replaceResults(eq(1L), any(RaceResultsRequest.class));
    }

    @Test
    @DisplayName("DELETE returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/races/1/results"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deleteResults(1L);
    }

    @Test
    @DisplayName("DELETE returns 404 when the race has no results")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Race result", 1L))
                .when(service).deleteResults(1L);

        mockMvc.perform(delete("/api/races/1/results"))
                .andExpect(status().isNotFound());
    }
}