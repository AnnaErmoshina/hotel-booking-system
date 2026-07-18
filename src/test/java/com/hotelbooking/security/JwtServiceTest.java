package com.hotelbooking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // @Value fields aren't populated outside a Spring context - set them directly.
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "irrelevant-hash", List.of(() -> "ROLE_USER"));
    }

    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        UserDetails user = userDetails("anna@example.com");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("anna@example.com");
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingFreshToken() {
        UserDetails user = userDetails("anna@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_whenUsernameDoesNotMatch() {
        UserDetails tokenOwner = userDetails("anna@example.com");
        UserDetails someoneElse = userDetails("bob@example.com");
        String token = jwtService.generateToken(tokenOwner);

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenTokenAlreadyExpired() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // expires immediately
        UserDetails user = userDetails("anna@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }
}
