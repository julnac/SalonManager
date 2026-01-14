package pl.edu.salonmanager.salon_manager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI salonManagerOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Salon Manager Team");
        contact.setEmail("contact@salonmanager.pl");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Salon Manager API")
                .version("1.0")
                .description("REST API dla systemu rezerwacji salonu beauty\n\n" +
                        "**Uwierzytelnianie:**\n" +
                        "- Publiczne GET endpoints nie wymagają autoryzacji\n" +
                        "- Pozostałe endpointy wymagają HTTP Basic Auth\n" +
                        "- ADMIN role: admin@salon.pl / admin123\n" +
                        "- USER role: zobacz DataInitializer dla przykładowych użytkowników lub zarejestruj się")
                .contact(contact)
                .license(license);

        SecurityScheme basicAuthScheme = new SecurityScheme()
                .name("basicAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("HTTP Basic Authentication - użyj email i hasła");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("basicAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", basicAuthScheme))
                .addSecurityItem(securityRequirement);
    }
}
