package com.shopsphere.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.LoginRequest;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.mapper.UserMapper;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.AuthService;

/**
 * Authentication endpoints (Phase 2B): registration, login and current user.
 * Refresh tokens and role-based authorization are intentionally not
 * implemented yet.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login and current user")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account",
            description = "Creates a user with a BCrypt-hashed password. Duplicate emails return 409.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password",
            description = "Authenticates credentials and returns a short-lived JWT access token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user",
            description = "Returns the profile of the authenticated user. Requires a valid Bearer token.")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(UserMapper.toResponse(principal.user()));
    }
}