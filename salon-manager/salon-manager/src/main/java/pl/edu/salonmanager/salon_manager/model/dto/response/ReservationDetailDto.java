package pl.edu.salonmanager.salon_manager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailDto {
    private Long reservationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private String clientFirstName;
    private String clientLastName;
    private String clientEmail;
    private String employeeFirstName;
    private String employeeLastName;
}
