package pl.edu.salonmanager.salon_manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.edu.salonmanager.salon_manager.model.entity.Role;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.RoleRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Starting data initialization...");

        initializeRoles();
        initializeAdminUser();

        log.info("Data initialization completed");
    }

    private void initializeRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .build();
            roleRepository.save(adminRole);
            log.info("Created role: ADMIN");
        }

        if (!roleRepository.existsByName("USER")) {
            Role userRole = Role.builder()
                    .name("USER")
                    .build();
            roleRepository.save(userRole);
            log.info("Created role: USER");
        }
    }

    private void initializeAdminUser() {
        if (!userRepository.existsByEmail("admin@salon.pl")) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

            User admin = User.builder()
                    .email("admin@salon.pl")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("Administrator")
                    .enabled(true)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);
            log.info("Created default admin user: admin@salon.pl / admin123");
        }
    }
}
