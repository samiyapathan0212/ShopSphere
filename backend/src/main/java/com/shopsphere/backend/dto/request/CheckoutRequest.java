package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for checkout (Phase 5). The shipping address is captured
 * as an immutable snapshot on the order. {@code simulatePaymentFailure} is a
 * sandbox-only test hook: when true the payment gateway is asked to decline,
 * which cancels the order, fails the payment and releases the reserved stock.
 */
public record CheckoutRequest(

        @NotBlank(message = "recipientName is required")
        @Size(max = 100, message = "recipientName must not exceed 100 characters")
        String recipientName,

        @NotBlank(message = "phone is required")
        @Size(max = 30, message = "phone must not exceed 30 characters")
        String phone,

        @NotBlank(message = "addressLine1 is required")
        @Size(max = 255, message = "addressLine1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "addressLine2 must not exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must not exceed 100 characters")
        String city,

        @NotBlank(message = "state is required")
        @Size(max = 100, message = "state must not exceed 100 characters")
        String state,

        @NotBlank(message = "postalCode is required")
        @Size(max = 20, message = "postalCode must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "country is required")
        @Size(max = 100, message = "country must not exceed 100 characters")
        String country,

        Boolean simulatePaymentFailure) {
}
