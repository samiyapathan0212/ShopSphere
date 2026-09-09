package com.shopsphere.backend.dto.request;

import com.shopsphere.backend.domain.OrderStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for the admin order-status endpoint (Phase 5). The target
 * status is validated against {@link OrderStatus#canTransitionTo} in the
 * service layer; invalid transitions yield 409.
 */
public record UpdateOrderStatusRequest(

        @NotNull(message = "orderStatus is required")
        OrderStatus orderStatus) {
}
