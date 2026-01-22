package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User disabledUser;
    private final String email = "blocked@example.com";

    @BeforeEach
    void setUp() {
        disabledUser = new User();
        disabledUser.setEmail(email);
        disabledUser.setPassword("encodedPassword");
        disabledUser.setEnabled(false);
        disabledUser.setRoles(Set.of());
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserIsDisabled() {
        // Given
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User account is disabled: " + email);

        // Verify
        verify(userRepository).findByEmail(email);
        // czy nie próbuje mapować ról lub tworzyć obiektu UserDetails po rzuceniu wyjątku
        verifyNoMoreInteractions(userRepository);
    }
}