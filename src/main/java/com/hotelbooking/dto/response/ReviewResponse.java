package com.hotelbooking.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long hotelId,
        Long bookingId,
        Long userId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
