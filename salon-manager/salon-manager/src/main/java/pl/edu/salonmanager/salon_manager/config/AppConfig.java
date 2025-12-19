package pl.edu.salonmanager.salon_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class AppConfig {

    // This method creates a bean that can be injected elsewhere
    @Bean
    public DateTimeFormatter dateFormatter() {
        System.out.println("Creating DateTimeFormatter bean...");
        return DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    // Another example: creating a custom object as a bean
    @Bean
    public String appName() {
        return "Salon Manager";
    }
}
