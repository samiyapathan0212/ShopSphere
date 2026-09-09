package com.shopsphere.backend.domain;

import java.util.EnumSet;
import java.util.Map;

/**
 * Lifecycle of an order (Phase 5). Transitions are enforced centrally through
 * {@link #canTransitionTo(OrderStatus)} so both the admin status endpoint and
 * the customer cancellation flow share a single source of truth — invalid
 * transitions such as {@code DELIVERED -> PROCESSING} are rejected with 409.
 */
public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            PLACED, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(OUT_FOR_DELIVERY, DELIVERED),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
            // Terminal states: no further transitions are permitted.
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    /**
     * @param target the candidate next status
     * @return whether the transition from this status to the target is valid
     */
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
