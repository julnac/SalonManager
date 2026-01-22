package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOfferCsvServiceTest {

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @Mock
    private ServiceOfferService serviceOfferService;

    @InjectMocks
    private ServiceOfferCsvService csvService;

    private ServiceOfferDto testServiceDto1;
    private ServiceOfferDto testServiceDto2;

    @BeforeEach
    void setUp() {
        testServiceDto1 = new ServiceOfferDto(1L, "Strzyżenie", new BigDecimal("50.00"), 30);
        testServiceDto2 = new ServiceOfferDto(2L, "Farbowanie", new BigDecimal("150.00"), 120);
    }

    // ==================== Export Tests ====================

    @Test
    void shouldExportServicesToCsv() {
        // Given
        when(serviceOfferService.getAllServices()).thenReturn(Arrays.asList(testServiceDto1, testServiceDto2));

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).isNotNull();
        assertThat(csv).contains("name,price,durationMinutes");
        assertThat(csv).contains("Strzyżenie,50.00,30");
        assertThat(csv).contains("Farbowanie,150.00,120");
        verify(serviceOfferService).getAllServices();
    }

    @Test
    void shouldExportEmptyListToCsv() {
        // Given
        when(serviceOfferService.getAllServices()).thenReturn(Collections.emptyList());

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).isEqualTo("name,price,durationMinutes\n");
        verify(serviceOfferService).getAllServices();
    }

    @Test
    void shouldEscapeCsvValuesWithComma() {
        // Given
        ServiceOfferDto serviceWithComma = new ServiceOfferDto(1L, "Strzyżenie, stylizacja", new BigDecimal("80.00"), 45);
        when(serviceOfferService.getAllServices()).thenReturn(List.of(serviceWithComma));

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).contains("\"Strzyżenie, stylizacja\"");
        verify(serviceOfferService).getAllServices();
    }

    @Test
    void shouldEscapeCsvValuesWithQuotes() {
        // Given
        ServiceOfferDto serviceWithQuotes = new ServiceOfferDto(1L, "Strzyżenie \"Premium\"", new BigDecimal("100.00"), 60);
        when(serviceOfferService.getAllServices()).thenReturn(List.of(serviceWithQuotes));

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).contains("\"Strzyżenie \"\"Premium\"\"\"");
        verify(serviceOfferService).getAllServices();
    }

    @Test
    void shouldEscapeCsvValuesWithNewline() {
        // Given
        ServiceOfferDto serviceWithNewline = new ServiceOfferDto(1L, "Strzyżenie\nz myciem", new BigDecimal("60.00"), 40);
        when(serviceOfferService.getAllServices()).thenReturn(List.of(serviceWithNewline));

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).contains("\"Strzyżenie\nz myciem\"");
        verify(serviceOfferService).getAllServices();
    }

    @Test
    void shouldHandleNullNameInExport() {
        // Given
        ServiceOfferDto serviceWithNullName = new ServiceOfferDto(1L, null, new BigDecimal("50.00"), 30);
        when(serviceOfferService.getAllServices()).thenReturn(List.of(serviceWithNullName));

        // When
        String csv = csvService.exportToCsv();

        // Then
        assertThat(csv).contains(",50.00,30");
        verify(serviceOfferService).getAllServices();
    }

    // ==================== Import Tests ====================

    @Test
    void shouldImportServicesFromCsv() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,50.00,30\nFarbowanie,150.00,120";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(serviceOfferService.createService(any(CreateServiceRequest.class)))
                .thenReturn(testServiceDto1)
                .thenReturn(testServiceDto2);

        // When
        List<ServiceOfferDto> result = csvService.importFromCsv(file);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Strzyżenie");
        assertThat(result.get(1).getName()).isEqualTo("Farbowanie");
        verify(serviceOfferService, times(2)).createService(any(CreateServiceRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is empty");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenFileIsNotCsv() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "services.txt", "text/plain",
                "some content".getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File must be a CSV file");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenHeaderIsInvalid() {
        // Given
        String csvContent = "invalid,header,format\nStrzyżenie,50.00,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid CSV format");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenHeaderIsMissing() {
        // Given - file with content but header line is null (only whitespace/newlines)
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                "\n\n".getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid CSV format");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldSkipEmptyLines() {
        // Given
        String csvContent = "name,price,durationMinutes\n\nStrzyżenie,50.00,30\n\n";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(serviceOfferService.createService(any(CreateServiceRequest.class))).thenReturn(testServiceDto1);

        // When
        List<ServiceOfferDto> result = csvService.importFromCsv(file);

        // Then
        assertThat(result).hasSize(1);
        verify(serviceOfferService, times(1)).createService(any(CreateServiceRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenWrongNumberOfColumns() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,50.00";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Expected 3 columns");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Given
        String csvContent = "name,price,durationMinutes\n,50.00,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Name cannot be empty");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenNameIsTooLong() {
        // Given
        String longName = "A".repeat(101);
        String csvContent = "name,price,durationMinutes\n" + longName + ",50.00,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Name too long");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenPriceIsInvalid() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,invalid,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid price format");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenPriceIsZero() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,0,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Price must be positive");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNegative() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,-10.00,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Price must be positive");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenDurationIsInvalid() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,50.00,invalid";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid duration format");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZero() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,50.00,0";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duration must be positive");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldThrowExceptionWhenDurationIsNegative() {
        // Given
        String csvContent = "name,price,durationMinutes\nStrzyżenie,50.00,-30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duration must be positive");

        verify(serviceOfferService, never()).createService(any());
    }

    @Test
    void shouldTrimWhitespaceFromValues() {
        // Given
        String csvContent = "name,price,durationMinutes\n  Strzyżenie  ,  50.00  ,  30  ";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(serviceOfferService.createService(any(CreateServiceRequest.class))).thenReturn(testServiceDto1);

        // When
        List<ServiceOfferDto> result = csvService.importFromCsv(file);

        // Then
        assertThat(result).hasSize(1);
        verify(serviceOfferService).createService(argThat(req ->
                req.getName().equals("Strzyżenie") &&
                req.getPrice().compareTo(new BigDecimal("50.00")) == 0 &&
                req.getDurationMinutes() == 30
        ));
    }

    @Test
    void shouldIncludeLineNumberInErrorMessage() {
        // Given
        String csvContent = "name,price,durationMinutes\nValid,50.00,30\nInvalid,bad,30";
        MultipartFile file = new MockMultipartFile("file", "services.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        when(serviceOfferService.createService(any(CreateServiceRequest.class))).thenReturn(testServiceDto1);

        // When & Then
        assertThatThrownBy(() -> csvService.importFromCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("line 3");

        verify(serviceOfferService, times(1)).createService(any(CreateServiceRequest.class));
    }
}
