package com.hotelbooking.service;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;

public interface UserService {

    UserResponse updateRole(Long userId, UpdateUserRoleRequest request);
}
