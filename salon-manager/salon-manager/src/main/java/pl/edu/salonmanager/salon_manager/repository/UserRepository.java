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

    // @Query przykład - JPQL z JOIN przez relację
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findUsersByRoleName(@Param("roleName") String roleName);

    // @Query przykład - Native SQL z LIKE
    @Query(value = "SELECT * FROM users u WHERE " +
           "LOWER(u.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           nativeQuery = true)
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Paginacja dla istniejących metod
    Page<User> findByEnabled(boolean enabled, Pageable pageable);
}
