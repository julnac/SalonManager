package pl.edu.salonmanager.salon_manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

@Controller
@RequestMapping("/admin/services")
@RequiredArgsConstructor
@Slf4j
public class AdminServiceController {

    private final ServiceOfferService serviceOfferService;

    @GetMapping
    public String listServices(Model model) {
        log.debug("Listing all services");
        model.addAttribute("services", serviceOfferService.getAllServices());
        return "admin/services/list";
    }
}
