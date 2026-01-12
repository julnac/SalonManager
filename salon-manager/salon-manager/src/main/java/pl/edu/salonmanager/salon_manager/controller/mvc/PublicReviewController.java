package pl.edu.salonmanager.salon_manager.controller.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.repository.ReviewRepository;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class PublicReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public String listReviews(Model model) {
        log.debug("Loading reviews list");
        model.addAttribute("reviews", reviewService.getAllReviews());
        return "public/reviews/list";
    }
}
