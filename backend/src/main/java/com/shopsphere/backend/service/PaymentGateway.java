package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment gateway abstraction (Phase 5). Checkout and payment processing talk
 * only to this interface; swapping the sandbox for a real provider means
 * adding another implementation selected via the {@code app.payment.provider}
 * configuration property — no changes to the order/checkout flow, and no
 * provider credentials anywhere in source code (they would come from the
 * environment in a real implementation).
 */
public interface PaymentGateway {

    /** @return the provider name persisted on payment records (e.g. "sandbox"). */
    String providerName();

    /**
     * Charges the given amount for an order. The metadata map is opaque
     * key/value data a specific provider may interpret (the sandbox honors
     * {@code forceFailure=true} to simulate a declined charge in tests).
     */
    Result charge(Charge charge);

    /** Verifies a charge with the provider by its reference id. */
    Result verify(String providerPaymentId);

    /** A charge request for the gateway. */
    record Charge(BigDecimal amount, String orderNumber, Map<String, String> metadata) {
    }

    /** The gateway's outcome for a charge/verify call. */
    record Result(boolean success, String providerPaymentId, String message) {
    }
}
