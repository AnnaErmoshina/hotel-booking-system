package com.hotelbooking.controller;

import com.hotelbooking.dto.request.LoginRequest;
import com.hotelbooking.dto.request.RegisterRequest;
import com.hotelbooking.dto.response.AuthResponse;
import com.hotelbooking.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void register_returns201WithToken() {
        AuthController controller = new AuthController(authService);
        RegisterRequest request = new RegisterRequest("Anna", "Doe", "anna@example.com", "password123");
        AuthResponse authResponse = new AuthResponse("jwt", "Bearer", "anna@example.com", "USER");
        when(authService.register(request)).thenReturn(authResponse);

        var response = controller.register(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    void login_returns200WithToken() {
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest("anna@example.com", "password123");
        AuthResponse authResponse = new AuthResponse("jwt", "Bearer", "anna@example.com", "USER");
        when(authService.login(request)).thenReturn(authResponse);

        var response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }
}
