package dev.marcelo.racemanager.circuit;

import dev.marcelo.racemanager.circuit.dto.CircuitRequest;
import dev.marcelo.racemanager.circuit.dto.CircuitResponse;
import dev.marcelo.racemanager.common.exception.DuplicateResourceException;
import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(CircuitController.class)
class CircuitControllerTest {

    private static final BigDecimal MONZA_LENGTH = new BigDecimal("5.793");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CircuitService service;

    @Test
    @DisplayName("GET /api/circuits returns 200 with the list of circuits")
    void findAllReturnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new CircuitResponse(1L, "Monza", "Italy", "Monza", MONZA_LENGTH),
                new CircuitResponse(2L, "Monaco", "Monaco", "Monte Carlo", new BigDecimal("3.337"))));

        mockMvc.perform(get("/api/circuits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Monza"))
                .andExpect(jsonPath("$[1].name").value("Monaco"));
    }

    @Test
    @DisplayName("GET /api/circuits/{id} returns 200 with the length as a JSON number")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(1L)).thenReturn(
                new CircuitResponse(1L, "Monza", "Italy", "Monza", MONZA_LENGTH));

        mockMvc.perform(get("/api/circuits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city").value("Monza"))
                .andExpect(jsonPath("$.lengthKm").value(5.793));
    }

    @Test
    @DisplayName("GET /api/circuits/{id} returns 404 when the circuit does not exist")
    void findByIdReturnsNotFound() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Circuit", 999L));

        mockMvc.perform(get("/api/circuits/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Circuit with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/circuits returns 201 with the Location header")
    void createReturnsCreated() throws Exception {
        when(service.create(any(CircuitRequest.class))).thenReturn(
                new CircuitResponse(3L, "Monza", "Italy", "Monza", MONZA_LENGTH));

        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "city": "Monza",
                                  "lengthKm": 5.793
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/circuits/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.lengthKm").value(5.793));
    }

    @Test
    @DisplayName("POST /api/circuits returns 400 and does not reach the service when the name is missing")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "country": "Italy",
                                  "lengthKm": 5.793
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/circuits returns 400 when the length is zero")
    void createReturnsBadRequestWhenLengthIsZero() throws Exception {
        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "lengthKm": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lengthKm").value("Length must be greater than zero"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/circuits returns 400 when the length has more than three decimals")
    void createReturnsBadRequestWhenLengthHasTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "lengthKm": 5.1234
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lengthKm")
                        .value("Length must have at most 2 integer digits and 3 decimals"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/circuits/{id} returns 200 with the updated circuit")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(1L), any(CircuitRequest.class))).thenReturn(
                new CircuitResponse(1L, "Monza", "Italy", "Monza", MONZA_LENGTH));

        mockMvc.perform(put("/api/circuits/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "city": "Monza",
                                  "lengthKm": 5.793
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lengthKm").value(5.793));

        verify(service).update(eq(1L), any(CircuitRequest.class));
    }

    @Test
    @DisplayName("POST /api/circuits returns 409 when the circuit already exists")
    void createReturnsConflictWhenDuplicate() throws Exception {
        when(service.create(any(CircuitRequest.class)))
                .thenThrow(new DuplicateResourceException("Circuit 'Monza' already exists"));

        mockMvc.perform(post("/api/circuits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Monza",
                                  "country": "Italy",
                                  "lengthKm": 5.793
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource already exists"))
                .andExpect(jsonPath("$.detail").value("Circuit 'Monza' already exists"));
    }

    @Test
    @DisplayName("DELETE /api/circuits/{id} returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/circuits/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/circuits/{id} returns 404 when the circuit does not exist")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Circuit", 999L)).when(service).delete(eq(999L));

        mockMvc.perform(delete("/api/circuits/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Circuit with id 999 not found"));
    }
}