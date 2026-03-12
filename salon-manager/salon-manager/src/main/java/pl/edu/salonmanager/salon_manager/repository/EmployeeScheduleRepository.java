package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSchedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, Long> {

    List<EmployeeSchedule> findByEmployeeId(Long employeeId);

    Optional<EmployeeSchedule> findByEmployeeIdAndDayOfWeek(Long employeeId, DayOfWeek dayOfWeek);
    Optional<EmployeeSchedule> findByEmployeeAndDayOfWeek(Employee employee, DayOfWeek dayOfWeek);

    List<EmployeeSchedule> findByEmployeeIdInAndDayOfWeek(List<Long> employeeIds, DayOfWeek dayOfWeek);

    boolean existsByEmployeeAndDayOfWeek(Employee employee, DayOfWeek dayOfWeek);
}
