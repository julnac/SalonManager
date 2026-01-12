package pl.edu.salonmanager.salon_manager.controller.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final SalonProperties salonProperties;
    private final ServiceOfferService serviceOfferService;

    @GetMapping("/")
    public String index(Model model) {
        log.debug("Loading public homepage");
        model.addAttribute("salon", salonProperties);
        return "public/index";
    }

    @GetMapping("/services")
    public String services(Model model) {
        log.debug("Loading services page");
        model.addAttribute("salon", salonProperties);
        model.addAttribute("services", serviceOfferService.getAllServices());
        return "public/services";
    }
}
