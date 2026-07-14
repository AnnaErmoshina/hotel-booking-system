package com.hotelbooking.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        String email,
        String role
) {
}
