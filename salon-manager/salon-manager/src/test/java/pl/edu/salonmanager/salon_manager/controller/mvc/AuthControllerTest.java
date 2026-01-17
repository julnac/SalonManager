package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.model.entity.Role;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.RoleRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
    }

    @Test
    
    void shouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("errorMessage"))
                .andExpect(model().attributeDoesNotExist("successMessage"));
    }

    @Test
    
    void shouldDisplayLoginPageWithErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", is("Nieprawidłowy email lub hasło")));
    }

    @Test
    
    void shouldDisplayLoginPageWithLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("successMessage"))
                .andExpect(model().attribute("successMessage", is("Zostałeś wylogowany")));
    }

    @Test
    
    void shouldDisplayRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerForm"))
                .andExpect(model().attribute("registerForm",
                        instanceOf(AuthController.RegisterForm.class)));
    }

    @Test
    
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage",
                        is("Rejestracja zakończona sukcesem! Możesz się teraz zalogować.")));

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    
    void shouldRejectRegistrationWhenPasswordsDontMatch() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "differentPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", is("Hasła nie są identyczne")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    
    void shouldRejectRegistrationWhenEmailAlreadyExists() throws Exception {
        // Given
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "existing@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage",
                        is("Użytkownik z tym adresem email już istnieje")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    
    void shouldRejectRegistrationWithValidationErrors() throws Exception {
        // When & Then - brak firstName
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "Kowalski")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "invalid-email")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    
    void shouldRejectRegistrationWithShortPassword() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "test@example.com")
                        .param("password", "123")
                        .param("confirmPassword", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    
    void shouldHandleRegistrationErrorWhenRoleNotFound() throws Exception {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage",
                        is("Wystąpił błąd podczas rejestracji")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRedirectAdminToAdminDashboardAfterLogin() throws Exception {
        // Given
        Authentication auth = createMockAuthentication("admin@example.com", "ROLE_ADMIN");

        // When & Then
        String result = new AuthController(userRepository, roleRepository, passwordEncoder)
                .loginSuccess(auth);

        assert result.equals("redirect:/admin/dashboard");
    }

    @Test
    void shouldRedirectUserToClientDashboardAfterLogin() throws Exception {
        // Given
        Authentication auth = createMockAuthentication("user@example.com", "ROLE_USER");

        // When & Then
        String result = new AuthController(userRepository, roleRepository, passwordEncoder)
                .loginSuccess(auth);

        assert result.equals("redirect:/client/dashboard");
    }

    @Test
    void shouldRedirectToHomePageWhenNoRoleMatches() throws Exception {
        // Given
        Authentication auth = createMockAuthentication("user@example.com", "ROLE_UNKNOWN");

        // When & Then
        String result = new AuthController(userRepository, roleRepository, passwordEncoder)
                .loginSuccess(auth);

        assert result.equals("redirect:/");
    }

    private Authentication createMockAuthentication(String email, String role) {
        return new Authentication() {
            @Override
            public String getName() {
                return email;
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return null;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
    }
}
