package com.shopsphere.backend.dto.response;

import java.time.Instant;

import com.shopsphere.backend.domain.Role;

/**
 * Public user representation. Intentionally excludes {@code passwordHash}
 * to guarantee it can never leak through API responses.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Instant updatedAt) {
}