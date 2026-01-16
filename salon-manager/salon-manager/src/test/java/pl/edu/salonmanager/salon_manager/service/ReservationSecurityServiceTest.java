package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationSecurityService reservationSecurityService;

    private User adminUser;
    private User regularUser;
    private User otherUser;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@salon.pl");

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setEmail("user@example.com");

        otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail("other@example.com");

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(regularUser);
        reservation.setStatus(ReservationStatus.CREATED);
    }

    @Test
    void shouldAllowAdminToEditAnyReservation() {
        when(userRepository.hasAdminRole(1L)).thenReturn(true);

        boolean result = reservationSecurityService.canEditReservation(reservation, adminUser);

        assertThat(result).isTrue();
    }

    @Test
    void shouldAllowOwnerToEditCreatedReservation() {
        reservation.setStatus(ReservationStatus.CREATED);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        boolean result = reservationSecurityService.canEditReservation(reservation, regularUser);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotAllowOwnerToEditApprovedReservation() {
        reservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        boolean result = reservationSecurityService.canEditReservation(reservation, regularUser);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotAllowOwnerToEditCancelledReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canEditReservation(reservation, regularUser);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotAllowNonOwnerToEditReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CREATED);
        when(userRepository.hasAdminRole(3L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canEditReservation(reservation, otherUser);

        // Then
        assertThat(result).isFalse();
    }

    // ========== canCancelReservation Tests ==========

    @Test
    void shouldAllowAdminToCancelAnyReservation() {
        // Given
        when(userRepository.hasAdminRole(1L)).thenReturn(true);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, adminUser);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldAllowOwnerToCancelCreatedReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CREATED);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, regularUser);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldAllowOwnerToCancelApprovedReservation() {
        // Given
        reservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, regularUser);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotAllowOwnerToCancelConfirmedReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, regularUser);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotAllowOwnerToCancelAlreadyCancelledReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(userRepository.hasAdminRole(2L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, regularUser);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotAllowNonOwnerToCancelReservation() {
        // Given
        reservation.setStatus(ReservationStatus.CREATED);
        when(userRepository.hasAdminRole(3L)).thenReturn(false);

        // When
        boolean result = reservationSecurityService.canCancelReservation(reservation, otherUser);

        // Then
        assertThat(result).isFalse();
    }
}
