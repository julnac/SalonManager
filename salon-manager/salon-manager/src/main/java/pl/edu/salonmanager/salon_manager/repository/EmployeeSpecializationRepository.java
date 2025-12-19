package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSpecialization;

import java.util.List;

@Repository
public interface EmployeeSpecializationRepository extends JpaRepository<EmployeeSpecialization, Long> {

    List<EmployeeSpecialization> findByEmployeeId(Long employeeId);

    List<EmployeeSpecialization> findByServiceOfferId(Long serviceOfferId);

    List<EmployeeSpecialization> findByEmployeeIdAndServiceOfferId(Long employeeId, Long serviceOfferId);
}
