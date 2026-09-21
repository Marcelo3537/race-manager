package dev.marcelo.racemanager.driver;

import dev.marcelo.racemanager.common.exception.ResourceNotFoundException;
import dev.marcelo.racemanager.driver.dto.DriverRequest;
import dev.marcelo.racemanager.driver.dto.DriverResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    private static final LocalDate ALONSO_BIRTH = LocalDate.of(1981, 7, 29);

    @Mock
    private DriverRepository repository;

    @InjectMocks
    private DriverService service;

    @Test
    @DisplayName("findById returns the driver when it exists")
    void findByIdReturnsDriver() {
        Driver driver = new Driver("Fernando", "Alonso", "Spanish", ALONSO_BIRTH);
        when(repository.findById(1L)).thenReturn(Optional.of(driver));

        DriverResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Fernando");
        assertThat(response.surname()).isEqualTo("Alonso");
        assertThat(response.nationality()).isEqualTo("Spanish");
        assertThat(response.dateOfBirth()).isEqualTo(ALONSO_BIRTH);
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("findAll returns every driver mapped to a response")
    void findAllReturnsEveryDriver() {
        when(repository.findAll()).thenReturn(List.of(
                new Driver("Fernando", "Alonso", "Spanish", ALONSO_BIRTH),
                new Driver("Lando", "Norris", "British", LocalDate.of(1999, 11, 13))));

        List<DriverResponse> responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DriverResponse::surname)
                .containsExactly("Alonso", "Norris");
    }

    @Test
    @DisplayName("create saves the driver")
    void createSavesDriver() {
        DriverRequest request = new DriverRequest("Fernando", "Alonso", "Spanish", ALONSO_BIRTH);
        when(repository.save(any(Driver.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = service.create(request);

        assertThat(response.surname()).isEqualTo("Alonso");
        assertThat(response.dateOfBirth()).isEqualTo(ALONSO_BIRTH);
        verify(repository).save(any(Driver.class));
    }

    @Test
    @DisplayName("update modifies the driver when it exists")
    void updateModifiesDriver() {
        DriverRequest request =
                new DriverRequest("Fernando", "Alonso Díaz", "Spanish", ALONSO_BIRTH);
        Driver existingDriver =
                new Driver("Fernando", "Alonso", "Asturian", LocalDate.of(1981, 1, 1));
        when(repository.findById(1L)).thenReturn(Optional.of(existingDriver));

        DriverResponse response = service.update(1L, request);

        assertThat(response.surname()).isEqualTo("Alonso Díaz");
        assertThat(response.dateOfBirth()).isEqualTo(ALONSO_BIRTH);

        assertThat(existingDriver.getSurname()).isEqualTo("Alonso Díaz");
        assertThat(existingDriver.getNationality()).isEqualTo("Spanish");
        assertThat(existingDriver.getDateOfBirth()).isEqualTo(ALONSO_BIRTH);

        // update relies on dirty checking, so save() is never called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException when the driver does not exist")
    void updateThrowsWhenMissing() {
        DriverRequest request = new DriverRequest("Fernando", "Alonso", "Spanish", ALONSO_BIRTH);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("delete removes the driver when it exists")
    void deleteRemovesDriver() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException and deletes nothing when the driver does not exist")
    void deleteThrowsWhenMissing() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).deleteById(anyLong());
    }
}