package com.hotelbooking.dto.request;

import com.hotelbooking.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(

        @NotNull(message = "Role is required")
        Role role
) {
}
