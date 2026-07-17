package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateRole_changesRoleAndReturnsUpdatedUser() {
        User user = User.builder().id(1L).firstName("Anna").lastName("Doe")
                .email("anna@example.com").role(Role.USER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateRole(1L, new UpdateUserRoleRequest(Role.HOTEL_MANAGER));

        assertThat(response.role()).isEqualTo("HOTEL_MANAGER");
        assertThat(user.getRole()).isEqualTo(Role.HOTEL_MANAGER);
    }

    @Test
    void updateRole_throwsResourceNotFound_whenUserMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(404L, new UpdateUserRoleRequest(Role.ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
