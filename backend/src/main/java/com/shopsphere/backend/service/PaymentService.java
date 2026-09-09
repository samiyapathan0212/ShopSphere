package com.shopsphere.backend.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Order;
import com.shopsphere.backend.domain.OrderStatus;
import com.shopsphere.backend.domain.Payment;
import com.shopsphere.backend.domain.PaymentStatus;
import com.shopsphere.backend.dto.request.UpdatePaymentStatusRequest;
import com.shopsphere.backend.dto.response.PaymentResponse;
import com.shopsphere.backend.dto.response.PaymentVerificationResponse;
import com.shopsphere.backend.exception.OrderNotFoundException;
import com.shopsphere.backend.exception.PaymentNotFoundException;
import com.shopsphere.backend.exception.PaymentStateException;
import com.shopsphere.backend.mapper.PaymentMapper;
import com.shopsphere.backend.repository.OrderRepository;
import com.shopsphere.backend.repository.PaymentRepository;

/**
 * Payment operations (Phase 5). Each order carries exactly one payment (DB
 * unique FK backstop, service pre-check). Charges go through the configured
 * {@link PaymentGateway}; the sandbox honors {@code forceFailure=true} as a
 * test hook. The resulting {@code PaymentStatus} is always mirrored onto the
 * order for quick order-list rendering, and payment success/failure advances
 * the order lifecycle (PLACED to CONFIRMED / CANCELLED). Customer access is
 * ownership-checked — a foreign payment is indistinguishable from a missing
 * one (404).
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Creates the PENDING payment for an order; the amount always equals the
     * order total (never client-supplied). Rejects a second payment (409).
     */
    @Transactional
    public Payment createPayment(Order order) {
        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new PaymentStateException(
                    "Order " + order.getId() + " already has a payment record");
        }
        return paymentRepository.save(Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentStatus(PaymentStatus.PENDING)
                .provider(paymentGateway.providerName())
                .build());
    }

    /**
     * Charges the order's PENDING payment through the gateway and mirrors the
     * outcome onto the order (payment status + order lifecycle). Used by the
     * checkout flow and by payment retry; stock handling stays with the
     * checkout/cancellation flows that own the reservation.
     */
    @Transactional
    public Payment chargePayment(Order order, boolean simulateFailure) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new PaymentNotFoundException(order.getId()));
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentStateException(
                    "Payment for order " + order.getId() + " is already "
                            + payment.getPaymentStatus());
        }
        PaymentGateway.Charge charge = new PaymentGateway.Charge(
                order.getTotalAmount(),
                order.getOrderNumber(),
                simulateFailure ? Map.of(SandboxPaymentGateway.FORCE_FAILURE_KEY, "true") : Map.of());
        PaymentGateway.Result result = paymentGateway.charge(charge);

        payment.setProviderPaymentId(result.providerPaymentId());
        PaymentStatus newStatus = result.success() ? PaymentStatus.PAID : PaymentStatus.FAILED;
        payment.updateStatus(newStatus);
        order.setPaymentStatus(newStatus);
        OrderStatus targetOrderStatus = result.success()
                ? OrderStatus.CONFIRMED
                : OrderStatus.CANCELLED;
        if (order.getOrderStatus() != targetOrderStatus
                && order.getOrderStatus().canTransitionTo(targetOrderStatus)) {
            order.setOrderStatus(targetOrderStatus);
        }
        return paymentRepository.save(payment);
    }

    /** Customer read of the payment for an order (ownership-checked). */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForOrder(Long orderId, Long userId) {
        return PaymentMapper.toResponse(requireOwnedPayment(orderId, userId));
    }

    /**
     * Retries a pending payment (e.g. after a failed first attempt) for the
     * authenticated customer; the outcome is mirrored onto the order.
     */
    @Transactional
    public PaymentResponse processPayment(Long orderId, Long userId, Boolean simulateFailure) {
        Order order = requireOwnedOrder(orderId, userId);
        Payment payment = chargePayment(order, Boolean.TRUE.equals(simulateFailure));
        return PaymentMapper.toResponse(payment);
    }

    /** Asks the gateway to verify the stored provider reference (read-only). */
    @Transactional(readOnly = true)
    public PaymentVerificationResponse verifyPayment(Long orderId, Long userId) {
        Payment payment = requireOwnedPayment(orderId, userId);
        PaymentGateway.Result result = paymentGateway.verify(payment.getProviderPaymentId());
        return new PaymentVerificationResponse(
                payment.getId(),
                orderId,
                result.success(),
                payment.getPaymentStatus(),
                result.message());
    }

    /** Admin manual status change: PENDING to PAID/FAILED, PAID to REFUNDED. */
    @Transactional
    public PaymentResponse updatePaymentStatus(Long orderId, UpdatePaymentStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        PaymentStatus current = payment.getPaymentStatus();
        PaymentStatus target = request.paymentStatus();
        boolean allowed = (current == PaymentStatus.PENDING
                && (target == PaymentStatus.PAID || target == PaymentStatus.FAILED))
                || (current == PaymentStatus.PAID && target == PaymentStatus.REFUNDED);
        if (!allowed) {
            throw new PaymentStateException(
                    "Unsupported payment status transition: " + current + " -> " + target);
        }
        payment.updateStatus(target);
        order.setPaymentStatus(target);
        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    /** A foreign order must be indistinguishable from a missing one (404). */
    private Order requireOwnedOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Payment requireOwnedPayment(Long orderId, Long userId) {
        requireOwnedOrder(orderId, userId);
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
