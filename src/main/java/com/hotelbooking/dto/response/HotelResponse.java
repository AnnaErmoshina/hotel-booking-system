package com.hotelbooking.dto.response;

public record HotelResponse(
        Long id,
        String name,
        String description,
        String address,
        String city,
        String country,
        Long ownerId
) {
}
