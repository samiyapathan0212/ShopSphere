package com.shopsphere.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.exception.GlobalExceptionHandler;
import com.shopsphere.backend.security.JwtProperties;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.RestAccessDeniedHandler;
import com.shopsphere.backend.security.SecurityConfig;
import com.shopsphere.backend.security.UserPrincipal;

/**
 * Phase 2D role-authorization tests: proves the JWT role claim is mapped to
 * Spring authorities and that {@code @PreAuthorize} rules yield 200 for the
 * owning role, a clean JSON 403 for the other authenticated role, and 401 for
 * unauthenticated requests. Uses a real {@link JwtService} (dev-only secret)
 * and the real {@link SecurityConfig}; the persistence layer is mocked.
 */
@WebMvcTest(RoleTestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RoleAuthorizationTest.JwtTestBeans.class})
class RoleAuthorizationTest {

    private static final String DEV_SECRET =
            "ZGV2LW9ubHktc2hvcHNwaGVyZS1qd3Qtc2VjcmV0LWtleS1wbGFjZWhvbGRlci0wMTIzNDU2Nzg5YWJjZGVm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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

    private UserPrincipal principal(Role role, String email) {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email(email)
                .passwordHash("$2a$10$devOnlyHashValue0000000000000000000000000000000")
                .role(role)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
        return UserPrincipal.from(user);
    }

    /** Issues a real JWT and stubs the user lookup performed by the JWT filter. */
    private String tokenFor(UserPrincipal principal) {
        when(userDetailsService.loadUserByUsername(principal.getUsername())).thenReturn(principal);
        return jwtService.generateToken(principal);
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        UserPrincipal admin = principal(Role.ADMIN, "admin@example.com");
        String token = tokenFor(admin);

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredRole", is("ADMIN")))
                .andExpect(jsonPath("$.authenticatedAs", is("admin@example.com")));
    }

    @Test
    void customerIsForbiddenFromAdminEndpoint() throws Exception {
        UserPrincipal customer = principal(Role.CUSTOMER, "ada@example.com");
        String token = tokenFor(customer);

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(RestAccessDeniedHandler.FORBIDDEN_MESSAGE)));
    }

    @Test
    void customerCanAccessCustomerEndpoint() throws Exception {
        UserPrincipal customer = principal(Role.CUSTOMER, "ada@example.com");
        String token = tokenFor(customer);

        mockMvc.perform(get("/api/test/customer").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredRole", is("CUSTOMER")))
                .andExpect(jsonPath("$.authenticatedAs", is("ada@example.com")));
    }

    @Test
    void adminIsForbiddenFromCustomerEndpoint() throws Exception {
        UserPrincipal admin = principal(Role.ADMIN, "admin@example.com");
        String token = tokenFor(admin);

        mockMvc.perform(get("/api/test/customer").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(RestAccessDeniedHandler.FORBIDDEN_MESSAGE)));
    }

    @Test
    void unauthenticatedRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication is required")));

        mockMvc.perform(get("/api/test/customer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtRoleClaimMapsToSpringAuthorities() {
        UserPrincipal admin = principal(Role.ADMIN, "admin@example.com");
        UserPrincipal customer = principal(Role.CUSTOMER, "ada@example.com");

        // The existing Role enum maps 1:1 onto ROLE_<NAME> authorities — no
        // duplicated role definitions anywhere.
        assertThat(admin.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(customer.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");

        // The JWT role claim round-trips to the enum the authorities come from.
        assertThat(jwtService.extractRole(jwtService.generateToken(admin))).isEqualTo(Role.ADMIN);
        assertThat(jwtService.extractRole(jwtService.generateToken(customer))).isEqualTo(Role.CUSTOMER);
    }
}