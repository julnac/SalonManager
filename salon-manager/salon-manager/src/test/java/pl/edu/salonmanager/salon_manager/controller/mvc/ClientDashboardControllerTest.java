package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientDashboardController.class)
class ClientDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ReservationService reservationService;

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
    void shouldDisplayDashboard() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // When & Then
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", is(mockUser)))
                .andExpect(model().attribute("user",
                        allOf(
                                hasProperty("email", is("test@example.com")),
                                hasProperty("firstName", is("Jan")),
                                hasProperty("lastName", is("Kowalski"))
                        )));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void shouldThrowExceptionWhenUserNotFound() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().isNotFound());
    }
}
