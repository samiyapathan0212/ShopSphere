package com.shopsphere.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.Payment;

/**
 * Payment repository (Phase 5). One payment per order (unique order_id);
 * the provider reference lookup backs gateway verification flows.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
