package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.LoginRequest;
import com.hotelbooking.dto.request.RegisterRequest;
import com.hotelbooking.dto.response.AuthResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.EmailAlreadyExistsException;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Anna", "Doe", "anna@example.com", "password123");
    }

    @Test
    void register_savesUserWithHashedPasswordAndUserRole() {
        when(userRepository.existsByEmail("anna@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("anna@example.com");
        assertThat(response.role()).isEqualTo("USER");

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        when(userRepository.existsByEmail("anna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequest loginRequest = new LoginRequest("anna@example.com", "password123");
        User user = User.builder().id(1L).email("anna@example.com").role(Role.USER).build();
        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsUsernameNotFound_whenUserVanishedAfterAuthentication() {
        LoginRequest loginRequest = new LoginRequest("ghost@example.com", "password123");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
