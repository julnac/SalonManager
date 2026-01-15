package pl.edu.salonmanager.salon_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class AppConfig {

    @Bean
    public DateTimeFormatter dateFormatter() {
        System.out.println("Creating DateTimeFormatter bean...");
        return DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Bean
    public String appName() {
        return "Salon Manager";
    }
}
