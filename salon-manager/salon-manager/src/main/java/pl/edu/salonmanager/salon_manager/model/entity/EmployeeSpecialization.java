package pl.edu.salonmanager.salon_manager.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_specializations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSpecialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offer_id", nullable = false)
    private ServiceOffer serviceOffer;

    @Column(name = "experience_years")
    private Integer experienceYears;
}
