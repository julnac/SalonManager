package pl.edu.salonmanager.salon_manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;
import pl.edu.salonmanager.salon_manager.service.StatisticsService;

import java.util.Collections;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final StatisticsService statisticsService;
    private final SalonProperties salonProperties;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        log.debug("Loading admin dashboard");

        // Add salon information
        model.addAttribute("salon", salonProperties);

        try {
            model.addAttribute("reservationStats", statisticsService.getReservationCountByStatus());
        } catch (Exception e) {
            log.error("Error loading reservation statistics", e);
            model.addAttribute("reservationStats", Collections.emptyList());
        }

        try {
            model.addAttribute("revenueStats", statisticsService.getReservationRevenueByStatus());
        } catch (Exception e) {
            log.error("Error loading revenue statistics", e);
            model.addAttribute("revenueStats", Collections.emptyList());
        }

        try {
            model.addAttribute("busiestSlots", statisticsService.getBusiestTimeSlots());
        } catch (Exception e) {
            log.error("Error loading busiest slots", e);
            model.addAttribute("busiestSlots", Collections.emptyList());
        }

        return "admin/dashboard";
    }
}
