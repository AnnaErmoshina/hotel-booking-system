package com.hotelbooking.dto.response;

import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        Long hotelId,
        String name,
        String description,
        BigDecimal basePrice,
        Integer capacity
) {
}
