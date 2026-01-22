package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.AvailabilityResponseDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSchedule;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @Mock
    private EmployeeSpecializationRepository employeeSpecializationRepository;

    @Mock
    private EmployeeScheduleRepository employeeScheduleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalonProperties salonProperties;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Employee employee;
    private ServiceOffer service1;
    private ServiceOffer service2;
    private EmployeeSchedule schedule;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);

        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@salon.pl");

        service1 = new ServiceOffer();
        service1.setId(1L);
        service1.setName("Haircut");
        service1.setPrice(new BigDecimal("50.00"));
        service1.setDurationMinutes(30);

        service2 = new ServiceOffer();
        service2.setId(2L);
        service2.setName("Hair Coloring");
        service2.setPrice(new BigDecimal("100.00"));
        service2.setDurationMinutes(60);

        schedule = new EmployeeSchedule();
        schedule.setId(1L);
        schedule.setEmployee(employee);
        schedule.setDayOfWeek(testDate.getDayOfWeek());
        schedule.setIsWorkingDay(true);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
    }


    @Test
    void shouldThrowExceptionWhenDateIsNull() {
        // When & Then
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(null, Arrays.asList(1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Date cannot be null");
        verify(serviceOfferRepository, never()).findAllById(any());
    }

    @Test
    void shouldThrowExceptionWhenDateIsInPast() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // When & Then
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(pastDate, Arrays.asList(1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot search for dates in the past");
        verify(serviceOfferRepository, never()).findAllById(any());
    }

    @Test
    void shouldThrowExceptionWhenServiceIdsIsNull() {
        // When & Then
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(testDate, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Service IDs list cannot be empty");
        verify(serviceOfferRepository, never()).findAllById(any());
    }

    @Test
    void shouldThrowExceptionWhenServiceIdsIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(testDate, Collections.emptyList()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Service IDs list cannot be empty");
        verify(serviceOfferRepository, never()).findAllById(any());
    }

    @Test
    void shouldThrowExceptionWhenServiceNotFound() {
        // Given
        List<Long> serviceIds = Arrays.asList(1L, 2L);
        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1)); // Only 1 service found, 2 requested

        // When & Then
        assertThatThrownBy(() -> availabilityService.findAvailableSlots(testDate, serviceIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("One or more services not found");
        verify(serviceOfferRepository).findAllById(serviceIds);
    }

    @Test
    void shouldFindAvailableSlotsForQualifiedEmployee() {
        // Given
        List<Long> serviceIds = Arrays.asList(1L);
        when(salonProperties.getSlotDurationMinutes()).thenReturn(30);
        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeSpecializationRepository.findEmployeesWithAllServices(serviceIds, 1L))
                .thenReturn(Arrays.asList(employee));
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(reservationRepository.findActiveReservationsByEmployeeAndDate(1L, testDate))
                .thenReturn(Collections.emptyList());

        // When
        AvailabilityResponseDto result = availabilityService.findAvailableSlots(testDate, serviceIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSearchDate()).isEqualTo(testDate);
        assertThat(result.getTotalDurationMinutes()).isEqualTo(30);
        assertThat(result.getEmployees()).hasSize(1);
        assertThat(result.getEmployees().get(0).getEmployeeId()).isEqualTo(1L);
        assertThat(result.getEmployees().get(0).getAvailableSlots()).isNotEmpty();
        verify(serviceOfferRepository).findAllById(serviceIds);
        verify(employeeSpecializationRepository).findEmployeesWithAllServices(serviceIds, 1L);
        verify(employeeScheduleRepository).findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek());
        verify(reservationRepository).findActiveReservationsByEmployeeAndDate(1L, testDate);
    }

    @Test
    void shouldReturnEmptyListWhenNoQualifiedEmployees() {
        // Given
        List<Long> serviceIds = Arrays.asList(1L);
        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeSpecializationRepository.findEmployeesWithAllServices(serviceIds, 1L))
                .thenReturn(Collections.emptyList());

        // When
        AvailabilityResponseDto result = availabilityService.findAvailableSlots(testDate, serviceIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployees()).isEmpty();
        verify(serviceOfferRepository).findAllById(serviceIds);
        verify(employeeSpecializationRepository).findEmployeesWithAllServices(serviceIds, 1L);
    }

    @Test
    void shouldReturnEmptyListWhenEmployeeDoesNotWorkOnDate() {
        // Given
        List<Long> serviceIds = Arrays.asList(1L);
        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeSpecializationRepository.findEmployeesWithAllServices(serviceIds, 1L))
                .thenReturn(Arrays.asList(employee));
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.empty());

        // When
        AvailabilityResponseDto result = availabilityService.findAvailableSlots(testDate, serviceIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployees()).isEmpty();
        verify(serviceOfferRepository).findAllById(serviceIds);
        verify(employeeSpecializationRepository).findEmployeesWithAllServices(serviceIds, 1L);
        verify(employeeScheduleRepository).findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek());
    }

    // ========== isSlotAvailable Tests ==========

    @Test
    void shouldThrowExceptionWhenEmployeeNotFoundForSlotCheck() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> availabilityService.isSlotAvailable(1L, startTime, serviceIds, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
        verify(employeeRepository).findById(1L);
    }

    @Test
    void shouldReturnFalseWhenEmployeeNotQualified() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(0L); // Not qualified

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, null);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository).findById(1L);
        verify(employeeSpecializationRepository).countEmployeeQualifiedServices(1L, serviceIds);
    }

    @Test
    void shouldReturnFalseWhenEmployeeDoesNotWorkOnDay() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(1L);
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.empty());

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, null);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository).findById(1L);
        verify(employeeSpecializationRepository).countEmployeeQualifiedServices(1L, serviceIds);
        verify(employeeScheduleRepository).findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek());
    }

    @Test
    void shouldReturnFalseWhenSlotIsOutsideWorkingHours() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(8, 0)); // Before 9 AM
        List<Long> serviceIds = Arrays.asList(1L);

        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(1L);
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, null);

        // Then
        assertThat(result).isFalse();
        verify(employeeRepository).findById(1L);
        verify(employeeSpecializationRepository).countEmployeeQualifiedServices(1L, serviceIds);
        verify(employeeScheduleRepository).findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek());
        verify(serviceOfferRepository).findAllById(serviceIds);
    }

    @Test
    void shouldReturnFalseWhenSlotConflictsWithExistingReservation() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);

        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(1L);
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(reservationRepository.hasOverlappingReservation(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true); // Has conflict

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, null);

        // Then
        assertThat(result).isFalse();
        verify(reservationRepository).hasOverlappingReservation(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnTrueWhenSlotIsAvailable() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);

        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(1L);
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(reservationRepository.hasOverlappingReservation(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false); // No conflict

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, null);

        // Then
        assertThat(result).isTrue();
        verify(reservationRepository).hasOverlappingReservation(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(employeeRepository).findById(1L);
        verify(employeeSpecializationRepository).countEmployeeQualifiedServices(1L, serviceIds);
        verify(employeeScheduleRepository).findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek());
    }

    @Test
    void shouldExcludeReservationWhenCheckingAvailability() {
        // Given
        LocalDateTime startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0));
        List<Long> serviceIds = Arrays.asList(1L);
        Long excludeReservationId = 99L;

        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1));
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));
        when(employeeSpecializationRepository.countEmployeeQualifiedServices(1L, serviceIds))
                .thenReturn(1L);
        when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(reservationRepository.hasOverlappingReservationExcluding(
                eq(excludeReservationId), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        // When
        boolean result = availabilityService.isSlotAvailable(1L, startTime, serviceIds, excludeReservationId);

        // Then
        assertThat(result).isTrue();
        verify(reservationRepository).hasOverlappingReservationExcluding(
                eq(excludeReservationId), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(employeeRepository).findById(1L);
        verify(employeeSpecializationRepository).countEmployeeQualifiedServices(1L, serviceIds);
    }

    @Test
    void shouldCalculateCorrectDurationForMultipleServices() {
        // Given
        List<Long> serviceIds = Arrays.asList(1L, 2L);
        when(serviceOfferRepository.findAllById(serviceIds))
                .thenReturn(Arrays.asList(service1, service2)); // 30 + 60 = 90 minutes
        when(employeeSpecializationRepository.findEmployeesWithAllServices(serviceIds, 2L))
                .thenReturn(Collections.emptyList());

        // When
        AvailabilityResponseDto result = availabilityService.findAvailableSlots(testDate, serviceIds);

        // Then
        assertThat(result.getTotalDurationMinutes()).isEqualTo(90);
        verify(serviceOfferRepository).findAllById(serviceIds);
        verify(employeeSpecializationRepository).findEmployeesWithAllServices(serviceIds, 2L);
    }
}
