package com.shopsphere.backend.dto.request;

import com.shopsphere.backend.domain.PaymentStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for the admin payment-status endpoint (Phase 5). Allowed
 * manual transitions (PENDING to PAID/FAILED, PAID to REFUNDED) are enforced
 * in the payment service.
 */
public record UpdatePaymentStatusRequest(

        @NotNull(message = "paymentStatus is required")
        PaymentStatus paymentStatus) {
}
