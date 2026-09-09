package com.shopsphere.backend.dto.response;

/**
 * Response DTO for checkout (Phase 5): the created order (already advanced to
 * CONFIRMED when payment succeeded, or CANCELLED when the sandbox payment was
 * declined) plus the associated payment.
 */
public record CheckoutResponse(
        OrderResponse order,
        PaymentResponse payment) {
}
