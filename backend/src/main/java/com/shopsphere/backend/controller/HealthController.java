package com.shopsphere.backend.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Simple health endpoint for Phase 1. No business logic involved.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health checks")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns the application status")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "service", "shopsphere-backend",
                        "timestamp", Instant.now()));
    }
}