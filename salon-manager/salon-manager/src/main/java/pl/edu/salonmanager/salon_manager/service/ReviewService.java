package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Review;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.ReviewRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

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
    public void deleteReview(Long id) {
        log.debug("Deleting review with id: {}", id);

        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Review not found with id: " + id);
        }

        reviewRepository.deleteById(id);
        log.info("Review deleted successfully with id: {}", id);
    }

    // Mapper: Entity → DTO
    private ReviewDto mapToDto(Review entity) {
        return new ReviewDto(
                entity.getId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUser().getId(),
                entity.getUser().getFirstName() + " " + entity.getUser().getLastName()
        );
    }
}
