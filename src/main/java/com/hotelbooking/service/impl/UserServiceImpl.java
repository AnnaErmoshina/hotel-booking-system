package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.UpdateUserRoleRequest;
import com.hotelbooking.dto.response.UserResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, UpdateUserRoleRequest request, UserPrincipal currentUser) {
        // Guard rail: an admin changing their own role could accidentally lock
        // themselves (and, if they are the only admin, everyone) out of the
        // admin endpoints. Force that to happen through another admin account.
        if (id.equals(currentUser.getUser().getId())) {
            throw new ForbiddenOperationException("You cannot change your own role");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setRole(request.role());
        userRepository.save(user);

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
