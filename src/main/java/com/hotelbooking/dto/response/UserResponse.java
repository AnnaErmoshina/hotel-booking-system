package com.hotelbooking.dto.response;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
