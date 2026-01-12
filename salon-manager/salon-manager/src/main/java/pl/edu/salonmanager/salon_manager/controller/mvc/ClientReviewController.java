package pl.edu.salonmanager.salon_manager.controller.mvc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

@Controller
@RequestMapping("/client/reviews")
@RequiredArgsConstructor
@Slf4j
public class ClientReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/add")
    public String showAddReviewForm(Model model) {
        model.addAttribute("reviewForm", new ReviewForm());
        return "client/reviews/add";
    }

    @PostMapping("/add")
    public String addReview(
            @Valid @ModelAttribute("reviewForm") ReviewForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "client/reviews/add";
        }

        try {
            Long userId = getCurrentUserId();
            log.debug("Adding review for user ID: {}", userId);

            CreateReviewRequest request = new CreateReviewRequest();
            request.setContent(form.getContent());
            request.setUserId(userId);

            reviewService.createReview(request);

            redirectAttributes.addFlashAttribute("successMessage", "Dziękujemy za dodanie opinii!");
            return "redirect:/reviews";
        } catch (Exception e) {
            log.error("Error adding review: {}", e.getMessage());
            model.addAttribute("errorMessage", "Wystąpił błąd podczas dodawania opinii");
            return "client/reviews/add";
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return user.getId();
    }

    @Data
    public static class ReviewForm {
        @NotBlank(message = "Treść opinii jest wymagana")
        @Size(min = 10, max = 1000, message = "Opinia musi mieć od 10 do 1000 znaków")
        private String content;
    }
}
