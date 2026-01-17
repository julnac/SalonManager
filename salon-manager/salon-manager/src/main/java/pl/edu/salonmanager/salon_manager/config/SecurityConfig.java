package pl.edu.salonmanager.salon_manager.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // Publiczne GET endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/employees/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/availability").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/salon/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/employee-specializations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()

                        // ADMIN tylko
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/employee/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/employees/*/schedule").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/statistics/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/export/csv").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/import/csv").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/employee-specializations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/employee-specializations/**").hasRole("ADMIN")

                        // USER (zalogowany) - rezerwacje i opinie
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/my").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservations").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/confirm").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasRole("USER")

                        // Reszta wymaga uwierzytelnienia
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                );

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/login-success", true)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                .authorizeHttpRequests(auth -> auth
                        // Publiczne strony
                        .requestMatchers("/", "/services", "/reviews",
                                "/booking", "/register", "/login", "/error").permitAll()
                        .requestMatchers("/webjars/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Chronione endpointy
                        .requestMatchers("/client/**").hasRole("USER")

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
