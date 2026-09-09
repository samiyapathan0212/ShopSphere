package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Cart;
import com.shopsphere.backend.domain.CartItem;
import com.shopsphere.backend.domain.Order;
import com.shopsphere.backend.domain.Payment;
import com.shopsphere.backend.domain.PaymentStatus;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.CheckoutRequest;
import com.shopsphere.backend.dto.response.CheckoutResponse;
import com.shopsphere.backend.exception.EmptyCartException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.mapper.OrderMapper;
import com.shopsphere.backend.mapper.PaymentMapper;
import com.shopsphere.backend.repository.CartItemRepository;
import com.shopsphere.backend.repository.CartRepository;

/**
 * Checkout (Phase 5). A single {@code @Transactional} flow: reject an empty
 * cart, validate that every cart product is still active, lock and reserve the
 * inventory rows (pessimistic, deterministic order), compute subtotal /
 * shipping fee / total from catalog prices (never client input), create the
 * order with immutable item snapshots plus its PENDING payment, and charge it
 * through the gateway. On approval the reservation converts into a sale and
 * the cart is cleared; on a sandbox decline the reservation is released and
 * the cancelled order + failed payment are still returned. Any thrown failure
 * (empty cart, inactive product, insufficient stock, ...) rolls the entire
 * transaction back — order, items, payment and every stock mutation.
 */
@Service
public class CheckoutService {

    /** Flat shipping fee for orders below the free-shipping threshold. */
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("5.00");
    /** Subtotal at which shipping becomes free. */
    private static final BigDecimal FREE_SHIPPING_MIN_SUBTOTAL = new BigDecimal("50.00");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    public CheckoutService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                           InventoryService inventoryService, OrderService orderService,
                           PaymentService paymentService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Runs the full checkout for the authenticated customer's cart. Declined
     * sandbox payments are a business outcome (not an error): the order stays
     * CANCELLED, the payment FAILED and the reserved stock is released. The
     * cart is cleared only when the order was successfully created AND paid.
     */
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(EmptyCartException::new);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new EmptyCartException();
        }

        // Validate every product is still active and aggregate demand per product.
        Map<Long, Integer> quantitiesByProduct = new LinkedHashMap<>();
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new ProductNotFoundException(product.getId());
            }
            quantitiesByProduct.merge(product.getId(), item.getQuantity(), Integer::sum);
        }

        // Lock the inventory rows and reserve the demanded stock; an
        // InsufficientStockException here aborts the whole transaction.
        inventoryService.reserveStockForProducts(quantitiesByProduct);

        BigDecimal subtotal = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_MIN_SUBTOTAL) >= 0
                ? BigDecimal.ZERO
                : FLAT_SHIPPING_FEE;

        Order order = orderService.createOrder(cart.getUser(), request, items, subtotal, shippingFee);
        Payment payment = paymentService.createPayment(order);
        payment = paymentService.chargePayment(order, Boolean.TRUE.equals(request.simulatePaymentFailure()));

        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            inventoryService.convertReservationToSaleForProducts(quantitiesByProduct);
            cartItemRepository.deleteByCartId(cart.getId());
        } else {
            inventoryService.releaseReservationForProducts(quantitiesByProduct);
        }

        return new CheckoutResponse(
                OrderMapper.toResponse(order, order.getItems()),
                PaymentMapper.toResponse(payment));
    }
}
