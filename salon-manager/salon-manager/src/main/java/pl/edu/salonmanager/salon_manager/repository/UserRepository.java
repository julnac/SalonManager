package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
        SELECT COUNT(r) > 0
        FROM User u
        JOIN u.roles r
        WHERE u.id = :userId
          AND r.name = 'ADMIN'
     """)
    boolean hasAdminRole(@Param("userId") Long userId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findUsersByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT * FROM users u WHERE " +
           "LOWER(u.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           nativeQuery = true)
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

}
