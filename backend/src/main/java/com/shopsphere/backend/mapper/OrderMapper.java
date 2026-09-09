package com.shopsphere.backend.mapper;

import java.util.List;

import com.shopsphere.backend.domain.Order;
import com.shopsphere.backend.domain.OrderItem;
import com.shopsphere.backend.dto.response.OrderItemResponse;
import com.shopsphere.backend.dto.response.OrderResponse;
import com.shopsphere.backend.dto.response.OrderTrackingResponse;

/**
 * Maps order and order item entities to response DTOs (Phase 5).
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getSku(),
                item.getPriceAtPurchase(),
                item.getQuantity(),
                item.getLineTotal());
    }

    public static OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderMapper::toItemResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                itemResponses,
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getPhone(),
                order.getAddressLine1(),
                order.getAddressLine2(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCountry(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public static OrderTrackingResponse toTrackingResponse(Order order) {
        return new OrderTrackingResponse(
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.isCancellable(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
