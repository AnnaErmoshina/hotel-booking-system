package com.hotelbooking.controller;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Test
    void updateRole_returns200WithUpdatedUser() {
        AdminController controller = new AdminController(userService);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.HOTEL_MANAGER);
        UserResponse user = new UserResponse(1L, "Anna", "Doe", "anna@example.com", "HOTEL_MANAGER");
        when(userService.updateRole(1L, request)).thenReturn(user);

        var response = controller.updateRole(1L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(user);
    }
}
