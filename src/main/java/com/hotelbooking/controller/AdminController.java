package com.hotelbooking.controller;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only user management. Everything here requires the ADMIN role
 * (see SecurityConfig: method security is enabled, so @PreAuthorize is
 * what actually enforces this, not the request-matcher list there).
 *
 * This closes TODO 6.3 from PROJECT_STATUS.md: previously the only way
 * to grant HOTEL_MANAGER/ADMIN was to edit the users table by hand.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only user management")
public class AdminController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users (ADMIN only)")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change a user's role (ADMIN only)")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(userService.updateRole(id, request, currentUser));
    }
}
