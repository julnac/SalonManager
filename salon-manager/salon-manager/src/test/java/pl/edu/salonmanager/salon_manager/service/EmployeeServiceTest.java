package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.CreateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.UpdateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.response.EmployeeDto;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.EmployeeScheduleDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSchedule;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.EmployeeScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeScheduleRepository employeeScheduleRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee1;
    private Employee employee2;
    private CreateEmployeeRequest createRequest;
    private UpdateEmployeeRequest updateRequest;
    private EmployeeSchedule schedule;

    @BeforeEach
    void setUp() {
        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setFirstName("John");
        employee1.setLastName("Doe");
        employee1.setEmail("john@salon.pl");

        employee2 = new Employee();
        employee2.setId(2L);
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");
        employee2.setEmail("jane@salon.pl");

        createRequest = new CreateEmployeeRequest();
        createRequest.setFirstName("Anna");
        createRequest.setLastName("Kowalska");
        createRequest.setEmail("anna@salon.pl");

        updateRequest = new UpdateEmployeeRequest();
        updateRequest.setFirstName("John Updated");
        updateRequest.setLastName("Doe Updated");
        updateRequest.setEmail("john.updated@salon.pl");

        schedule = new EmployeeSchedule();
        schedule.setId(1L);
        schedule.setEmployee(employee1);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setIsWorkingDay(true);
    }

    // ========== getAllEmployees Tests ==========

    @Test
    void shouldGetAllEmployees() {
        // Given
        when(employeeRepository.findAll()).thenReturn(Arrays.asList(employee1, employee2));

        // When
        List<EmployeeDto> result = employeeService.getAllEmployees();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
    }

    @Test
    void shouldReturnEmptyListWhenNoEmployees() {
        // Given
        when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<EmployeeDto> result = employeeService.getAllEmployees();

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getEmployeeById Tests ==========

    @Test
    void shouldGetEmployeeById() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));

        // When
        EmployeeDto result = employeeService.getEmployeeById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john@salon.pl");
    }

    @Test
    void shouldThrowExceptionWhenEmployeeNotFound() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    // ========== createEmployee Tests ==========

    @Test
    void shouldCreateEmployee() {
        // Given
        Employee savedEmployee = new Employee();
        savedEmployee.setId(3L);
        savedEmployee.setFirstName("Anna");
        savedEmployee.setLastName("Kowalska");
        savedEmployee.setEmail("anna@salon.pl");

        when(employeeRepository.existsByEmail("anna@salon.pl")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeDto result = employeeService.createEmployee(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getFirstName()).isEqualTo("Anna");
        assertThat(result.getLastName()).isEqualTo("Kowalska");
        assertThat(result.getEmail()).isEqualTo("anna@salon.pl");

        verify(employeeRepository).existsByEmail("anna@salon.pl");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingEmployeeWithDuplicateEmail() {
        // Given
        when(employeeRepository.existsByEmail("anna@salon.pl")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Employee with email anna@salon.pl already exists");
    }

    // ========== updateEmployee Tests ==========

    @Test
    void shouldUpdateEmployee() {
        // Given
        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setFirstName("John Updated");
        updatedEmployee.setLastName("Doe Updated");
        updatedEmployee.setEmail("john.updated@salon.pl");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.existsByEmail("john.updated@salon.pl")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // When
        EmployeeDto result = employeeService.updateEmployee(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John Updated");
        assertThat(result.getLastName()).isEqualTo("Doe Updated");
        assertThat(result.getEmail()).isEqualTo("john.updated@salon.pl");

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldUpdateEmployeeWithSameEmail() {
        // Given
        updateRequest.setEmail("john@salon.pl"); // Same as existing
        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setFirstName("John Updated");
        updatedEmployee.setLastName("Doe Updated");
        updatedEmployee.setEmail("john@salon.pl");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // When
        EmployeeDto result = employeeService.updateEmployee(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@salon.pl");

        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingEmployeeNotFound() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.updateEmployee(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateEmail() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.existsByEmail("john.updated@salon.pl")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.updateEmployee(1L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Employee with email john.updated@salon.pl already exists");
    }

    // ========== deleteEmployee Tests ==========

    @Test
    void shouldDeleteEmployee() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);

        // When
        employeeService.deleteEmployee(1L);

        // Then
        verify(employeeRepository).existsById(1L);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentEmployee() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeService.deleteEmployee(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    // ========== getEmployeeSchedule Tests ==========

    @Test
    void shouldGetEmployeeSchedule() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(employeeScheduleRepository.findByEmployeeId(1L)).thenReturn(Arrays.asList(schedule));

        // When
        List<EmployeeScheduleDto> result = employeeService.getEmployeeSchedule(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.get(0).getIsWorkingDay()).isTrue();
    }

    @Test
    void shouldReturnEmptyListWhenEmployeeHasNoSchedule() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(employeeScheduleRepository.findByEmployeeId(1L)).thenReturn(Collections.emptyList());

        // When
        List<EmployeeScheduleDto> result = employeeService.getEmployeeSchedule(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenGettingScheduleForNonExistentEmployee() {
        // Given
        when(employeeRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeSchedule(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }
}
