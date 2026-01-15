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
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.confirmReservation(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED_BY_CLIENT);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldThrowExceptionWhenConfirmingOtherUsersReservation() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(1L, 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Not authorized to confirm this reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenConfirmingAlreadyConfirmedReservation() {
        testReservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.confirmReservation(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be confirmed in current status");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldApproveReservationSuccessfully() {
        testReservation.setStatus(ReservationStatus.CREATED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        Reservation result = reservationService.approveReservation(1L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.APPROVED_BY_SALON);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldCancelReservationSuccessfully() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
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
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
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

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
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
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
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
}
