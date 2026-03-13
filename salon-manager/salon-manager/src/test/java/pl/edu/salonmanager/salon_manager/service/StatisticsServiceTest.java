package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.dao.StatisticsDao;

import pl.edu.salonmanager.salon_manager.model.dto.statistics.ClientStatisticsDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StatisticsDao statisticsDao;

    @InjectMocks
    private StatisticsService statisticsService;

    private ClientStatisticsDto sampleStats;

    @BeforeEach
    void setUp() {
        sampleStats = new ClientStatisticsDto(
                1L,
                "Jan Kowalski",
                "jan.kowalski@example.com",
                10L, // totalVisits
                60, // averageDurationMinutes
                new BigDecimal("150.00"), // averageSpending
                new BigDecimal("1500.00") // totalSpending
        );
    }

    @Test
    void shouldReturnAllClientStatistics() {
        // Given
        when(statisticsDao.getClientStatistics()).thenReturn(List.of(sampleStats));

        // When
        List<ClientStatisticsDto> result = statisticsService.getClientStatistics();

        // Then
        assertThat(result).hasSize(1);
        ClientStatisticsDto dto = result.get(0);
        assertThat(dto.getClientId()).isEqualTo(1L);
        assertThat(dto.getClientName()).isEqualTo("Jan Kowalski");
        assertThat(dto.getTotalSpending()).isEqualByComparingTo("1500.00");
        verify(statisticsDao).getClientStatistics();
    }

    @Test
    void shouldReturnClientStatisticsById() {
        // Given
        Long id = 1L;
        when(statisticsDao.getClientStatisticsById(id)).thenReturn(Optional.of(sampleStats));

        // When
        ClientStatisticsDto result = statisticsService.getClientStatisticsById(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClientEmail()).isEqualTo("jan.kowalski@example.com");
        assertThat(result.getAverageSpending()).isEqualByComparingTo("150.00");
        verify(statisticsDao).getClientStatisticsById(id);
    }

    @Test
    void shouldReturnNullWhenClientHasNoStats() {
        // Given
        Long id = 99L;
        when(statisticsDao.getClientStatisticsById(id)).thenReturn(Optional.empty());

        // When
        ClientStatisticsDto result = statisticsService.getClientStatisticsById(id);

        // Then
        assertThat(result).isNull();
        verify(statisticsDao).getClientStatisticsById(id);
    }
}
