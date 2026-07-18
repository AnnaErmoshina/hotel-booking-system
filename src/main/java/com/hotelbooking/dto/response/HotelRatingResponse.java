package com.hotelbooking.dto.response;

public record HotelRatingResponse(
        Long hotelId,
        Double averageRating,
        long reviewCount
) {
}
