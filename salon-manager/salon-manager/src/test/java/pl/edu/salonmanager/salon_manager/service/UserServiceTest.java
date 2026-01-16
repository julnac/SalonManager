package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.user.response.UserDto;
import pl.edu.salonmanager.salon_manager.model.dto.user.request.UserRegistrationDto;
import pl.edu.salonmanager.salon_manager.model.entity.Role;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.RoleRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto registrationDto;
    private Role userRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("Jan");
        registrationDto.setLastName("Kowalski");

        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

        savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .firstName("Jan")
                .lastName("Kowalski")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
    }

    // ========== registerUser Tests ==========

    @Test
    void shouldRegisterNewUser() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDto result = userService.registerUser(registrationDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jan");
        assertThat(result.getLastName()).isEqualTo("Kowalski");

        verify(userRepository).existsByEmail("test@example.com");
        verify(roleRepository).findByName("USER");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User with email test@example.com already exists");
    }

    @Test
    void shouldThrowExceptionWhenUserRoleNotFound() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role USER not found");
    }

    @Test
    void shouldEncodePasswordDuringRegistration() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldSetEnabledToTrueDuringRegistration() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getEnabled()).isTrue();
            return savedUser;
        });

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldAssignUserRoleDuringRegistration() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getRoles()).contains(userRole);
            return savedUser;
        });

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(userRepository).save(any(User.class));
    }
}
