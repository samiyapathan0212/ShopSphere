package com.shopsphere.backend.dto.response;

import com.shopsphere.backend.domain.PaymentStatus;

/**
 * Response DTO for payment verification (Phase 5): whether the gateway could
 * confirm the charge, alongside the locally stored status for comparison.
 */
public record PaymentVerificationResponse(
        Long paymentId,
        Long orderId,
        boolean verified,
        PaymentStatus paymentStatus,
        String message) {
}
