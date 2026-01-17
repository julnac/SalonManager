package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.model.dto.review.response.ReviewDto;
import pl.edu.salonmanager.salon_manager.service.ReviewService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    void shouldDisplayReviewsList() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<ReviewDto> reviews = Arrays.asList(
                new ReviewDto(1L, "Great service!", now, 1L, "Jan Kowalski", null),
                new ReviewDto(2L, "Excellent!", now, 2L, "Anna Nowak", "/api/v1/reviews/2/image")
        );
        when(reviewService.getAllReviews()).thenReturn(reviews);

        // When & Then
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/reviews/list"))
                .andExpect(model().attributeExists("reviews"))
                .andExpect(model().attribute("reviews", hasSize(2)))
                .andExpect(model().attribute("reviews", everyItem(
                        instanceOf(ReviewDto.class))))
                .andExpect(model().attribute("reviews", hasItem(
                        allOf(
                                hasProperty("id", is(1L)),
                                hasProperty("content", is("Great service!")),
                                hasProperty("userName", is("Jan Kowalski")),
                                hasProperty("imageUrl", nullValue())
                        ))))
                .andExpect(model().attribute("reviews", hasItem(
                        allOf(
                                hasProperty("id", is(2L)),
                                hasProperty("content", is("Excellent!")),
                                hasProperty("userName", is("Anna Nowak")),
                                hasProperty("imageUrl", is("/api/v1/reviews/2/image"))
                        ))));
    }

    @Test
    void shouldDisplayEmptyReviewsListWhenNoReviewsExist() throws Exception {
        // Given
        when(reviewService.getAllReviews()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/reviews/list"))
                .andExpect(model().attribute("reviews", hasSize(0)));
    }
}
