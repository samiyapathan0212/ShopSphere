package com.shopsphere.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.exception.GlobalExceptionHandler;
import com.shopsphere.backend.security.JwtProperties;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.SecurityConfig;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.AuthService;

/**
 * End-to-end security-flow tests for the web layer: public vs protected
 * routes, JWT-rejected unauthenticated requests, and valid-token access.
 * Uses a real {@link JwtService} (with a dev-only secret) and real
 * {@link SecurityConfig}; the persistence layer is mocked.
 */
@WebMvcTest({AuthController.class, HealthController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityFlowTest.JwtTestBeans.class})
class SecurityFlowTest {

    private static final String DEV_SECRET =
            "ZGV2LW9ubHktc2hvcHNwaGVyZS1qd3Qtc2VjcmV0LWtleS1wbGFjZWhvbGRlci0wMTIzNDU2Nzg5YWJjZGVm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    static class JwtTestBeans {

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret(DEV_SECRET);
            properties.setExpirationMs(900_000L);
            return properties;
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }

    private UserPrincipal principal() {
        User user = User.builder()
                .id(7L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .passwordHash("$2a$10$devOnlyHashValue0000000000000000000000000000000")
                .role(Role.CUSTOMER)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
        return UserPrincipal.from(user);
    }

    @Test
    void registerAndLoginEndpointsRemainPublic() throws Exception {
        UserResponse user = new UserResponse(
                1L, "Ada Lovelace", "ada@example.com", Role.CUSTOMER,
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"));
        when(authService.register(any())).thenReturn(user);
        when(authService.login(any())).thenReturn(LoginResponse.of("sample.jwt.token", 900_000L, user));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Ada Lovelace\",\"email\":\"ada@example.com\",\"password\":\"superSecret1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"password\":\"superSecret1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("sample.jwt.token")));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        UserPrincipal principal = principal();
        when(userDetailsService.loadUserByUsername("ada@example.com")).thenReturn(principal);

        String token = jwtService.generateToken(principal);

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.email", is("ada@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}