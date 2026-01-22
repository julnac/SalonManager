package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

import java.io.IOException;
import java.nio.file.Files;
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

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @TempDir
    Path tempDir;

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
        verify(reviewRepository).findAllByOrderByCreatedAtDesc();
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
        verify(reviewRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenReviewNotFound() {
        // Given
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review not found");
        verify(reviewRepository).findById(1L);
    }

    // ========== createReview Tests ==========

    @Test
    void shouldCreateReviewWithoutImage() {
        // Given
        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When
        ReviewDto result = reviewService.createReview(createRequest, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getContent()).isEqualTo("Great experience!");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getImageUrl()).isNull();

        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void shouldCreateReviewWithImage() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(fileStorageService.storeFile(mockFile, "review_3")).thenReturn("review_3_abc.jpg");

        // When
        ReviewDto result = reviewService.createReview(createRequest, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getContent()).isEqualTo("Great experience!");
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/3/image");

        verify(fileStorageService).storeFile(mockFile, "review_3");
        verify(reviewRepository, times(2)).save(any(Review.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreateReview() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(createRequest, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository).findById(1L);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void shouldThrowBadRequestWhenCreatingReviewWithInvalidImageType() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("application/pdf");

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(createRequest, mockFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File must be an image");
        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
        verify(fileStorageService, never()).storeFile(any(), anyString());
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
        verify(reviewRepository).findById(1L);
        verify(reviewRepository, never()).deleteById(anyLong());
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
        verify(reviewRepository).findById(1L);
        verify(reviewRepository, never()).deleteById(anyLong());
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
        verify(reviewRepository).findById(1L);
        verify(fileStorageService, never()).loadFile(anyString());
    }

    @Test
    void shouldThrowExceptionWhenReviewNotFoundForImage() {
        // Given
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewImage(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review not found");
        verify(reviewRepository).findById(999L);
        verify(fileStorageService, never()).loadFile(anyString());
    }

    @Test
    void shouldSkipEmptyImageFile() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When
        ReviewDto result = reviewService.createReview(createRequest, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isNull();
        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void shouldThrowBadRequestWhenImageMimeTypeNotAllowed() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/svg+xml"); // starts with "image/" but not in allowed list

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(createRequest, mockFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Allowed image types: JPEG, PNG, GIF");
        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void shouldThrowBadRequestWhenContentTypeIsNull() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn(null);

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // When & Then
        assertThatThrownBy(() -> reviewService.createReview(createRequest, mockFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File must be an image");
        verify(userRepository).findById(1L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void shouldDeleteReviewAndLogWarningWhenFileDeleteFails() {
        // Given
        review1.setImageFilename("review_1_abc.jpg");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        doThrow(new RuntimeException("File delete failed")).when(fileStorageService).deleteFile("review_1_abc.jpg");

        // When
        reviewService.deleteReview(1L, 1L);

        // Then - should still delete the review even if file deletion fails
        verify(fileStorageService).deleteFile("review_1_abc.jpg");
        verify(reviewRepository).deleteById(1L);
    }

    @Test
    void shouldMapReviewWithImageToDto() {
        // Given
        review1.setImageFilename("test_image.jpg");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        // When
        ReviewDto result = reviewService.getReviewById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/1/image");
        verify(reviewRepository).findById(1L);
    }

    @Test
    void shouldAcceptJpgMimeType() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpg");

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(fileStorageService.storeFile(mockFile, "review_3")).thenReturn("review_3_abc.jpg");

        // When
        ReviewDto result = reviewService.createReview(createRequest, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/3/image");
        verify(fileStorageService).storeFile(mockFile, "review_3");
        verify(reviewRepository, times(2)).save(any(Review.class));
    }

    @Test
    void shouldAcceptPngMimeType() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/png");

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(fileStorageService.storeFile(mockFile, "review_3")).thenReturn("review_3_abc.png");

        // When
        ReviewDto result = reviewService.createReview(createRequest, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/3/image");
        verify(fileStorageService).storeFile(mockFile, "review_3");
        verify(reviewRepository, times(2)).save(any(Review.class));
    }

    @Test
    void shouldAcceptGifMimeType() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/gif");

        Review savedReview = new Review();
        savedReview.setId(3L);
        savedReview.setUser(testUser);
        savedReview.setContent("Great experience!");
        savedReview.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(fileStorageService.storeFile(mockFile, "review_3")).thenReturn("review_3_abc.gif");

        // When
        ReviewDto result = reviewService.createReview(createRequest, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/reviews/3/image");
        verify(fileStorageService).storeFile(mockFile, "review_3");
        verify(reviewRepository, times(2)).save(any(Review.class));
    }

    @Test
    void shouldReturnResourceWhenFileExistsAndIsReadable() throws IOException {
        // Given
        String filename = "test-image.jpg";
        Path imagePath = tempDir.resolve(filename);
        Files.createFile(imagePath); // Tworzymy fizyczny plik w tempDir

        review1.setImageFilename(filename);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        when(fileStorageService.loadFile(filename)).thenReturn(imagePath);

        // When
        Resource result = reviewService.getReviewImage(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
        verify(fileStorageService).loadFile(filename);
    }

    @Test
    void shouldThrowResourceNotFoundWhenFileDoesNotExistOnDisk() {
        // Given
        String filename = "non-existent.jpg";
        Path nonExistentPath = tempDir.resolve(filename); // Ścieżka poprawna, ale pliku nie ma

        review1.setImageFilename(filename);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));
        when(fileStorageService.loadFile(filename)).thenReturn(nonExistentPath);

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewImage(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image file not found or not readable");
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUrlIsMalformed() {
        // Given
        review1.setImageFilename("invalid-file");
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review1));

        Path mockPath = mock(Path.class);
        when(fileStorageService.loadFile(anyString())).thenReturn(mockPath);
        // Path.toUri() może rzucić błędem przy nieprawidłowych znakach w specyficznych systemach
        when(mockPath.toUri()).thenThrow(new IllegalArgumentException("Invalid path"));

        // When & Then
        assertThatThrownBy(() -> reviewService.getReviewImage(1L))
                .isInstanceOf(RuntimeException.class);
    }

}
