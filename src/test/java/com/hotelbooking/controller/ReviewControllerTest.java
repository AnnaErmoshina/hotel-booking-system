package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateReviewRequest;
import com.hotelbooking.dto.response.HotelRatingResponse;
import com.hotelbooking.dto.response.ReviewResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Test
    void create_returns201() {
        ReviewController controller = new ReviewController(reviewService);
        UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.USER).build());
        CreateReviewRequest request = new CreateReviewRequest(5, "Great");
        ReviewResponse review = new ReviewResponse(1L, 1L, 10L, 1L, 5, "Great", LocalDateTime.now());
        when(reviewService.create(10L, request, principal)).thenReturn(review);

        var response = controller.create(10L, request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(review);
    }

    @Test
    void getByHotel_returns200() {
        ReviewController controller = new ReviewController(reviewService);
        when(reviewService.getByHotel(1L)).thenReturn(List.of());

        var response = controller.getByHotel(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getHotelRating_returns200() {
        ReviewController controller = new ReviewController(reviewService);
        HotelRatingResponse rating = new HotelRatingResponse(1L, 4.5, 3L);
        when(reviewService.getHotelRating(1L)).thenReturn(rating);

        var response = controller.getHotelRating(1L);

        assertThat(response.getBody()).isEqualTo(rating);
    }
}
