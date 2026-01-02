package pl.edu.salonmanager.salon_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.request.ServiceOfferFormDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.response.ServiceOfferDto;
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

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.debug("Showing create service form");
        model.addAttribute("serviceForm", new ServiceOfferFormDto());
        model.addAttribute("isEdit", false);
        return "admin/services/form";
    }

    @PostMapping("/new")
    public String createService(
            @Valid @ModelAttribute("serviceForm") ServiceOfferFormDto formDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Creating new service: {}", formDto.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/services/form";
        }

        try {
            CreateServiceRequest request = new CreateServiceRequest();
            request.setName(formDto.getName());
            request.setPrice(formDto.getPrice());
            request.setDurationMinutes(formDto.getDurationMinutes());

            serviceOfferService.createService(request);
            redirectAttributes.addFlashAttribute("successMessage", "Usługa została dodana pomyślnie");
            return "redirect:/admin/services";
        } catch (Exception e) {
            log.error("Error creating service", e);
            model.addAttribute("errorMessage", "Wystąpił błąd podczas dodawania usługi");
            model.addAttribute("isEdit", false);
            return "admin/services/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for service: {}", id);

        try {
            ServiceOfferDto service = serviceOfferService.getServiceById(id);

            ServiceOfferFormDto formDto = new ServiceOfferFormDto();
            formDto.setId(service.getId());
            formDto.setName(service.getName());
            formDto.setPrice(service.getPrice());
            formDto.setDurationMinutes(service.getDurationMinutes());

            model.addAttribute("serviceForm", formDto);
            model.addAttribute("isEdit", true);
            return "admin/services/form";
        } catch (ResourceNotFoundException e) {
            log.error("Service not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Usługa nie została znaleziona");
            return "redirect:/admin/services";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateService(
            @PathVariable Long id,
            @Valid @ModelAttribute("serviceForm") ServiceOfferFormDto formDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating service: {}", id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/services/form";
        }

        try {
            UpdateServiceRequest request = new UpdateServiceRequest();
            request.setName(formDto.getName());
            request.setPrice(formDto.getPrice());
            request.setDurationMinutes(formDto.getDurationMinutes());

            serviceOfferService.updateService(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Usługa została zaktualizowana");
            return "redirect:/admin/services";
        } catch (ResourceNotFoundException e) {
            log.error("Service not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Usługa nie została znaleziona");
            return "redirect:/admin/services";
        } catch (Exception e) {
            log.error("Error updating service", e);
            model.addAttribute("errorMessage", "Wystąpił błąd podczas aktualizacji usługi");
            model.addAttribute("isEdit", true);
            return "admin/services/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Deleting service: {}", id);

        try {
            serviceOfferService.deleteService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Usługa została usunięta");
        } catch (ResourceNotFoundException e) {
            log.error("Service not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Usługa nie została znaleziona");
        } catch (Exception e) {
            log.error("Error deleting service", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Nie można usunąć usługi - może być używana w rezerwacjach");
        }

        return "redirect:/admin/services";
    }
}
