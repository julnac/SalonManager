package pl.edu.salonmanager.salon_manager.model.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatisticsDto {
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private Long totalVisits;
    private Integer averageDurationMinutes;
    private BigDecimal averageSpending;
    private BigDecimal totalSpending;
}
