package com.shopsphere.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.exception.EmailAlreadyExistsException;
import com.shopsphere.backend.exception.GlobalExceptionHandler;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.SecurityConfig;
import com.shopsphere.backend.service.AuthService;

/**
 * Web-slice tests for the authentication endpoints. Validates HTTP behavior,
 * centralized exception handling and that passwordHash never appears in
 * responses. The data layer is mocked; service logic is tested separately.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void registerReturnsCreatedWithUserBody() throws Exception {
        UserResponse response = new UserResponse(
                1L, "Ada Lovelace", "ada@example.com", Role.CUSTOMER,
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"ada@example.com","password":"superSecret1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Ada Lovelace")))
                .andExpect(jsonPath("$.email", is("ada@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsBlankFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.fieldErrors.name", is("name is required")))
                .andExpect(jsonPath("$.fieldErrors.email", is("email is required")))
                .andExpect(jsonPath("$.fieldErrors.password", is("password is required")));

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidEmailAndShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.email", containsString("valid email")))
                .andExpect(jsonPath("$.fieldErrors.password", containsString("8 and 72")));

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void registerReturnsConflictOnDuplicateEmail() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ada@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"ada@example.com","password":"superSecret1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("ada@example.com")));
    }

    @Test
    void registerRejectsMalformedBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void loginReturns200WithTokenAndUser() throws Exception {
        UserResponse user = new UserResponse(
                1L, "Ada Lovelace", "ada@example.com", Role.CUSTOMER,
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"));
        LoginResponse loginResponse = LoginResponse.of("sample.jwt.token", 900_000L, user);
        when(authService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"superSecret1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("sample.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresInMs", is(900000)))
                .andExpect(jsonPath("$.user.email", is("ada@example.com")))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginFailsValidationForBlankCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", is("email is required")));
    }

    @Test
    void loginInvalidCredentialsReturns401GenericError() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }
}