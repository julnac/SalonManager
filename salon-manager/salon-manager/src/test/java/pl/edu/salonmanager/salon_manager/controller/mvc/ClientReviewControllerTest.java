package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.model.dto.review.request.CreateReviewRequest;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientReviewController.class)
class ClientReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .enabled(true)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldDisplayAddReviewForm() throws Exception {
        mockMvc.perform(get("/client/reviews/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"))
                .andExpect(model().attributeExists("reviewForm"))
                .andExpect(model().attribute("reviewForm",
                        instanceOf(ClientReviewController.ReviewForm.class)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldAddReviewSuccessfully() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        ReviewDto createdReview = new ReviewDto(
                1L,
                "Great service!",
                LocalDateTime.now(),
                1L,
                "Jan Kowalski",
                null
        );
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(null)))
                .thenReturn(createdReview);

        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", "Great service! Very professional and friendly staff."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage",
                        "Dziękujemy za dodanie opinii!"));

        verify(reviewService).createReview(any(CreateReviewRequest.class), eq(null));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldRejectReviewWithEmptyContent() throws Exception {
        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"));

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldRejectReviewWithTooShortContent() throws Exception {
        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", "Short"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"));

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldRejectReviewWithTooLongContent() throws Exception {
        // Given
        String longContent = "a".repeat(1001);

        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", longContent))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"));

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldHandleErrorWhenAddingReview() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(null)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", "Great service! Very professional staff."))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage",
                        "Wystąpił błąd podczas dodawania opinii"));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void shouldHandleErrorWhenUserNotFound() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", "Great service! Very professional staff."))
                .andExpect(status().isOk())
                .andExpect(view().name("client/reviews/add"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void shouldAcceptReviewWithValidLength() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        String validContent = "a".repeat(500); // 500 characters - within limits
        ReviewDto createdReview = new ReviewDto(
                1L,
                validContent,
                LocalDateTime.now(),
                1L,
                "Jan Kowalski",
                null
        );
        when(reviewService.createReview(any(CreateReviewRequest.class), eq(null)))
                .thenReturn(createdReview);

        // When & Then
        mockMvc.perform(post("/client/reviews/add")
                        .with(csrf())
                        .param("content", validContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        verify(reviewService).createReview(any(CreateReviewRequest.class), eq(null));
    }
}
