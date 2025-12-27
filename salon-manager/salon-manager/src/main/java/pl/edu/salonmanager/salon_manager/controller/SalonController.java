package pl.edu.salonmanager.salon_manager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;

@RestController
@RequestMapping("/api/v1/salon")
@RequiredArgsConstructor
public class SalonController {

    private final SalonProperties salonProperties;

    @GetMapping
    public SalonProperties getSalonInfo() {
        return salonProperties;
    }
}
