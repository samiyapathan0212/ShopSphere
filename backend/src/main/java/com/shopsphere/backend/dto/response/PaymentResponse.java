package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.shopsphere.backend.domain.PaymentStatus;

/**
 * Response DTO for a payment (Phase 5). {@code providerPaymentId} is the
 * reference returned by the payment gateway (e.g. a sandbox token).
 */
public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        String provider,
        String providerPaymentId,
        Instant createdAt,
        Instant updatedAt) {
}
