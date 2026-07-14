package com.hotelbooking.service;

import com.hotelbooking.dto.request.LoginRequest;
import com.hotelbooking.dto.request.RegisterRequest;
import com.hotelbooking.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
