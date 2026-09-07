package com.shopsphere.backend.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.LoginRequest;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.exception.EmailAlreadyExistsException;
import com.shopsphere.backend.mapper.UserMapper;
import com.shopsphere.backend.repository.UserRepository;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.UserPrincipal;

/**
 * Authentication (Phases 2A + 2B).
 * <p>
 * Registration stores only BCrypt password hashes. Login authenticates
 * email + password through Spring Security's {@link AuthenticationManager}
 * and issues a short-lived JWT. Passwords are never logged, and authentication
 * failures always surface the same generic message so the API never reveals
 * whether an email is registered.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Backstop for a concurrent registration racing the pre-check above.
            throw new EmailAlreadyExistsException(email);
        }

        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            // Generic error for both wrong password and unknown email.
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal);
        return LoginResponse.of(token, jwtService.getExpirationMs(), UserMapper.toResponse(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}