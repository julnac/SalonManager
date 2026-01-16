package pl.edu.salonmanager.salon_manager.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalonController.class)
class SalonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalonProperties salonProperties;

    @BeforeEach
    void setUp() {
        when(salonProperties.getName()).thenReturn("Beauty Salon");
        when(salonProperties.getAddress()).thenReturn("123 Main St");
        when(salonProperties.getPhone()).thenReturn("123-456-789");
        when(salonProperties.getEmail()).thenReturn("contact@salon.pl");
        when(salonProperties.getSlotDurationMinutes()).thenReturn(30);
    }

    @Test
    @WithMockUser
    void shouldGetSalonInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/salon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Beauty Salon"))
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.phone").value("123-456-789"))
                .andExpect(jsonPath("$.email").value("contact@salon.pl"))
                .andExpect(jsonPath("$.slotDurationMinutes").value(30));
    }
}
