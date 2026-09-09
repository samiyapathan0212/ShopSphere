package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.Payment;
import com.shopsphere.backend.dto.response.PaymentResponse;

/**
 * Maps payment entities to response DTOs (Phase 5).
 */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
