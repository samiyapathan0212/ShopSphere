package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.CartItem;
import com.shopsphere.backend.domain.Order;
import com.shopsphere.backend.domain.OrderItem;
import com.shopsphere.backend.domain.OrderStatus;
import com.shopsphere.backend.domain.PaymentStatus;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.CheckoutRequest;
import com.shopsphere.backend.dto.response.OrderResponse;
import com.shopsphere.backend.dto.response.OrderTrackingResponse;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.exception.InvalidOrderStatusTransitionException;
import com.shopsphere.backend.exception.OrderCancellationException;
import com.shopsphere.backend.exception.OrderNotFoundException;
import com.shopsphere.backend.mapper.OrderMapper;
import com.shopsphere.backend.repository.OrderItemRepository;
import com.shopsphere.backend.repository.OrderRepository;
import com.shopsphere.backend.repository.PaymentRepository;

/**
 * Order operations (Phase 5). Orders are created exclusively from checkout
 * data: the shipping address is an immutable snapshot on the order and each
 * {@link OrderItem} snapshots product name/SKU/price at purchase time, so
 * later catalog edits never rewrite history. Customer reads are always
 * ownership-scoped (a foreign order is indistinguishable from a missing one —
 * both yield 404 via {@link OrderNotFoundException}); admin reads see every
 * order. Status changes are validated centrally through
 * {@link OrderStatus#canTransitionTo}, shared by the admin endpoint and the
 * customer cancellation flow.
 */
@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Creates a {@code PLACED} order plus one immutable snapshot
     * {@link OrderItem} per cart line item. Called by {@link CheckoutService}
     * inside its transaction; prices always come from the catalog entities,
     * never from the client.
     */
    @Transactional
    public Order createOrder(User user, CheckoutRequest request, List<CartItem> cartItems,
                             BigDecimal subtotal, BigDecimal shippingFee) {
        Order order = Order.builder()
                .user(user)
                .orderNumber(nextOrderNumber())
                .subtotal(subtotal)
                .discount(BigDecimal.ZERO)
                .shippingFee(shippingFee)
                .totalAmount(subtotal.add(shippingFee))
                .recipientName(request.recipientName())
                .phone(request.phone())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .build();
        for (CartItem cartItem : cartItems) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .sku(cartItem.getProduct().getSku())
                    .priceAtPurchase(cartItem.getProduct().getPrice())
                    .quantity(cartItem.getQuantity())
                    .lineTotal(cartItem.getSubtotal())
                    .build();
            order.addItem(item);
        }
        return orderRepository.save(order);
    }

    /** Paginated history for the authenticated customer (newest first). */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrderHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), SortUtils.createdAtDesc());
        return toPageResponse(orderRepository.findByUserId(userId, pageable));
    }

    /** Full order detail; 404 unless the order belongs to the given user. */
    @Transactional(readOnly = true)
    public OrderResponse getOrderForCustomer(Long orderId, Long userId) {
        Order order = requireOwnedOrder(orderId, userId);
        return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    /** Lightweight lifecycle/tracking view; 404 unless owned. */
    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrder(Long orderId, Long userId) {
        return OrderMapper.toTrackingResponse(requireOwnedOrder(orderId, userId));
    }

    /**
     * Customer cancellation. Only orders whose lifecycle still permits
     * {@code CANCELLED} may be cancelled; paid orders have their items
     * restocked, orders with a still-pending payment release the reservation.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = requireOwnedOrder(orderId, userId);
        if (!order.isCancellable()) {
            throw new OrderCancellationException(orderId, order.getOrderStatus());
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        Map<Long, Integer> quantitiesByProduct = quantitiesByProduct(order.getItems());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            inventoryService.restockForProducts(quantitiesByProduct);
        } else {
            inventoryService.releaseReservationForProducts(quantitiesByProduct);
        }
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.updateStatus(PaymentStatus.FAILED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
        orderRepository.save(order);
        return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    /** Admin: every order, newest first, optionally filtered by status. */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAllOrders(int page, int size, OrderStatus statusFilter) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), SortUtils.createdAtDesc());
        Page<Order> result = statusFilter == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByOrderStatus(statusFilter, pageable);
        return toPageResponse(result);
    }

    /** Admin: any order's detail without ownership restriction. */
    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        Order order = requireOrder(orderId);
        return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    /** Admin status update; invalid lifecycle transitions are rejected (409). */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus targetStatus) {
        Order order = requireOrder(orderId);
        OrderStatus current = order.getOrderStatus();
        if (!current.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStatusTransitionException(current, targetStatus);
        }
        order.setOrderStatus(targetStatus);
        orderRepository.save(order);
        return OrderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /** A foreign order must be indistinguishable from a missing one (404). */
    private Order requireOwnedOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Maps an order page with a single batched item query (no N+1 per order),
     * reusing the shared {@link PageResponse} metadata shape.
     */
    private PageResponse<OrderResponse> toPageResponse(Page<Order> page) {
        List<Long> orderIds = page.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = new LinkedHashMap<>();
        if (!orderIds.isEmpty()) {
            for (OrderItem item : orderItemRepository.findByOrderIdIn(orderIds)) {
                itemsByOrderId.computeIfAbsent(item.getOrder().getId(), key -> new ArrayList<>())
                        .add(item);
            }
        }
        List<OrderResponse> responses = page.getContent().stream()
                .map(order -> OrderMapper.toResponse(order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .toList();
        return PageResponse.from(new PageImpl<>(responses, page.getPageable(), page.getTotalElements()));
    }

    private static Map<Long, Integer> quantitiesByProduct(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    /** Unique, ≤32-char order number (the DB unique constraint backstops). */
    private String nextOrderNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "SS-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase(java.util.Locale.ROOT);
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique order number");
    }
}
