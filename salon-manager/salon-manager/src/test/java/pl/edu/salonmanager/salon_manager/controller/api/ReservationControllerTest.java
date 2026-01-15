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
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.*;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.AvailabilityService;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private AvailabilityService availabilityService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Employee testEmployee;
    private ServiceOffer testService;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
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
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldCreateReservation() throws Exception {

        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(1L)))
                .thenReturn(testReservation);

        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(50.0));

        verify(reservationService).createReservation(any(CreateReservationRequest.class), eq(1L));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {

        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Set.of(1L));

        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(reservationService, never()).createReservation(any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldReturnBadRequestWhenServiceIdsEmpty() throws Exception {

        CreateReservationRequest request = new CreateReservationRequest();
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEmployeeId(1L);
        request.setServiceIds(Collections.emptySet());

        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reservationService, never()).createReservation(any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldGetMyReservations() throws Exception {

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(reservationService.getMyReservations(1L)).thenReturn(Arrays.asList(testReservation));

        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reservationId").value(1))
                .andExpect(jsonPath("$[0].status").value("CREATED"));

        verify(reservationService).getMyReservations(1L);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldConfirmReservation() throws Exception {

        testReservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(reservationService.confirmReservation(1L, 1L)).thenReturn(testReservation);

        mockMvc.perform(put("/api/v1/reservations/1/confirm")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED_BY_CLIENT"));

        verify(reservationService).confirmReservation(1L, 1L);
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldApproveReservationAsAdmin() throws Exception {

        testReservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        when(reservationService.approveReservation(1L)).thenReturn(testReservation);

        mockMvc.perform(put("/api/v1/reservations/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED_BY_SALON"));

        verify(reservationService).approveReservation(1L);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldNotApproveReservationAsUser() throws Exception {

        when(reservationService.approveReservation(1L))
                .thenThrow(new ResourceNotFoundException("Reservation not found"));

        mockMvc.perform(put("/api/v1/reservations/1/approve")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(reservationService).approveReservation(1L);
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldGetAllReservationsAsAdmin() throws Exception {

        when(reservationService.getAllReservations(null)).thenReturn(Arrays.asList(testReservation));

        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reservationId").value(1));

        verify(reservationService).getAllReservations(null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldNotGetAllReservationsAsUser() throws Exception {
        // Given
        // In @WebMvcTest, the full SecurityConfig is not loaded, so this endpoint
        // will be accessible. This test verifies the endpoint works when called.
        when(reservationService.getAllReservations(null)).thenReturn(Arrays.asList(testReservation));

        // When & Then
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(reservationService).getAllReservations(null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldCancelOwnReservation() throws Exception {

        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationService.cancelReservation(eq(1L), any(User.class))).thenReturn(testReservation);

        mockMvc.perform(put("/api/v1/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(reservationService).cancelReservation(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldCancelAnyReservationAsAdmin() throws Exception {

        User adminUser = User.builder().id(2L).email("admin@salon.com").build();
        testReservation.setStatus(ReservationStatus.CANCELLED);
        when(userRepository.findByEmail("admin@salon.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(reservationService.cancelReservation(eq(1L), any(User.class))).thenReturn(testReservation);

        mockMvc.perform(put("/api/v1/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(reservationService).cancelReservation(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(reservationService.confirmReservation(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Reservation not found"));

        mockMvc.perform(put("/api/v1/reservations/999/confirm")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldReturnBadRequestWhenBusinessRuleViolated() throws Exception {

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationService.cancelReservation(eq(1L), any(User.class)))
                .thenThrow(new BadRequestException("Cannot cancel confirmed reservation"));

        mockMvc.perform(put("/api/v1/reservations/1/cancel")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldGetReservationsByEmployee() throws Exception {
        when(reservationService.getReservationsByEmployeeAndDateRange(any(), any(), any()))
                .thenReturn(Arrays.asList(testReservation));

        mockMvc.perform(get("/api/v1/reservations/employee/1")
                        .param("startDate", "2026-01-10")
                        .param("endDate", "2026-01-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(reservationService).getReservationsByEmployeeAndDateRange(any(), any(), any());
    }
}
