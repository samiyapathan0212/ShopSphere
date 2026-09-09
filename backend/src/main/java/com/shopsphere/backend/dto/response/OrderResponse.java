package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopsphere.backend.domain.OrderStatus;
import com.shopsphere.backend.domain.PaymentStatus;

/**
 * Response DTO for an order (Phase 5). The shipping address is the snapshot
 * taken at checkout.
 */
public record OrderResponse(
        Long id,
        Long userId,
        String orderNumber,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        Instant createdAt,
        Instant updatedAt) {
}
