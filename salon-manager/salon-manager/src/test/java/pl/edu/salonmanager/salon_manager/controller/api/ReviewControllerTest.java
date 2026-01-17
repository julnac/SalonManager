package pl.edu.salonmanager.salon_manager.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private UserRepository userRepository;

    private ReviewDto reviewDto;
    private CreateReviewRequest createRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        reviewDto = new ReviewDto(1L, "Excellent service!", LocalDateTime.now(), 1L, "Jan Kowalski", null);

        createRequest = new CreateReviewRequest();
        createRequest.setContent("Great experience!");
        createRequest.setUserId(1L);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@example.com");
        mockUser.setFirstName("Jan");
        mockUser.setLastName("Kowalski");
    }

    @Test
    @WithMockUser
    void shouldGetAllReviews() throws Exception {
        // Given
        when(reviewService.getAllReviews()).thenReturn(Arrays.asList(reviewDto));

        // When & Then
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].content").value("Excellent service!"))
                .andExpect(jsonPath("$[0].userName").value("Jan Kowalski"));
    }

    @Test
    @WithMockUser
    void shouldGetReviewById() throws Exception {
        // Given
        when(reviewService.getReviewById(1L)).thenReturn(reviewDto);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Excellent service!"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldCreateReviewWithoutImage() throws Exception {
        // Given
        ReviewDto created = new ReviewDto(2L, "Great experience!", LocalDateTime.now(), 1L, "Jan Kowalski", null);
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(null))).thenReturn(created);

        // When & Then
        mockMvc.perform(multipart("/api/v1/reviews")
                        .param("content", "Great experience!")
                        .param("userId", "1")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.content").value("Great experience!"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldCreateReviewWithImage() throws Exception {
        // Given
        MockMultipartFile mockImage = new MockMultipartFile(
            "image",
            "photo.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        ReviewDto created = new ReviewDto(2L, "Great experience!", LocalDateTime.now(), 1L, "Jan Kowalski",
                                         "/api/v1/reviews/2/image");
        when(reviewService.createReview(any(CreateReviewRequest.class), any())).thenReturn(created);

        // When & Then
        mockMvc.perform(multipart("/api/v1/reviews")
                        .file(mockImage)
                        .param("content", "Great experience!")
                        .param("userId", "1")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.content").value("Great experience!"))
                .andExpect(jsonPath("$.imageUrl").value("/api/v1/reviews/2/image"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void shouldDeleteReview() throws Exception {
        // Given
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));

        // When & Then
        mockMvc.perform(delete("/api/v1/reviews/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L, 1L);
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenReviewNotFound() throws Exception {
        // Given
        when(reviewService.getReviewById(99L)).thenThrow(new ResourceNotFoundException("Review not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoReviews() throws Exception {
        // Given
        when(reviewService.getAllReviews()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== Image download Tests ==========

    @Test
    @WithMockUser
    void shouldGetReviewImageAsPublic() throws Exception {
        // Given
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFilename()).thenReturn("review_1.jpg");
        when(mockResource.getFile()).thenReturn(new java.io.File("review_1.jpg"));

        when(reviewService.getReviewImage(1L)).thenReturn(mockResource);

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/1/image"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenImageNotFound() throws Exception {
        // Given
        when(reviewService.getReviewImage(1L))
            .thenThrow(new ResourceNotFoundException("Review does not have an image"));

        // When & Then
        mockMvc.perform(get("/api/v1/reviews/1/image"))
                .andExpect(status().isNotFound());
    }

}
