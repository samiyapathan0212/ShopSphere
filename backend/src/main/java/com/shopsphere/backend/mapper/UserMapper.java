package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.response.UserResponse;

/**
 * Maps {@link User} entities to response DTOs.
 * Response mapping intentionally omits {@code passwordHash}.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}