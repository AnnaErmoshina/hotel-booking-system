package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateReviewRequest;
import com.hotelbooking.dto.response.HotelRatingResponse;
import com.hotelbooking.dto.response.ReviewResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface ReviewService {

    ReviewResponse create(Long bookingId, CreateReviewRequest request, UserPrincipal currentUser);

    List<ReviewResponse> getByHotel(Long hotelId);

    HotelRatingResponse getHotelRating(Long hotelId);
}
