package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateReviewRequest;
import com.hotelbooking.dto.response.HotelRatingResponse;
import com.hotelbooking.dto.response.ReviewResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Hotel reviews left against a completed booking")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/bookings/{bookingId}/reviews")
    @Operation(summary = "Leave a review for one of your own COMPLETED bookings")
    public ResponseEntity<ReviewResponse> create(
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(bookingId, request, currentUser));
    }

    @GetMapping("/api/hotels/{hotelId}/reviews")
    @Operation(summary = "List reviews for a hotel (public)")
    public ResponseEntity<List<ReviewResponse>> getByHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reviewService.getByHotel(hotelId));
    }

    @GetMapping("/api/hotels/{hotelId}/rating")
    @Operation(summary = "Get a hotel's average rating and review count (public)")
    public ResponseEntity<HotelRatingResponse> getHotelRating(@PathVariable Long hotelId) {
        return ResponseEntity.ok(reviewService.getHotelRating(hotelId));
    }
}
