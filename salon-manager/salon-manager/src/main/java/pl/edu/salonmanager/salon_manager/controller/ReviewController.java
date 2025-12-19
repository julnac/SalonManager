package pl.edu.salonmanager.salon_manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get all reviews", description = "Returns list of all reviews")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        log.info("REST request to get all reviews");
        List<ReviewDto> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID", description = "Returns a single review by ID")
    public ResponseEntity<ReviewDto> getReviewById(@PathVariable Long id) {
        log.info("REST request to get review: {}", id);
        ReviewDto review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    @PostMapping
    @Operation(summary = "Create new review", description = "Creates a new review")
    public ResponseEntity<ReviewDto> createReview(@Valid @RequestBody CreateReviewRequest request) {
        log.info("REST request to create review");
        ReviewDto created = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Deletes a review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        log.info("REST request to delete review: {}", id);
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
