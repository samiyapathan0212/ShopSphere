/**
 * Security configuration.
 * Phase 2C: {@code SecurityConfig}, {@code JwtService}, {@code JwtProperties},
 * {@code RefreshTokenProperties}, {@code RefreshTokenCookieService},
 * {@code JwtAuthenticationFilter}, {@code UserPrincipal} and
 * {@code RestAuthenticationEntryPoint}.
 * Phase 2D: {@code RestAccessDeniedHandler} (clean JSON 403) plus
 * method-security role checks ({@code @PreAuthorize}) on controllers.
 */
package com.shopsphere.backend.security;