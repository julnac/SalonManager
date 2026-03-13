package pl.edu.salonmanager.salon_manager.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAccessPublicEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ForProtectedEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldAccessUserEndpointsWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldReturn403ForAdminEndpointsWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/reservations/1/approve"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldAccessAdminEndpointsWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAuthenticateWithHttpBasicAuth() throws Exception {

        String credentials = "admin@salon.pl:admin123";
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        mockMvc.perform(get("/api/v1/reservations")
                        .header("Authorization", "Basic " + base64Credentials))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithInvalidCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/reservations")
                        .with(httpBasic("invalid@user.com", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    // MVC endpoints
    @Test
    void shouldRedirectToLoginForMvcEndpoints() throws Exception {
        mockMvc.perform(get("/admin/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldRequireCsrfForMvcPosts() throws Exception {
        mockMvc.perform(post("/admin/services")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isForbidden()); // CSRF token missing
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldNotRequireCsrfForApiPosts() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"price\":50,\"durationMinutes\":30}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldAllowAdminToAccessUserEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldAllowUserToAccessOwnReservations() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@salon.com", roles = {"ADMIN"})
    void shouldAllowAdminToAccessStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/clients/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    void shouldDenyUserAccessToStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/clients/1"))
                .andExpect(status().isForbidden());
    }
}
