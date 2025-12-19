package pl.edu.salonmanager.salon_manager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSpecializationDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long serviceId;
    private String serviceName;
    private Integer experienceYears;
}
