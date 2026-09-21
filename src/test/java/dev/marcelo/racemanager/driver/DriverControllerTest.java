package dev.marcelo.racemanager.driver;

import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.dto.DriverRequest;
import dev.marcelo.racemanager.driver.dto.DriverResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    private static final LocalDate ALONSO_BIRTH = LocalDate.of(1981, 7, 29);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriverService service;

    @Test
    @DisplayName("GET /api/drivers returns 200 with the list of drivers")
    void findAllReturnsOk() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new DriverResponse(1L, "Fernando", "Alonso", "Spanish", ALONSO_BIRTH),
                new DriverResponse(2L, "Lando", "Norris", "British", LocalDate.of(1999, 11, 13))));

        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].surname").value("Alonso"))
                .andExpect(jsonPath("$[1].surname").value("Norris"));
    }

    @Test
    @DisplayName("GET /api/drivers/{id} returns 200 with the date in ISO format")
    void findByIdReturnsOk() throws Exception {
        when(service.findById(1L)).thenReturn(
                new DriverResponse(1L, "Fernando", "Alonso", "Spanish", ALONSO_BIRTH));

        mockMvc.perform(get("/api/drivers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fernando"))
                .andExpect(jsonPath("$.nationality").value("Spanish"))
                .andExpect(jsonPath("$.dateOfBirth").value("1981-07-29"));
    }

    @Test
    @DisplayName("GET /api/drivers/{id} returns 404 when the driver does not exist")
    void findByIdReturnsNotFound() throws Exception {
        when(service.findById(999L)).thenThrow(new ResourceNotFoundException("Driver", 999L));

        mockMvc.perform(get("/api/drivers/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Driver with id 999 not found"));
    }

    @Test
    @DisplayName("POST /api/drivers returns 201 with the Location header")
    void createReturnsCreated() throws Exception {
        when(service.create(any(DriverRequest.class))).thenReturn(
                new DriverResponse(3L, "Fernando", "Alonso", "Spanish", ALONSO_BIRTH));

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "Alonso",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "1981-07-29"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/drivers/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.dateOfBirth").value("1981-07-29"));
    }

    @Test
    @DisplayName("POST /api/drivers returns 400 and does not reach the service when the name is missing")
    void createReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "surname": "Alonso",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "1981-07-29"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/drivers returns 400 when the date of birth is in the future")
    void createReturnsBadRequestWhenDateIsInTheFuture() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "Alonso",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.dateOfBirth").value("Date of birth must be in the past"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/drivers returns 400 when the date has an invalid format")
    void createReturnsBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "Alonso",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "29/07/1981"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/drivers/{id} returns 200 with the updated driver")
    void updateReturnsOk() throws Exception {
        when(service.update(eq(1L), any(DriverRequest.class))).thenReturn(
                new DriverResponse(1L, "Fernando", "Alonso Díaz", "Spanish", ALONSO_BIRTH));

        mockMvc.perform(put("/api/drivers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fernando",
                                  "surname": "Alonso Díaz",
                                  "nationality": "Spanish",
                                  "dateOfBirth": "1981-07-29"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Alonso Díaz"));

        verify(service).update(eq(1L), any(DriverRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/drivers/{id} returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/drivers/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/drivers/{id} returns 404 when the driver does not exist")
    void deleteReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Driver", 999L)).when(service).delete(eq(999L));

        mockMvc.perform(delete("/api/drivers/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Driver with id 999 not found"));
    }
}