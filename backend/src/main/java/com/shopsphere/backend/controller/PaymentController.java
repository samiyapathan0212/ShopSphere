package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.backend.dto.request.ProcessPaymentRequest;
import com.shopsphere.backend.dto.response.PaymentResponse;
import com.shopsphere.backend.dto.response.PaymentVerificationResponse;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.PaymentService;

/**
 * Payment endpoints (Phase 5). Authenticated CUSTOMER only; ownership is
 * enforced inside {@link PaymentService} (a foreign order/payment yields 404).
 * The {@code {id}} path variable is the ORDER id — payments are one-per-order
 * and the service is keyed by order.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** The payment for the given order (owned by the authenticated customer). */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> getPayment(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentForOrder(id, currentUserId(principal)));
    }

    /**
     * Processes (or retries) a pending payment through the gateway; the
     * outcome is mirrored onto the order by the service. 200 with the updated
     * payment — a declined sandbox charge is a business outcome, not an error.
     */
    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> processPayment(
            Principal principal,
            @PathVariable Long id,
            @RequestBody(required = false) ProcessPaymentRequest request) {
        Boolean simulateFailure = request == null ? null : request.simulateFailure();
        return ResponseEntity.ok(paymentService.processPayment(id, currentUserId(principal), simulateFailure));
    }

    /** Gateway verification of the stored provider reference (read-only). */
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.verifyPayment(id, currentUserId(principal)));
    }

    private Long currentUserId(Principal principal) {
        return ((UserPrincipal) principal).user().getId();
    }
}
