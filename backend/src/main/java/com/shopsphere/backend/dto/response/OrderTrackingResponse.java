package com.shopsphere.backend.dto.response;

import java.time.Instant;

import com.shopsphere.backend.domain.OrderStatus;
import com.shopsphere.backend.domain.PaymentStatus;

/**
 * Response DTO for order tracking (Phase 5): the current lifecycle position
 * plus whether the order can still be cancelled.
 */
public record OrderTrackingResponse(
        String orderNumber,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        boolean cancellable,
        Instant createdAt,
        Instant updatedAt) {
}
