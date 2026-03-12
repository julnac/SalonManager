package pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeScheduleRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    @AssertTrue(message = "Start time and end time must be both set or both null")
    public boolean isValidWorkingDay() {
        return (startTime == null && endTime == null)
                || (startTime != null && endTime != null);
    }
}
