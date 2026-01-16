package pl.edu.salonmanager.salon_manager.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSpecialization.request.CreateEmployeeSpecializationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSpecialization.response.EmployeeSpecializationDto;
import pl.edu.salonmanager.salon_manager.service.EmployeeSpecializationService;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeSpecializationController.class)
class EmployeeSpecializationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeSpecializationService employeeSpecializationService;

    private EmployeeSpecializationDto specializationDto;
    private CreateEmployeeSpecializationRequest createRequest;

    @BeforeEach
    void setUp() {
        specializationDto = new EmployeeSpecializationDto(1L, 1L, "John Doe", 1L, "Haircut", 5);

        createRequest = new CreateEmployeeSpecializationRequest();
        createRequest.setEmployeeId(1L);
        createRequest.setServiceId(1L);
        createRequest.setExperienceYears(5);
    }

    @Test
    @WithMockUser
    void shouldGetAllSpecializations() throws Exception {
        // Given
        when(employeeSpecializationService.getAllSpecializations()).thenReturn(Arrays.asList(specializationDto));

        // When & Then
        mockMvc.perform(get("/api/v1/employee-specializations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].employeeName").value("John Doe"))
                .andExpect(jsonPath("$[0].serviceName").value("Haircut"))
                .andExpect(jsonPath("$[0].experienceYears").value(5));
    }

    @Test
    @WithMockUser
    void shouldGetSpecializationById() throws Exception {
        // Given
        when(employeeSpecializationService.getSpecializationById(1L)).thenReturn(specializationDto);

        // When & Then
        mockMvc.perform(get("/api/v1/employee-specializations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateSpecialization() throws Exception {
        // Given
        EmployeeSpecializationDto created = new EmployeeSpecializationDto(2L, 1L, "John Doe", 1L, "Haircut", 5);
        when(employeeSpecializationService.createSpecialization(any(CreateEmployeeSpecializationRequest.class)))
                .thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/v1/employee-specializations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.experienceYears").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteSpecialization() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/employee-specializations/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenSpecializationNotFound() throws Exception {
        // Given
        when(employeeSpecializationService.getSpecializationById(99L))
                .thenThrow(new ResourceNotFoundException("Specialization not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee-specializations/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoSpecializations() throws Exception {
        // Given
        when(employeeSpecializationService.getAllSpecializations()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/employee-specializations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
