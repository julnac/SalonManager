package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientReservationController.class)
class ClientReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .enabled(true)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldDisplayUserReservations() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        ServiceOffer service1 = ServiceOffer.builder()
                .id(1L)
                .name("Haircut")
                .price(new BigDecimal("50.00"))
                .durationMinutes(30)
                .build();

        ServiceOffer service2 = ServiceOffer.builder()
                .id(2L)
                .name("Manicure")
                .price(new BigDecimal("40.00"))
                .durationMinutes(45)
                .build();

        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("Anna")
                .lastName("Nowak")
                .build();

        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jan")
                .lastName("Kowalski")
                .build();

        Reservation reservation1 = Reservation.builder()
                        .id(1L)
                        .user(mockUser)
                        .employee(employee1)
                        .startTime(LocalDateTime.now().plusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1).plusMinutes(30))
                        .totalPrice(new BigDecimal("50.00"))
                        .status(ReservationStatus.CREATED)
                        .build();

        Reservation reservation2 = Reservation.builder()
                        .id(2L)
                        .user(mockUser)
                        .employee(employee2)
                        .startTime(LocalDateTime.now().plusDays(2))
                        .endTime(LocalDateTime.now().plusDays(2).plusMinutes(45))
                        .totalPrice(new BigDecimal("40.00"))
                        .status(ReservationStatus.CONFIRMED_BY_CLIENT)
                        .build();

        List<Reservation> reservations = Arrays.asList(reservation1, reservation2);
        when(reservationService.getMyReservations(1L)).thenReturn(reservations);

        // When & Then
        mockMvc.perform(get("/client/reservations"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reservations/list"))
                .andExpect(model().attributeExists("reservations"))
                .andExpect(model().attribute("reservations", hasSize(2)))
                .andExpect(model().attribute("reservations", everyItem(
                        instanceOf(Reservation.class))))
                .andExpect(model().attribute("reservations", hasItem(
                        allOf(
                                hasProperty("id", is(1L)),
                                hasProperty("status", is(ReservationStatus.CREATED))
                        ))))
                .andExpect(model().attribute("reservations", hasItem(
                        allOf(
                                hasProperty("id", is(2L)),
                                hasProperty("status", is(ReservationStatus.CONFIRMED_BY_CLIENT))
                        ))));

        verify(reservationService).getMyReservations(1L);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldDisplayEmptyReservationsListWhenNoReservationsExist() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(reservationService.getMyReservations(1L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/client/reservations"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reservations/list"))
                .andExpect(model().attribute("reservations", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldCancelReservationSuccessfully() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Reservation cancelledReservation = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.CANCELLED)
                .build();
        when(reservationService.cancelReservation(eq(1L), any(User.class)))
                .thenReturn(cancelledReservation);

        // When & Then
        mockMvc.perform(post("/client/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reservations"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage", is("Rezerwacja anulowana")));

        verify(reservationService).cancelReservation(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldHandleErrorWhenCancellingReservation() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(reservationService.cancelReservation(eq(1L), any(User.class)))
                .thenThrow(new RuntimeException("Cannot cancel reservation"));

        // When & Then
        mockMvc.perform(post("/client/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reservations"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", is("Cannot cancel reservation")));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldConfirmReservationSuccessfully() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        Reservation confirmedReservation = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.CONFIRMED_BY_CLIENT)
                .build();
        when(reservationService.confirmReservation(1L, 1L))
                .thenReturn(confirmedReservation);

        // When & Then
        mockMvc.perform(post("/client/reservations/1/confirm")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reservations"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage", is("Rezerwacja potwierdzona")));

        verify(reservationService).confirmReservation(1L, 1L);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldHandleErrorWhenConfirmingReservation() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(reservationService.confirmReservation(1L, 1L))
                .thenThrow(new RuntimeException("Cannot confirm reservation"));

        // When & Then
        mockMvc.perform(post("/client/reservations/1/confirm")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reservations"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", is("Cannot confirm reservation")));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void shouldThrowExceptionWhenUserNotFoundForReservations() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/client/reservations"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void shouldHandleErrorWhenUserNotFoundForCancel() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/client/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/reservations"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
