package com.shopsphere.backend.dto.request;

/**
 * Request payload for processing (or retrying) a pending payment (Phase 5).
 * {@code simulateFailure} is a sandbox-only test hook that asks the payment
 * gateway to decline the charge.
 */
public record ProcessPaymentRequest(Boolean simulateFailure) {
}
