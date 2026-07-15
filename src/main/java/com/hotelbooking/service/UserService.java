package com.hotelbooking.service;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface UserService {

    List<UserResponse> getAll();

    UserResponse updateRole(Long id, UpdateUserRoleRequest request, UserPrincipal currentUser);
}
