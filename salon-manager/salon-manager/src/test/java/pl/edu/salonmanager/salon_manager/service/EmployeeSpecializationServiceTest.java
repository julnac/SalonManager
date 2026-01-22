package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSpecialization.request.CreateEmployeeSpecializationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSpecialization.response.EmployeeSpecializationDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSpecialization;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.EmployeeSpecializationRepository;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.math.BigDecimal;
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
class EmployeeSpecializationServiceTest {

    @Mock
    private EmployeeSpecializationRepository employeeSpecializationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @InjectMocks
    private EmployeeSpecializationService employeeSpecializationService;

    private Employee employee;
    private ServiceOffer service;
    private EmployeeSpecialization specialization1;
    private EmployeeSpecialization specialization2;
    private CreateEmployeeSpecializationRequest createRequest;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@salon.pl");

        service = new ServiceOffer();
        service.setId(1L);
        service.setName("Haircut");
        service.setPrice(new BigDecimal("50.00"));
        service.setDurationMinutes(30);

        specialization1 = new EmployeeSpecialization();
        specialization1.setId(1L);
        specialization1.setEmployee(employee);
        specialization1.setServiceOffer(service);
        specialization1.setExperienceYears(5);

        ServiceOffer service2 = new ServiceOffer();
        service2.setId(2L);
        service2.setName("Hair Coloring");
        service2.setPrice(new BigDecimal("100.00"));
        service2.setDurationMinutes(60);

        specialization2 = new EmployeeSpecialization();
        specialization2.setId(2L);
        specialization2.setEmployee(employee);
        specialization2.setServiceOffer(service2);
        specialization2.setExperienceYears(3);

        createRequest = new CreateEmployeeSpecializationRequest();
        createRequest.setEmployeeId(1L);
        createRequest.setServiceId(1L);
        createRequest.setExperienceYears(5);
    }

    // ========== getAllSpecializations Tests ==========

    @Test
    void shouldGetAllSpecializations() {
        // Given
        when(employeeSpecializationRepository.findAll()).thenReturn(Arrays.asList(specialization1, specialization2));

        // When
        List<EmployeeSpecializationDto> result = employeeSpecializationService.getAllSpecializations();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getServiceName()).isEqualTo("Haircut");
        assertThat(result.get(1).getServiceName()).isEqualTo("Hair Coloring");
        verify(employeeSpecializationRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSpecializations() {
        // Given
        when(employeeSpecializationRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<EmployeeSpecializationDto> result = employeeSpecializationService.getAllSpecializations();

        // Then
        assertThat(result).isEmpty();
        verify(employeeSpecializationRepository).findAll();
    }

    // ========== getSpecializationById Tests ==========

    @Test
    void shouldGetSpecializationById() {
        // Given
        when(employeeSpecializationRepository.findById(1L)).thenReturn(Optional.of(specialization1));

        // When
        EmployeeSpecializationDto result = employeeSpecializationService.getSpecializationById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        assertThat(result.getEmployeeName()).isEqualTo("John Doe");
        assertThat(result.getServiceId()).isEqualTo(1L);
        assertThat(result.getServiceName()).isEqualTo("Haircut");
        assertThat(result.getExperienceYears()).isEqualTo(5);
        verify(employeeSpecializationRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSpecializationNotFound() {
        // Given
        when(employeeSpecializationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeSpecializationService.getSpecializationById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialization not found");
        verify(employeeSpecializationRepository).findById(1L);
    }

    // ========== createSpecialization Tests ==========

    @Test
    void shouldCreateSpecialization() {
        // Given
        EmployeeSpecialization savedSpecialization = new EmployeeSpecialization();
        savedSpecialization.setId(3L);
        savedSpecialization.setEmployee(employee);
        savedSpecialization.setServiceOffer(service);
        savedSpecialization.setExperienceYears(5);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(service));
        when(employeeSpecializationRepository.save(any(EmployeeSpecialization.class))).thenReturn(savedSpecialization);

        // When
        EmployeeSpecializationDto result = employeeSpecializationService.createSpecialization(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getEmployeeId()).isEqualTo(1L);
        assertThat(result.getServiceId()).isEqualTo(1L);
        assertThat(result.getExperienceYears()).isEqualTo(5);

        verify(employeeRepository).findById(1L);
        verify(serviceOfferRepository).findById(1L);
        verify(employeeSpecializationRepository).save(any(EmployeeSpecialization.class));
    }

    @Test
    void shouldThrowExceptionWhenEmployeeNotFoundForCreateSpecialization() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeSpecializationService.createSpecialization(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
        verify(employeeRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenServiceNotFoundForCreateSpecialization() {
        // Given
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeSpecializationService.createSpecialization(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
        verify(employeeRepository).findById(1L);
    }

    @Test
    void shouldSetExperienceYearsWhenCreatingSpecialization() {
        // Given
        EmployeeSpecialization savedSpecialization = new EmployeeSpecialization();
        savedSpecialization.setId(3L);
        savedSpecialization.setEmployee(employee);
        savedSpecialization.setServiceOffer(service);
        savedSpecialization.setExperienceYears(10);

        createRequest.setExperienceYears(10);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(service));
        when(employeeSpecializationRepository.save(any(EmployeeSpecialization.class))).thenReturn(savedSpecialization);

        // When
        EmployeeSpecializationDto result = employeeSpecializationService.createSpecialization(createRequest);

        // Then
        assertThat(result.getExperienceYears()).isEqualTo(10);
        verify(employeeRepository).findById(1L);
    }

    // ========== deleteSpecialization Tests ==========

    @Test
    void shouldDeleteSpecialization() {
        // Given
        when(employeeSpecializationRepository.existsById(1L)).thenReturn(true);

        // When
        employeeSpecializationService.deleteSpecialization(1L);

        // Then
        verify(employeeSpecializationRepository).existsById(1L);
        verify(employeeSpecializationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentSpecialization() {
        // Given
        when(employeeSpecializationRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeSpecializationService.deleteSpecialization(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialization not found");
        verify(employeeSpecializationRepository).existsById(1L);
    }
}
