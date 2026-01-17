package pl.edu.salonmanager.salon_manager.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferCsvService;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceOfferController.class)
class ServiceOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceOfferService serviceOfferService;

    @MockBean
    private ServiceOfferCsvService csvService;

    private ServiceOfferDto serviceDto;
    private CreateServiceRequest createRequest;
    private UpdateServiceRequest updateRequest;

    @BeforeEach
    void setUp() {
        serviceDto = new ServiceOfferDto(1L, "Haircut", new BigDecimal("50.00"), 30);

        createRequest = new CreateServiceRequest();
        createRequest.setName("Manicure");
        createRequest.setPrice(new BigDecimal("40.00"));
        createRequest.setDurationMinutes(45);

        updateRequest = new UpdateServiceRequest();
        updateRequest.setName("Haircut Premium");
        updateRequest.setPrice(new BigDecimal("70.00"));
        updateRequest.setDurationMinutes(40);
    }

    @Test
    @WithMockUser
    void shouldGetAllServices() throws Exception {
        // Given
        when(serviceOfferService.getAllServices()).thenReturn(Arrays.asList(serviceDto));

        // When & Then
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Haircut"))
                .andExpect(jsonPath("$[0].price").value(50.00))
                .andExpect(jsonPath("$[0].durationMinutes").value(30));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateService() throws Exception {
        // Given
        ServiceOfferDto created = new ServiceOfferDto(2L, "Manicure", new BigDecimal("40.00"), 45);
        when(serviceOfferService.createService(any(CreateServiceRequest.class))).thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/v1/services")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Manicure"))
                .andExpect(jsonPath("$.price").value(40.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateService() throws Exception {
        // Given
        ServiceOfferDto updated = new ServiceOfferDto(1L, "Haircut Premium", new BigDecimal("70.00"), 40);
        when(serviceOfferService.updateService(eq(1L), any(UpdateServiceRequest.class))).thenReturn(updated);

        // When & Then
        mockMvc.perform(put("/api/v1/services/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Haircut Premium"))
                .andExpect(jsonPath("$.price").value(70.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteService() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/services/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenServiceNotFound() throws Exception {
        // Given
        when(serviceOfferService.updateService(eq(99L), any(UpdateServiceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Service not found"));

        // When & Then
        mockMvc.perform(put("/api/v1/services/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoServices() throws Exception {
        // Given
        when(serviceOfferService.getAllServices()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldImportServicesFromCsv() throws Exception {
        // Given
        String csvContent = """
                name,price,durationMinutes
                Massage,100.00,60
                Facial,80.00,45
                """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "services.csv",
                "text/csv",
                csvContent.getBytes()
        );

        List<ServiceOfferDto> importedServices = Arrays.asList(
                new ServiceOfferDto(1L, "Massage", new BigDecimal("100.00"), 60),
                new ServiceOfferDto(2L, "Facial", new BigDecimal("80.00"), 45)
        );

        when(csvService.importFromCsv(any())).thenReturn(importedServices);

        // When & Then
        mockMvc.perform(multipart("/api/v1/services/import/csv")
                        .file(csvFile)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Massage"))
                .andExpect(jsonPath("$[0].price").value(100.00))
                .andExpect(jsonPath("$[0].durationMinutes").value(60))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Facial"))
                .andExpect(jsonPath("$[1].price").value(80.00))
                .andExpect(jsonPath("$[1].durationMinutes").value(45));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportServicesToCsv() throws Exception {
        // Given
        String csvContent = """
                name,price,durationMinutes
                Haircut,50.00,30
                Massage,100.00,60
                """;

        when(csvService.exportToCsv()).thenReturn(csvContent);

        // When & Then
        mockMvc.perform(get("/api/v1/services/export/csv")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"services.csv\""))
                .andExpect(content().string(csvContent));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportEmptyListToCsv() throws Exception {
        // Given
        String csvContent = "name,price,durationMinutes\n";
        when(csvService.exportToCsv()).thenReturn(csvContent);

        // When & Then
        mockMvc.perform(get("/api/v1/services/export/csv")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(csvContent));
    }
}
