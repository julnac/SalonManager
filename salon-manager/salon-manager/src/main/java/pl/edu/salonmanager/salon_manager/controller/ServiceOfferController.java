package pl.edu.salonmanager.salon_manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Services", description = "Service management endpoints")
public class ServiceOfferController {

    private final ServiceOfferService serviceOfferService;

    // GET /api/v1/services
    @GetMapping
    @Operation(summary = "Get all services", description = "Returns list of all available services")
    public ResponseEntity<List<ServiceOfferDto>> getAllServices() {
        log.info("REST request to get all services");
        List<ServiceOfferDto> services = serviceOfferService.getAllServices();
        return ResponseEntity.ok(services);
    }

    // POST /api/v1/services (ADMIN)
    @PostMapping
    @Operation(summary = "Create new service (ADMIN)", description = "Creates a new service offer")
    public ResponseEntity<ServiceOfferDto> createService(@Valid @RequestBody CreateServiceRequest request) {
        log.info("REST request to create service: {}", request.getName());

        ServiceOfferDto created = serviceOfferService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/v1/services/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Update service", description = "Updates an existing service offer")
    public ResponseEntity<ServiceOfferDto> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {

        log.info("REST request to update service: {}", id);

        try {
            ServiceOfferDto updated = serviceOfferService.updateService(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating service: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/v1/services/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service", description = "Deletes a service offer")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        log.info("REST request to delete service: {}", id);

        try {
            serviceOfferService.deleteService(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting service: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

