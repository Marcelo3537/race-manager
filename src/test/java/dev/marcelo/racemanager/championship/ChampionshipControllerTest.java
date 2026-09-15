package dev.marcelo.racemanager.championship;

import dev.marcelo.racemanager.championship.dto.ChampionshipRequest;
import dev.marcelo.racemanager.championship.dto.ChampionshipResponse;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
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

@WebMvcTest(ChampionshipController.class)
class ChampionshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChampionshipService service;

    @Test
    @DisplayName("GET /api/championships returns 200 with the list of championships")
    void findAllReturnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new ChampionshipResponse(1L, "Formula 1", 2026, "International", ChampionshipStatus.ONGOING),
                new ChampionshipResponse(2L, "GT World", 2026, "International", ChampionshipStatus.UPCOMING)));

        mockMvc.perform(get("/api/championships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Formula 1"))
                .andExpect(jsonPath("$[1].name").value("GT World"));
    }

    @Test
    @DisplayName("GET /api/championships/{id} returns 200 with the championship")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(1L)).thenReturn(
                new ChampionshipResponse(1L, "Formula 1", 2026, "International", ChampionshipStatus.ONGOING));

        mockMvc.perform(get("/api/championships/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ONGOING"));
    }

    @Test
    @DisplayName("GET /api/championships/{id} returns 404 when the championship does not exist")
    void findByIdReturnsNotFound() throws Exception {
        when(service.findById(999L))
                .thenThrow(new ResourceNotFoundException("Championship", 999L));

        mockMvc.perform(get("/api/championships/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Championship with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/championships returns 201 with the Location header")
    void createReturnsCreated() throws Exception {
        when(service.create(any(ChampionshipRequest.class))).thenReturn(
                new ChampionshipResponse(3L, "Formula 1", 2026, "International", ChampionshipStatus.UPCOMING));

        mockMvc.perform(post("/api/championships")
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
                .andExpect(header().string("Location", "http://localhost/api/championships/3"))
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("POST /api/championships returns 400 and does not reach the service when the name is missing")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "season": 2026,
                                  "status": "UPCOMING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("DELETE /api/championships/{id} returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/championships/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/championships/{id} returns 404 when the championship does not exist")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Championship", 999L))
                .when(service).delete(eq(999L));

        mockMvc.perform(delete("/api/championships/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/championships/{id} returns 200 with the updated championship")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(1L), any(ChampionshipRequest.class))).thenReturn(
                new ChampionshipResponse(1L, "Formula 1", 2026, "International", ChampionshipStatus.ONGOING));

        mockMvc.perform(put("/api/championships/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Formula 1",
                                  "season": 2026,
                                  "country": "International",
                                  "status": "ONGOING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ONGOING"));
    }

    @Test
    @DisplayName("POST /api/championships returns 409 when the championship already exists")
    void createReturnsConflictWhenDuplicate() throws Exception {
        when(service.create(any(ChampionshipRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "Championship 'Formula 1' already exists for season 2026"));

        mockMvc.perform(post("/api/championships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Formula 1",
                                  "season": 2026,
                                  "country": "International",
                                  "status": "ONGOING"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value("Championship 'Formula 1' already exists for season 2026"));
    }
}