package pl.edu.salonmanager.salon_manager.model.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UpdateReservationAdminRequest {

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotEmpty(message = "At least one service must be selected")
    private Set<Long> serviceIds;
}
