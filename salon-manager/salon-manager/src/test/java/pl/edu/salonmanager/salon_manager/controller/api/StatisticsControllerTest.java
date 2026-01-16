package pl.edu.salonmanager.salon_manager.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.statistics.ClientStatisticsDto;
import pl.edu.salonmanager.salon_manager.service.StatisticsService;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    private ClientStatisticsDto statisticsDto;

    @BeforeEach
    void setUp() {
        statisticsDto = new ClientStatisticsDto(
                1L,
                "Jan Kowalski",
                "jan@example.com",
                10L,
                45,
                new BigDecimal("75.00"),
                new BigDecimal("750.00")
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetClientStatistics() throws Exception {
        // Given
        when(statisticsService.getClientStatisticsById(1L)).thenReturn(statisticsDto);

        // When & Then
        mockMvc.perform(get("/api/v1/statistics/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.clientName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.clientEmail").value("jan@example.com"))
                .andExpect(jsonPath("$.totalVisits").value(10))
                .andExpect(jsonPath("$.averageDurationMinutes").value(45))
                .andExpect(jsonPath("$.averageSpending").value(75.00))
                .andExpect(jsonPath("$.totalSpending").value(750.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenClientNotFound() throws Exception {
        // Given
        when(statisticsService.getClientStatisticsById(99L))
                .thenThrow(new ResourceNotFoundException("Client not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/statistics/clients/99"))
                .andExpect(status().isNotFound());
    }
}
