package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.statistics.ClientStatisticsDto;
import pl.edu.salonmanager.salon_manager.service.StatisticsService;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "Client statistics endpoints")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/clients/{id}")
    @Operation(summary = "Get client statistics by ID (ADMIN)",
               description = "Returns statistics for a specific client including visit count, duration, and spending")
    public ResponseEntity<ClientStatisticsDto> getClientStatisticsById(@PathVariable Long id) {
        log.info("REST request to get statistics for client: {}", id);
        ClientStatisticsDto statistics = statisticsService.getClientStatisticsById(id);
        return ResponseEntity.ok(statistics);
    }
}
