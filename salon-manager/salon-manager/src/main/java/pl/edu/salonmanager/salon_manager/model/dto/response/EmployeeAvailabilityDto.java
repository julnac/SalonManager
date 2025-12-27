package pl.edu.salonmanager.salon_manager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAvailabilityDto {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private List<LocalTime> availableSlots;
}
