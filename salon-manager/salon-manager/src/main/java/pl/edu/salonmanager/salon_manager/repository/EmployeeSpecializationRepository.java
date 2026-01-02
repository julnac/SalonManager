package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSpecialization;

import java.util.List;

@Repository
public interface EmployeeSpecializationRepository extends JpaRepository<EmployeeSpecialization, Long> {

    List<EmployeeSpecialization> findByEmployeeId(Long employeeId);

    List<EmployeeSpecialization> findByServiceOfferId(Long serviceOfferId);

    List<EmployeeSpecialization> findByEmployeeIdAndServiceOfferId(Long employeeId, Long serviceOfferId);

    @Query("SELECT DISTINCT es.employee FROM EmployeeSpecialization es " +
           "WHERE es.serviceOffer.id IN :serviceIds " +
           "GROUP BY es.employee " +
           "HAVING COUNT(DISTINCT es.serviceOffer.id) = :serviceCount")
    List<Employee> findEmployeesWithAllServices(
        @Param("serviceIds") List<Long> serviceIds,
        @Param("serviceCount") Long serviceCount
    );
}
