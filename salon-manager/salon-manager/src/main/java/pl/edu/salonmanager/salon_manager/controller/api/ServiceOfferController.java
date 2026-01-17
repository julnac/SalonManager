package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferCsvService;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Services", description = "Service management endpoints")
public class ServiceOfferController {

    private final ServiceOfferService serviceOfferService;
    private final ServiceOfferCsvService csvService;

    // GET /api/v1/services
    @GetMapping
    @Operation(summary = "Get all services (PUBLIC)", description = "Returns list of all available services")
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
    @Operation(summary = "Update service (ADMIN)", description = "Updates an existing service offer")
    public ResponseEntity<ServiceOfferDto> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {

        log.info("REST request to update service: {}", id);

        ServiceOfferDto updated = serviceOfferService.updateService(id, request);
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/v1/services/{id}
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete service (ADMIN)", description = "Deletes a service offer")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        log.info("REST request to delete service: {}", id);

        serviceOfferService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/services/export/csv
    @GetMapping("/export/csv")
    @Operation(summary = "Export services to CSV (ADMIN)", description = "Exports all service offers to CSV file")
    public ResponseEntity<String> exportToCsv() {
        log.info("REST request to export services to CSV");

        String csv = csvService.exportToCsv();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "services.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    // POST /api/v1/services/import/csv
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import services from CSV (ADMIN)", description = "Imports service offers from CSV file")
    public ResponseEntity<List<ServiceOfferDto>> importFromCsv(
            @RequestParam("file") MultipartFile file) {
        log.info("REST request to import services from CSV: {}", file.getOriginalFilename());

        List<ServiceOfferDto> imported = csvService.importFromCsv(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(imported);
    }
}

