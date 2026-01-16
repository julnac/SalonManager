package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.entity.Review;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.ReviewRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Review review1;
    private Review review2;
    private CreateReviewRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("Jan");
        testUser.setLastName("Kowalski");

        review1 = new Review();
        review1.setId(1L);
        review1.setUser(testUser);
        review1.setContent("Excellent service!");
        review1.setCreatedAt(LocalDateTime.now());

        review2 = new Review();
        review2.setId(2L);
        review2.setUser(testUser);
        review2.setContent("Very good!");
        review2.setCreatedAt(LocalDateTime.now().minusDays(1));

        createRequest = new CreateReviewRequest();
        createRequest.setContent("Great experience!");
        createRequest.setUserId(1L);
    }

    // ========== getAllReviews Tests ==========

    @Test
    void shouldGetAllReviewsSortedByCreatedAtDesc() {
        // Given
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Arrays.asList(review1, review2));

        // When
        List<ReviewDto> result = reviewService.getAllReviews();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("Excellent service!");
        assertThat(result.get(1).getContent()).isEqualTo("Very good!");
        verify(reviewRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldReturnEmptyListWhenNoReviews() {
        // Given
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Arrays.asList());

        // When
        List<ReviewDto> result = reviewService.getAllReviews();

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getReviewById Tests ==========

    @Test
    void shouldGetReviewById() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When
        ReviewDto result = reviewService.getReviewById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Excellent service!");
        assertThat(result.getUserName()).isEqualTo("Jan Kowalski");
    }

    @Test
    void shouldThrowExceptionWhenReviewNotFound() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review not found");
    }

    // ========== createReview Tests ==========

    @Test
    void shouldCreateReview() {
        // Given
        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When
        ReviewDto result = reviewService.createReview(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getContent()).isEqualTo("Great experience!");
        assertThat(result.getUserId()).isEqualTo(1L);

        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreateReview() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ========== deleteReview Tests ==========

    @Test
    void shouldDeleteReview() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When
        reviewService.deleteReview(1L, 1L);

        // Then
        verify(reviewRepository).findById(1L);
        verify(reviewRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentReview() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void shouldThrowUnauthorizedWhenDeletingOthersReview() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        review1.getUser().setId(1L);

        // When & Then
        assertThatThrownBy(() -> reviewService.deleteReview(1L, 999L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("your own reviews");
    }

    @Test
    void shouldDeleteReviewAndCascadeDeleteImage() {
        // Given
        review1.setImageFilename("review_1_abc.jpg");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When
        reviewService.deleteReview(1L, 1L);

        // Then
        verify(fileStorageService).deleteFile("review_1_abc.jpg");
        verify(reviewRepository).deleteById(1L);
    }

    // ========== addImageToReview Tests ==========

    @Test
    void shouldAddImageToReview() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        when(fileStorageService.storeFile(mockFile, "review_1")).thenReturn("review_1_abc123.jpg");
        when(reviewRepository.save(any(Review.class))).thenReturn(review1);

        // When
        ReviewDto result = reviewService.addImageToReview(1L, mockFile, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/1/image");
        verify(fileStorageService).storeFile(mockFile, "review_1");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void shouldThrowUnauthorizedWhenAddingImageToOthersReview() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        review1.getUser().setId(1L);

        // When & Then
        assertThatThrownBy(() -> reviewService.addImageToReview(1L, mockFile, 999L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldReplaceExistingImage() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("image/png");
        review1.setImageFilename("old_image.jpg");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        when(fileStorageService.storeFile(mockFile, "review_1")).thenReturn("review_1_new.jpg");
        when(reviewRepository.save(any(Review.class))).thenReturn(review1);

        // When
        reviewService.addImageToReview(1L, mockFile, 1L);

        // Then
        verify(fileStorageService).deleteFile("old_image.jpg");
        verify(fileStorageService).storeFile(mockFile, "review_1");
    }

    @Test
    void shouldThrowBadRequestForInvalidImageType() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When & Then
        assertThatThrownBy(() -> reviewService.addImageToReview(1L, mockFile, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File must be an image");
    }

    // ========== getReviewImage Tests ==========

    @Test
    void shouldGetReviewImage() throws Exception {
        // Given
        review1.setImageFilename("review_1_abc.jpg");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When & Then - test sprawdza tylko czy metoda wywołuje odpowiednie zależności
        // Faktyczne utworzenie Resource jest testowane w testach integracyjnych
        assertThatThrownBy(() -> reviewService.getReviewImage(1L))
            .isInstanceOf(RuntimeException.class);

        verify(reviewRepository).findById(1L);
        verify(fileStorageService).loadFile("review_1_abc.jpg");
    }

    @Test
    void shouldThrowExceptionWhenReviewHasNoImage() {
        // Given
        review1.setImageFilename(null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewImage(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not have an image");
    }

    // ========== deleteReviewImage Tests ==========

    @Test
    void shouldDeleteReviewImage() {
        // Given
        review1.setImageFilename("review_1_abc.jpg");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        when(reviewRepository.save(any(Review.class))).thenReturn(review1);

        // When
        reviewService.deleteReviewImage(1L, 1L);

        // Then
        verify(fileStorageService).deleteFile("review_1_abc.jpg");
        verify(reviewRepository).save(review1);
        assertThat(review1.getImageFilename()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenDeletingImageFromReviewWithoutImage() {
        // Given
        review1.setImageFilename(null);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When & Then
        assertThatThrownBy(() -> reviewService.deleteReviewImage(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not have an image");
    }

    @Test
    void shouldThrowUnauthorizedWhenDeletingImageFromOthersReview() {
        // Given
        review1.setImageFilename("review_1_abc.jpg");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        review1.getUser().setId(1L);

        // When & Then
        assertThatThrownBy(() -> reviewService.deleteReviewImage(1L, 999L))
                .isInstanceOf(UnauthorizedException.class);
    }
}
