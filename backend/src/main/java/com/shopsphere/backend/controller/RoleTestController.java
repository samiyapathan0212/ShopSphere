package com.shopsphere.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * TEMPORARY role-authorization verification endpoints (Phase 2D).
 *
 * <p>These endpoints exist solely to prove that {@code @PreAuthorize} role
 * checks work end-to-end. They are NOT business features, expose nothing
 * beyond the caller's own identity, and must be removed once real
 * role-protected resources exist (e.g. admin dashboard, orders). To remove
 * them cleanly: delete this class and the {@code RoleAuthorizationTest}
 * test class — no other code references them.</p>
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "Role verification (temporary)",
        description = "Throwaway security-verification endpoints; remove once real role-protected features exist")
public class RoleTestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[TEMPORARY] ADMIN-only check",
            description = "Security verification only: 200 for ADMIN, 403 for any other authenticated role, 401 when unauthenticated.")
    public ResponseEntity<Object> admin(Authentication authentication) {
        return ResponseEntity.ok(responseFor("/api/test/admin", "ADMIN", authentication));
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "[TEMPORARY] CUSTOMER-only check",
            description = "Security verification only: 200 for CUSTOMER, 403 for any other authenticated role, 401 when unauthenticated.")
    public ResponseEntity<Object> customer(Authentication authentication) {
        return ResponseEntity.ok(responseFor("/api/test/customer", "CUSTOMER", authentication));
    }

    private static Object responseFor(String endpoint, String requiredRole, Authentication authentication) {
        // Only the caller's own username (email) is echoed back — no sensitive data.
        return java.util.Map.of(
                "endpoint", endpoint,
                "requiredRole", requiredRole,
                "temporary", "verification endpoint — remove in a later phase",
                "authenticatedAs", authentication.getName());
    }
}