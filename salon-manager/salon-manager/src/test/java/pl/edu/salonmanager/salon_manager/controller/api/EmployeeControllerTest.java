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
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.CreateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.UpdateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.response.EmployeeDto;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.EmployeeScheduleDto;
import pl.edu.salonmanager.salon_manager.service.EmployeeService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private EmployeeDto employeeDto;
    private CreateEmployeeRequest createRequest;
    private UpdateEmployeeRequest updateRequest;

    @BeforeEach
    void setUp() {
        employeeDto = new EmployeeDto(1L, "John", "Doe", "john@salon.pl");

        createRequest = new CreateEmployeeRequest();
        createRequest.setFirstName("Jane");
        createRequest.setLastName("Smith");
        createRequest.setEmail("jane@salon.pl");

        updateRequest = new UpdateEmployeeRequest();
        updateRequest.setFirstName("John Updated");
        updateRequest.setLastName("Doe Updated");
        updateRequest.setEmail("john.updated@salon.pl");
    }

    @Test
    @WithMockUser
    void shouldGetAllEmployees() throws Exception {
        // Given
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList(employeeDto));

        // When & Then
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].email").value("john@salon.pl"));
    }

    @Test
    @WithMockUser
    void shouldGetEmployeeById() throws Exception {
        // Given
        when(employeeService.getEmployeeById(1L)).thenReturn(employeeDto);

        // When & Then
        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        // Given
        when(employeeService.getEmployeeById(99L)).thenThrow(new ResourceNotFoundException("Employee not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateEmployee() throws Exception {
        // Given
        EmployeeDto created = new EmployeeDto(2L, "Jane", "Smith", "jane@salon.pl");
        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateEmployee() throws Exception {
        // Given
        EmployeeDto updated = new EmployeeDto(1L, "John Updated", "Doe Updated", "john.updated@salon.pl");
        when(employeeService.updateEmployee(eq(1L), any(UpdateEmployeeRequest.class))).thenReturn(updated);

        // When & Then
        mockMvc.perform(put("/api/v1/employees/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteEmployee() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/employees/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetEmployeeSchedule() throws Exception {
        // Given
        EmployeeScheduleDto scheduleDto = new EmployeeScheduleDto(
                1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true
        );
        when(employeeService.getEmployeeSchedule(1L)).thenReturn(Arrays.asList(scheduleDto));

        // When & Then
        mockMvc.perform(get("/api/v1/employees/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].isWorkingDay").value(true));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoEmployees() throws Exception {
        // Given
        when(employeeService.getAllEmployees()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
