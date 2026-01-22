package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.UpdateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.*;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.ReservationRepository;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @Mock
    private ReservationSecurityService securityService;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Employee testEmployee;
    private ServiceOffer testService;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");

        testService = new ServiceOffer();
        testService.setId(1L);
        testService.setName("Haircut");
        testService.setPrice(new BigDecimal("50.00"));
        testService.setDurationMinutes(30);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setEmployee(testEmployee);
        testReservation.setStartTime(LocalDateTime.now().plusDays(1));
        testReservation.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        testReservation.setServices(Set.of(testService));
        testReservation.setTotalPrice(new BigDecimal("50.00"));
        testReservation.setStatus(ReservationStatus.CREATED);
    }

    @Test
    void shouldCreateReservationSuccessfully() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(availabilityService.isSlotAvailable(any(), any(), any(), any())).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.createReservation(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CREATED);
        verify(reservationRepository).save(any(Reservation.class));
        verify(availabilityService).isSlotAvailable(eq(1L), any(), any(), eq(null));
    }

    @Test
    void shouldThrowExceptionWhenCreatingReservationWithoutServices() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Collections.emptySet());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Total service duration must be greater than 0");

        verify(reservationRepository, never()).save(any());
        verify(availabilityService, never()).isSlotAvailable(any(), any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSlotNotAvailable() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(availabilityService.isSlotAvailable(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldConfirmReservationSuccessfully() {
        testReservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.confirmReservation(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED_BY_CLIENT);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldThrowExceptionWhenConfirmingOtherUsersReservation() {
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(1L, 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Not authorized to confirm this reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenConfirmingAlreadyConfirmedReservation() {
        testReservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be confirmed in current status");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldApproveReservationSuccessfully() {
        testReservation.setStatus(ReservationStatus.CREATED);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.approveReservation(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.APPROVED_BY_SALON);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldCancelReservationSuccessfully() {
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(securityService.canCancelReservation(any(), any())).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.cancelReservation(1L, testUser);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(any(Reservation.class));
        verify(securityService).canCancelReservation(testReservation, testUser);
    }

    @Test
    void shouldThrowExceptionWhenCancellingAlreadyCancelledReservation() {
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(securityService.canCancelReservation(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, testUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized to cancel this reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldUpdateReservationAndCalculateEndTime() {
        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(securityService.canEditReservation(any(), any())).thenReturn(true);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(availabilityService.isSlotAvailable(any(), any(), any(), any())).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.updateReservation(1L, request);

        assertThat(result).isNotNull();
        verify(reservationRepository).save(any(Reservation.class));
        verify(serviceOfferRepository).findById(1L);
        verify(securityService).canEditReservation(testReservation, testUser);
    }

    @Test
    void shouldNotAllowUserToCancelConfirmedReservation() {
        testReservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(securityService.canCancelReservation(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, testUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized to cancel this reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldGetMyReservations() {
        List<Reservation> reservations = Arrays.asList(testReservation);
        when(reservationRepository.findByUserId(1L)).thenReturn(reservations);

        List<Reservation> result = reservationService.getMyReservations(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findByUserId(1L);
    }

    @Test
    void shouldGetAllReservationsWithStatusFilter() {
        List<Reservation> reservations = Arrays.asList(testReservation);
        when(reservationRepository.findByStatus(ReservationStatus.CREATED)).thenReturn(reservations);

        List<Reservation> result = reservationService.getAllReservations(ReservationStatus.CREATED);

        assertThat(result).hasSize(1);
        verify(reservationRepository).findByStatus(ReservationStatus.CREATED);
        verify(reservationRepository, never()).findAll();
    }

    @Test
    void shouldCalculateTotalPriceFromMultipleServices() {
        ServiceOffer service2 = new ServiceOffer();
        service2.setId(2L);
        service2.setName("Styling");
        service2.setPrice(new BigDecimal("30.00"));
        service2.setDurationMinutes(20);

        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L, 2L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(serviceOfferRepository.findById(2L)).thenReturn(Optional.of(service2));
        when(availabilityService.isSlotAvailable(any(), any(), any(), any())).thenReturn(true);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        Reservation result = reservationService.createReservation(request, 1L);

        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(result.getEndTime()).isEqualTo(request.getStartTime().plusMinutes(50)); // 30 + 20
    }

    // ==================== Additional Tests for 100% Coverage ====================

    @Test
    void shouldGetAllReservationsWithoutStatusFilter() {
        // Given
        List<Reservation> reservations = Arrays.asList(testReservation);
        when(reservationRepository.findAll()).thenReturn(reservations);

        // When
        List<Reservation> result = reservationService.getAllReservations(null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(reservationRepository).findAll();
        verify(reservationRepository, never()).findByStatus(any());
    }

    @Test
    void shouldGetReservationById() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When
        Reservation result = reservationService.getReservationById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(reservationRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenReservationNotFoundById() {
        // Given
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.getReservationById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found with id: 999");

        verify(reservationRepository).findById(999L);
    }

    @Test
    void shouldGetReservationsByEmployeeAndDateRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        List<Reservation> reservations = Arrays.asList(testReservation);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(reservationRepository.findByEmployeeAndStartTimeBetween(
                eq(testEmployee), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(reservations);

        // When
        List<Reservation> result = reservationService.getReservationsByEmployeeAndDateRange(1L, startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testReservation);
        verify(employeeRepository).findById(1L);
        verify(reservationRepository).findByEmployeeAndStartTimeBetween(
                eq(testEmployee), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldThrowExceptionWhenEmployeeNotFoundInDateRangeQuery() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.getReservationsByEmployeeAndDateRange(999L, startDate, endDate))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: 999");

        verify(employeeRepository).findById(999L);
        verify(reservationRepository, never()).findByEmployeeAndStartTimeBetween(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenApprovingAlreadyApprovedReservation() {
        // Given
        testReservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.approveReservation(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be approved in current status");

        verify(reservationRepository).findByIdWithUserAndEmployee(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenApprovingCancelledReservation() {
        // Given
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.approveReservation(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be approved in current status: CANCELLED");

        verify(reservationRepository).findByIdWithUserAndEmployee(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenApproveReservationNotFound() {
        // Given
        when(reservationRepository.findByIdWithUserAndEmployee(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.approveReservation(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found with id: 999");

        verify(reservationRepository).findByIdWithUserAndEmployee(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenConfirmReservationNotFound() {
        // Given
        when(reservationRepository.findByIdWithUserAndEmployee(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.confirmReservation(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found with id: 999");

        verify(reservationRepository).findByIdWithUserAndEmployee(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCancelReservationNotFound() {
        // Given
        when(reservationRepository.findByIdWithUserAndEmployee(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.cancelReservation(999L, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found with id: 999");

        verify(reservationRepository).findByIdWithUserAndEmployee(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdateReservationNotFound() {
        // Given
        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(reservationRepository.findByIdWithUserAndEmployee(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.updateReservation(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found with id: 999");

        verify(reservationRepository).findByIdWithUserAndEmployee(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdateNotAuthorized() {
        // Given
        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));
        when(securityService.canEditReservation(any(), any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> reservationService.updateReservation(1L, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not authorized to update this reservation");

        verify(reservationRepository).findByIdWithUserAndEmployee(1L);
        verify(securityService).canEditReservation(testReservation, testUser);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCreatingReservationInThePast() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create reservation in the past");

        verify(userRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCreatingReservationTooFarInFuture() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusMonths(4));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create reservation more than 3 months ahead");

        verify(userRepository).findById(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEmployeeNotFoundDuringCreate() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(999L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: 999");

        verify(userRepository).findById(1L);
        verify(employeeRepository).findById(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenServiceNotFoundDuringCreate() {
        // Given
        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(999L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(serviceOfferRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found with id: 999");

        verify(userRepository).findById(1L);
        verify(employeeRepository).findById(1L);
        verify(serviceOfferRepository).findById(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldConfirmReservationWhenStatusIsCreated() {
        // Given - trying to confirm CREATED status should fail
        testReservation.setStatus(ReservationStatus.CREATED);
        when(reservationRepository.findByIdWithUserAndEmployee(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThatThrownBy(() -> reservationService.confirmReservation(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be confirmed in current status: CREATED");

        verify(reservationRepository).findByIdWithUserAndEmployee(1L);
        verify(reservationRepository, never()).save(any());
    }
}
