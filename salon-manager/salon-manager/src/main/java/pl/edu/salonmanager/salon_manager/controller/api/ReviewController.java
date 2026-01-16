package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all reviews (PUBLIC)", description = "Returns list of all reviews")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        log.info("REST request to get all reviews");
        List<ReviewDto> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID (PUBLIC)", description = "Returns a single review by ID")
    public ResponseEntity<ReviewDto> getReviewById(@PathVariable Long id) {
        log.info("REST request to get review: {}", id);
        ReviewDto review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    @PostMapping
    @Operation(summary = "Create new review (USER)", description = "Creates a new review")
    public ResponseEntity<ReviewDto> createReview(@Valid @RequestBody CreateReviewRequest request) {
        log.info("REST request to create review");
        ReviewDto created = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review (USER, owner only)", description = "Deletes a review")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        log.info("REST request to delete review: {}", id);
        Long userId = getCurrentUserId();
        reviewService.deleteReview(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Add/update image to review (USER, owner only)", description = "Adds or updates an image to a review")
    public ResponseEntity<ReviewDto> addImageToReview(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        log.info("REST request to add image to review: {}", id);
        Long userId = getCurrentUserId();
        ReviewDto updated = reviewService.addImageToReview(id, image, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get review image (PUBLIC)", description = "Returns the image of a review")
    public ResponseEntity<Resource> getReviewImage(@PathVariable Long id) {
        log.info("REST request to get image for review: {}", id);
        Resource resource = reviewService.getReviewImage(id);

        String contentType;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
        } catch (IOException ex) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                       "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Delete review image (USER, owner only)", description = "Deletes the image from a review")
    public ResponseEntity<Void> deleteReviewImage(@PathVariable Long id) {
        log.info("REST request to delete image from review: {}", id);
        Long userId = getCurrentUserId();
        reviewService.deleteReviewImage(id, userId);
        return ResponseEntity.noContent().build();
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
}
