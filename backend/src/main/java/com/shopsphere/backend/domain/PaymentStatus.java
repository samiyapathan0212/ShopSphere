package com.shopsphere.backend.domain;

/**
 * Payment lifecycle (Phase 5). Stored on both the payment record and the
 * order (denormalized snapshot for quick order-list rendering).
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
