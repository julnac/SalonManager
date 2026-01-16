package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Review;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.ReviewRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviews() {
        log.debug("Fetching all reviews");
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewDto getReviewById(Long id) {
        log.debug("Fetching review with id: {}", id);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return mapToDto(review);
    }

    @Transactional
    public ReviewDto createReview(CreateReviewRequest request) {
        log.debug("Creating new review for user id: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Review review = new Review();
        review.setContent(request.getContent());
        review.setUser(user);

        Review saved = reviewRepository.save(review);
        log.info("Review created successfully with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public void deleteReview(Long id, Long currentUserId) {
        log.debug("Deleting review with id: {} by user {}", id, currentUserId);

        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        if (!review.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        if (review.getImageFilename() != null) {
            try {
                fileStorageService.deleteFile(review.getImageFilename());
                log.info("Deleted image file: {}", review.getImageFilename());
            } catch (Exception e) {
                log.warn("Failed to delete image file: {}", review.getImageFilename(), e);
            }
        }

        reviewRepository.deleteById(id);
        log.info("Review deleted successfully with id: {}", id);
    }

    @Transactional
    public ReviewDto addImageToReview(Long reviewId, MultipartFile image, Long currentUserId) {
        log.debug("Adding image to review {} by user {}", reviewId, currentUserId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can only add images to your own reviews");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image");
        }

        List<String> allowedMimeTypes = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif"
        );
        if (!allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Allowed image types: JPEG, PNG, GIF");
        }

        if (review.getImageFilename() != null) {
            try {
                fileStorageService.deleteFile(review.getImageFilename());
                log.info("Deleted old image: {}", review.getImageFilename());
            } catch (Exception e) {
                log.warn("Failed to delete old image: {}", review.getImageFilename(), e);
            }
        }

        String filename = fileStorageService.storeFile(image, "review_" + reviewId);

        review.setImageFilename(filename);
        Review saved = reviewRepository.save(review);

        log.info("Image added to review {} with filename: {}", reviewId, filename);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Resource getReviewImage(Long reviewId) {
        log.debug("Fetching image for review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (review.getImageFilename() == null) {
            throw new ResourceNotFoundException("Review does not have an image");
        }

        Path filePath = fileStorageService.loadFile(review.getImageFilename());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Image file not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading image file", e);
        }
    }

    @Transactional
    public void deleteReviewImage(Long reviewId, Long currentUserId) {
        log.debug("Deleting image from review {} by user {}", reviewId, currentUserId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can only delete images from your own reviews");
        }

        if (review.getImageFilename() == null) {
            throw new BadRequestException("Review does not have an image");
        }

        String filename = review.getImageFilename();
        fileStorageService.deleteFile(filename);

        review.setImageFilename(null);
        reviewRepository.save(review);

        log.info("Image deleted from review {}: {}", reviewId, filename);
    }

    private ReviewDto mapToDto(Review entity) {
        String imageUrl = null;
        if (entity.getImageFilename() != null) {
            imageUrl = "/api/v1/reviews/" + entity.getId() + "/image";
        }

        return new ReviewDto(
                entity.getId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUser().getId(),
                entity.getUser().getFirstName() + " " + entity.getUser().getLastName(),
                imageUrl
        );
    }
}
