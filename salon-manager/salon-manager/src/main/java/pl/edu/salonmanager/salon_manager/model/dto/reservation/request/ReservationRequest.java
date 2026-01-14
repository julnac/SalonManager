package pl.edu.salonmanager.salon_manager.model.dto.reservation.request;

import java.time.LocalDateTime;
import java.util.Set;

public interface ReservationRequest {
    LocalDateTime getStartTime();
    Long getEmployeeId();
    Set<Long> getServiceIds();
}
