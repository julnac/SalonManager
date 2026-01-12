package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Get Salon info (PUBLIC)", description = "Get Salon info")
    public SalonProperties getSalonInfo() {
        return salonProperties;
    }
}
