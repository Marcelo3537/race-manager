package dev.marcelo.racemanager.team;

import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.team.dto.TeamRequest;
import dev.marcelo.racemanager.team.dto.TeamResponse;
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

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService service;

    @Test
    @DisplayName("GET /api/teams returns 200 with the list of teams")
    void findAllReturnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new TeamResponse(1L, "Mercedes", "Mercedes-AMG Petronas", "Germany"),
                new TeamResponse(2L, "Red Bull Racing", "Red Bull Racing Honda", "Austria")));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Mercedes"))
                .andExpect(jsonPath("$[1].name").value("Red Bull Racing"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} returns 200 with the team")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(1L)).thenReturn(
                new TeamResponse(1L, "Mercedes", "Mercedes-AMG Petronas", "Germany"));

        mockMvc.perform(get("/api/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mercedes"))
                .andExpect(jsonPath("$.constructorName").value("Mercedes-AMG Petronas"))
                .andExpect(jsonPath("$.country").value("Germany"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} returns 404 when the team does not exist")
    void findByIdReturnsNotFound() throws Exception {
        when(service.findById(999L))
                .thenThrow(new ResourceNotFoundException("Team", 999L));

        mockMvc.perform(get("/api/teams/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Team with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/teams returns 201 with the created team")
    void createReturnsCreated() throws Exception {
        when(service.create(any(TeamRequest.class))).thenReturn(
                new TeamResponse(3L, "Ferrari", "Scuderia Ferrari", "Italy"));

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ferrari",
                                  "constructorName": "Scuderia Ferrari",
                                  "country": "Italy"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/teams/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Ferrari"))
                .andExpect(jsonPath("$.constructorName").value("Scuderia Ferrari"))
                .andExpect(jsonPath("$.country").value("Italy"));
    }

    @Test
    @DisplayName("POST /api/teams returns 400 and does not reach the service when the name is missing")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "constructorName": "Scuderia Ferrari",
                                    "country": "Italy"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} returns 204 when the team is deleted")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/teams/{id} returns 404 when the team does not exist")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Team", 999L)).when(service).delete(eq(999L));

        mockMvc.perform(delete("/api/teams/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Team with id 999 not found"));
    }

    @Test
    @DisplayName("PUT /api/teams/{id} returns 200 with the updated team")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(1L), any(TeamRequest.class))).thenReturn(
                new TeamResponse(1L, "Mercedes", "Mercedes-AMG Petronas", "Germany"));

        mockMvc.perform(put("/api/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mercedes"))
                .andExpect(jsonPath("$.constructorName").value("Mercedes-AMG Petronas"))
                .andExpect(jsonPath("$.country").value("Germany"));
    }

    @Test
    @DisplayName("POST /api/teams returns 409 when the team name already exists")
    void createReturnsConflictWhenDuplicate() throws Exception {
        when(service.create(any(TeamRequest.class)))
                .thenThrow(new DuplicateResourceException("Team with name 'Mercedes' already exists"));

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mercedes",
                                  "constructorName": "Mercedes-AMG Petronas",
                                  "country": "Germany"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value("Team with name 'Mercedes' already exists"));
    }
}
